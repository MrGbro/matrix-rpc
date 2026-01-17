package io.homeey.matrix.rpc.protocol.http2;

import io.homeey.matrix.rpc.codec.api.Codec;
import io.homeey.matrix.rpc.core.*;
import io.homeey.matrix.rpc.spi.Activate;
import io.homeey.matrix.rpc.spi.ExtensionLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * HTTP/2 协议实现
 * 
 * <h3>⚡ 核心特性</h3>
 * <ul>
 *   <li><b>多路复用</b>: 基于 HTTP/2 Stream 实现</li>
 *   <li><b>头部压缩</b>: HPACK 算法</li>
 *   <li><b>标准化</b>: 完全符合 RFC 7540</li>
 *   <li><b>兼容性</b>: 支持标准 HTTP/2 客户端</li>
 * </ul>
 * 
 * <h3>📋 配置方式</h3>
 * <pre>
 * // Provider 端暴露 HTTP/2 服务
 * RpcService.export(EchoService.class, new EchoServiceImpl(), "http2://0.0.0.0:8080");
 * 
 * // Consumer 端调用 HTTP/2 服务
 * EchoService service = RpcReference.refer(EchoService.class, "http2://localhost:8080");
 * </pre>
 * 
 * @author Matrix RPC Team
 * @since 1.0.0
 */
@Activate(order = 200)
public class Http2Protocol implements Protocol {
    
    private static final Logger logger = LoggerFactory.getLogger(Http2Protocol.class);
    
    // 服务导出器缓存：key = serviceKey, value = Exporter
    private final ConcurrentMap<String, Exporter<?>> exporters = new ConcurrentHashMap<>();
    
    // HTTP/2 服务器实例缓存：key = port, value = Http2Server
    private final ConcurrentMap<Integer, Http2Server> servers = new ConcurrentHashMap<>();
    
    // HTTP/2 客户端缓存：key = address(host:port), value = Http2Client
    private final ConcurrentMap<String, Http2Client> clients = new ConcurrentHashMap<>();
    
    // Codec 序列化器（默认使用 Kryo）
    private final Codec codec;
    
    public Http2Protocol() {
        // 通过 SPI 加载 Codec（默认 kryo）
        this.codec = ExtensionLoader.getExtensionLoader(Codec.class)
                .getExtension("kryo");
    }
    
    @Override
    public <T> Exporter<T> export(Invoker<T> invoker, URL url) {
        int port = url.getPort();
        
        // 1. 获取或创建 HTTP/2 Server（同一端口复用）
        Http2Server server = servers.computeIfAbsent(port, k -> {
            Http2Server s = new Http2Server(port);
            s.setRequestHandler(this::handleRequest);
            s.start();
            logger.info("HTTP/2 server started on port: {}", port);
            return s;
        });
        
        // 2. 注册服务到 Exporter 缓存
        String serviceKey = buildServiceKey(url, invoker.getInterface());
        Exporter<T> exporter = new AbstractExporter<T>(invoker) {
            @Override
            public void unexport() {
                exporters.remove(serviceKey);
                logger.info("HTTP/2 service unexported: {}", serviceKey);
                
                // 如果该端口没有服务了，关闭服务器
                if (exporters.keySet().stream().noneMatch(key -> key.endsWith(":" + port))) {
                    server.stop();
                    servers.remove(port);
                    logger.info("HTTP/2 server stopped on port: {}", port);
                }
            }
        };
        
        exporters.put(serviceKey, exporter);
        logger.info("HTTP/2 service exported: {}", serviceKey);
        
        return exporter;
    }
    
    @Override
    public <T> Invoker<T> refer(Class<T> type, URL url) {
        // 1. 获取或创建 HTTP/2 Client
        String address = url.getAddress();
        Http2Client client = clients.computeIfAbsent(address, k -> {
            Http2Client c = new Http2Client(url.getHost(), url.getPort());
            c.connect();
            logger.info("HTTP/2 client connected to: {}", address);
            return c;
        });
        
        // 2. 创建 Invoker
        return new AbstractInvoker<T>(type) {
            @Override
            public Result invoke(Invocation invocation) {
                try {
                    // 构建 HTTP/2 请求
                    Http2Request request = buildHttp2Request(invocation, url);
                    
                    // 获取超时时间
                    long timeout = url.getParameter("timeout", 3000);
                    
                    // 发送请求并等待响应
                    Http2Response response = client.send(request, timeout);
                    
                    // 解析响应
                    return parseHttp2Response(response, invocation);
                } catch (Exception e) {
                    logger.error("HTTP/2 call failed: {}", invocation.methodName(), e);
                    return new Result(new RpcException("HTTP/2 call failed: " + e.getMessage(), e));
                }
            }
            

        };
    }
    
    /**
     * 构建 HTTP/2 请求
     * 
     * <p>请求格式：
     * <pre>
     * POST /io.homeey.example.api.EchoService/echo HTTP/2
     * content-type: application/grpc+proto
     * matrix-service: io.homeey.example.api.EchoService
     * matrix-method: echo
     * matrix-version: 1.0.0
     * matrix-group: default
     * 
     * [Serialized body using Kryo/Protobuf]
     * </pre>
     */
    private Http2Request buildHttp2Request(Invocation invocation, URL url) {
        Http2Request request = new Http2Request();
        
        // 1. 设置路径（RESTful 风格）
        String path = "/" + invocation.getServiceName() + "/" + invocation.methodName();
        request.setPath(path);
        request.setMethod("POST");
        
        // 2. 设置 Headers（元数据传递）
        request.addHeader("content-type", "application/matrix-rpc");
        request.addHeader("matrix-service", invocation.getServiceName());
        request.addHeader("matrix-method", invocation.methodName());
        request.addHeader("matrix-version", url.getParameter("version", "1.0.0"));
        request.addHeader("matrix-group", url.getParameter("group", ""));
        
        // 3. 传递 attachments
        invocation.getAttachments().forEach((k, v) -> 
            request.addHeader("matrix-attachment-" + k, v));
        
        // 4. 序列化 Invocation（使用 Codec）
        try {
            // 将整个 Invocation 序列化
            byte[] body = codec.encode(invocation);
            request.setBody(body);
        } catch (Exception e) {
            logger.error("Failed to serialize invocation", e);
            throw new RuntimeException("Failed to serialize invocation", e);
        }
        
        return request;
    }
    
    /**
     * 解析 HTTP/2 响应
     */
    private Result parseHttp2Response(Http2Response response, Invocation invocation) {
        // 检查状态码
        if (response.getStatus() != 200) {
            String error = response.getHeader("matrix-error");
            return new Result(new RpcException("HTTP/2 error: " + error));
        }
        
        // 检查是否有异常
        String exception = response.getHeader("matrix-exception");
        if (exception != null && !exception.isEmpty()) {
            return new Result(new RpcException(exception));
        }
        
        // 反序列化结果
        try {
            byte[] body = response.getBody();
            if (body == null || body.length == 0) {
                return new Result(null);
            }
            
            // 使用 Codec 反序列化
            Object result = codec.decode(body, Object.class);
            return new Result(result);
        } catch (Exception e) {
            logger.error("Failed to parse HTTP/2 response", e);
            return new Result(new RpcException("Failed to parse HTTP/2 response", e));
        }
    }
    
    /**
     * 处理 HTTP/2 请求
     */
    private Http2Response handleRequest(Http2Request request) {
        try {
            // 1. 从 Headers 提取元数据
            String serviceName = request.getHeader("matrix-service");
            String methodName = request.getHeader("matrix-method");
            String version = request.getHeader("matrix-version");
            String group = request.getHeader("matrix-group");
            
            // 2. 查找 Exporter
            String serviceKey = serviceName + ":" + group + ":" + version;
            Exporter<?> exporter = exporters.get(serviceKey);
            
            if (exporter == null) {
                logger.warn("Service not found: {}", serviceKey);
                Http2Response response = new Http2Response();
                response.setStatus(404);
                response.addHeader("matrix-error", "Service not found: " + serviceKey);
                return response;
            }
            
            // 3. 反序列化 Invocation
            byte[] body = request.getBody();
            if (body == null || body.length == 0) {
                Http2Response response = new Http2Response();
                response.setStatus(400);
                response.addHeader("matrix-error", "Empty request body");
                return response;
            }
            
            Invocation invocation = codec.decode(body, Invocation.class);
            
            // 4. 调用服务
            Result result = exporter.getInvoker().invoke(invocation);
            
            // 5. 构建响应
            Http2Response response = new Http2Response();
            response.setStatus(200);
            
            if (result.hasException()) {
                response.addHeader("matrix-exception", result.getException().getMessage());
            } else {
                // 序列化结果
                Object value = result.getValue(Object.class);
                if (value != null) {
                    byte[] responseBody = codec.encode(value);
                    response.setBody(responseBody);
                }
            }
            
            return response;
        } catch (Exception e) {
            logger.error("Failed to handle HTTP/2 request", e);
            Http2Response response = new Http2Response();
            response.setStatus(500);
            response.addHeader("matrix-error", "Internal error: " + e.getMessage());
            return response;
        }
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
