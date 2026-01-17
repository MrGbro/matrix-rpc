package io.homeey.matrix.rpc.protocol.grpc;

import io.grpc.*;
import io.grpc.netty.shaded.io.grpc.netty.NettyServerBuilder;
import io.grpc.netty.shaded.io.grpc.netty.NettyChannelBuilder;
import io.homeey.matrix.rpc.codec.api.Codec;
import io.homeey.matrix.rpc.core.*;
import io.homeey.matrix.rpc.spi.Activate;
import io.homeey.matrix.rpc.spi.ExtensionLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

/**
 * gRPC 协议实现
 * 
 * <h3>⚡ 核心特性</h3>
 * <ul>
 *   <li><b>完全兼容</b>: 使用官方 gRPC-Java 库</li>
 *   <li><b>动态调用</b>: 无需预生成 stub 代码</li>
 *   <li><b>流式支持</b>: 支持四种流式模式（后续扩展）</li>
 *   <li><b>标准协议</b>: 完全兼容 gRPC 生态</li>
 * </ul>
 * 
 * <h3>📋 配置方式</h3>
 * <pre>
 * // Provider 端暴露 gRPC 服务
 * RpcService.export(EchoService.class, new EchoServiceImpl(), "grpc://0.0.0.0:9090");
 * 
 * // Consumer 端调用 gRPC 服务
 * EchoService service = RpcReference.refer(EchoService.class, "grpc://localhost:9090");
 * </pre>
 * 
 * @author Matrix RPC Team
 * @since 1.0.0
 */
@Activate(order = 300)
public class GrpcProtocol implements Protocol {
    
    private static final Logger logger = LoggerFactory.getLogger(GrpcProtocol.class);
    
    // gRPC 服务器缓存：key = port, value = Server
    private final ConcurrentMap<Integer, Server> servers = new ConcurrentHashMap<>();
    
    // gRPC Channel 缓存：key = address(host:port), value = ManagedChannel
    private final ConcurrentMap<String, ManagedChannel> channels = new ConcurrentHashMap<>();
    
    // 服务导出器缓存：key = serviceKey, value = Exporter
    private final ConcurrentMap<String, Exporter<?>> exporters = new ConcurrentHashMap<>();
    
    // Codec 序列化器（默认使用 Kryo）
    private final Codec codec;
    
    public GrpcProtocol() {
        // 通过 SPI 加载 Codec（默认 kryo）
        this.codec = ExtensionLoader.getExtensionLoader(Codec.class)
                .getExtension("kryo");
    }
    
    @Override
    public <T> Exporter<T> export(Invoker<T> invoker, URL url) {
        int port = url.getPort();
        
        // 1. 获取或创建 gRPC Server（同一端口复用）
        Server server = servers.computeIfAbsent(port, k -> {
            try {
                // 构建 gRPC Server
                ServerBuilder<?> serverBuilder = NettyServerBuilder.forPort(port)
                    .maxInboundMessageSize(url.getParameter("maxMessageSize", 4 * 1024 * 1024))
                    .maxConcurrentCallsPerConnection(url.getParameter("maxConcurrentCalls", 100));
                
                // 创建泛化服务
                GenericGrpcService genericService = new GenericGrpcService(this, codec);
                ServerServiceDefinition serviceDefinition = genericService.bindService();
                serverBuilder.addService(serviceDefinition);
                
                // 启动服务器
                Server s = serverBuilder.build().start();
                logger.info("gRPC server started on port: {}", port);
                
                // 添加关闭钩子
                Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                    logger.info("Shutting down gRPC server on port: {}", port);
                    s.shutdown();
                    try {
                        if (!s.awaitTermination(5, TimeUnit.SECONDS)) {
                            s.shutdownNow();
                        }
                    } catch (InterruptedException e) {
                        s.shutdownNow();
                    }
                }));
                
                return s;
            } catch (IOException e) {
                throw new RuntimeException("Failed to start gRPC server on port: " + port, e);
            }
        });
        
        // 2. 注册服务到 Exporter 缓存
        String serviceKey = buildServiceKey(url, invoker.getInterface());
        Exporter<T> exporter = new AbstractExporter<T>(invoker) {
            @Override
            public void unexport() {
                exporters.remove(serviceKey);
                logger.info("gRPC service unexported: {}", serviceKey);
                
                // 如果该端口没有服务了，关闭服务器
                if (exporters.keySet().stream().noneMatch(key -> key.contains(":" + port))) {
                    Server s = servers.remove(port);
                    if (s != null) {
                        s.shutdown();
                        logger.info("gRPC server stopped on port: {}", port);
                    }
                }
            }
        };
        
        exporters.put(serviceKey, exporter);
        logger.info("gRPC service exported: {}", serviceKey);
        
        return exporter;
    }
    
    @Override
    public <T> Invoker<T> refer(Class<T> type, URL url) {
        // 1. 获取或创建 gRPC Channel
        String address = url.getAddress();
        ManagedChannel channel = channels.computeIfAbsent(address, k -> {
            ManagedChannel ch = NettyChannelBuilder
                .forAddress(url.getHost(), url.getPort())
                .usePlaintext()  // 默认不使用 TLS，生产环境应使用 TLS
                .maxInboundMessageSize(url.getParameter("maxMessageSize", 4 * 1024 * 1024))
                .keepAliveTime(url.getParameter("keepAliveTime", 30), TimeUnit.SECONDS)
                .keepAliveTimeout(url.getParameter("keepAliveTimeout", 10), TimeUnit.SECONDS)
                .build();
            
            logger.info("gRPC channel created to: {}", address);
            return ch;
        });
        
        // 2. 创建 Invoker
        return new AbstractInvoker<T>(type) {
            @Override
            public Result invoke(Invocation invocation) {
                try {
                    // 构建 gRPC 方法描述符
                    String fullMethodName = MethodDescriptor.generateFullMethodName(
                        invocation.getServiceName(), 
                        invocation.methodName()
                    );
                    
                    MethodDescriptor<byte[], byte[]> methodDescriptor = MethodDescriptor
                        .<byte[], byte[]>newBuilder()
                        .setType(MethodDescriptor.MethodType.UNARY)
                        .setFullMethodName(fullMethodName)
                        .setRequestMarshaller(new ByteArrayMarshaller())
                        .setResponseMarshaller(new ByteArrayMarshaller())
                        .build();
                    
                    // 序列化请求
                    byte[] requestBytes = codec.encode(invocation);
                    
                    // 创建 CallOptions（添加超时）
                    long timeout = url.getParameter("timeout", 3000);
                    CallOptions callOptions = CallOptions.DEFAULT
                        .withDeadlineAfter(timeout, TimeUnit.MILLISECONDS);
                    
                    // 同步调用
                    byte[] responseBytes = io.grpc.stub.ClientCalls.blockingUnaryCall(
                        channel, 
                        methodDescriptor, 
                        callOptions,
                        requestBytes
                    );
                    
                    // 反序列化响应
                    Object result = codec.decode(responseBytes, Object.class);
                    return new Result(result);
                    
                } catch (StatusRuntimeException e) {
                    logger.error("gRPC call failed: {} - {}", 
                        e.getStatus().getCode(), e.getStatus().getDescription());
                    return new Result(new RpcException(
                        "gRPC call failed: " + e.getStatus().getDescription(), e));
                } catch (Exception e) {
                    logger.error("gRPC call failed: {}", invocation.methodName(), e);
                    return new Result(new RpcException("gRPC call failed: " + e.getMessage(), e));
                }
            }
        };
    }
    
    /**
     * 根据 serviceKey 查找 Exporter
     */
    public Exporter<?> getExporter(String serviceKey) {
        return exporters.get(serviceKey);
    }
    
    /**
     * 构建服务 Key
     * 格式：serviceName:group:version
     */
    private String buildServiceKey(URL url, Class<?> type) {
        return type.getName() + ":" + 
               url.getParameter("group", "") + ":" + 
               url.getParameter("version", "1.0.0");
    }
    
    /**
     * 字节数组序列化器（用于泛化调用）
     */
    private static class ByteArrayMarshaller implements MethodDescriptor.Marshaller<byte[]> {
        @Override
        public java.io.InputStream stream(byte[] value) {
            return new java.io.ByteArrayInputStream(value);
        }
        
        @Override
        public byte[] parse(java.io.InputStream stream) {
            try {
                return stream.readAllBytes();
            } catch (IOException e) {
                throw new RuntimeException("Failed to parse bytes", e);
            }
        }
    }
    
    /**
     * 抽象 Exporter 实现
     */
    private static class AbstractExporter<T> implements Exporter<T> {
        private final Invoker<T> invoker;
        
        public AbstractExporter(Invoker<T> invoker) {
            this.invoker = invoker;
        }
        
        @Override
        public Invoker<T> getInvoker() {
            return invoker;
        }
        
        @Override
        public void unexport() {
            // 子类可重写
        }
    }
    
    /**
     * 抽象 Invoker 实现
     */
    private static abstract class AbstractInvoker<T> implements Invoker<T> {
        private final Class<T> type;
        
        public AbstractInvoker(Class<T> type) {
            this.type = type;
        }
        
        @Override
        public Class<T> getInterface() {
            return type;
        }
    }
}
