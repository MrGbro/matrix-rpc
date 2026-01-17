package io.homeey.matrix.rpc.protocol.grpc;

import io.grpc.*;
import io.grpc.stub.ServerCalls;
import io.homeey.matrix.rpc.codec.api.Codec;
import io.homeey.matrix.rpc.core.Exporter;
import io.homeey.matrix.rpc.core.Invocation;
import io.homeey.matrix.rpc.core.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

/**
 * 泛化 gRPC 服务（服务端）
 * 
 * <p>支持动态服务注册，无需预生成 stub 代码。通过运行时构建 MethodDescriptor
 * 和 ServerCallHandler 来处理所有 gRPC 调用。
 * 
 * <h3>核心功能：</h3>
 * <ul>
 *   <li>动态方法注册：无需编译期生成代码</li>
 *   <li>通用序列化：使用 Codec 处理任意对象</li>
 *   <li>完全兼容：符合 gRPC 标准协议</li>
 *   <li>错误处理：规范的 gRPC Status 错误码</li>
 * </ul>
 * 
 * @author Matrix RPC Team
 * @since 1.0.0
 */
public class GenericGrpcService implements BindableService {
    
    private static final Logger logger = LoggerFactory.getLogger(GenericGrpcService.class);
    
    private final GrpcProtocol protocol;
    private final Codec codec;
    
    public GenericGrpcService(GrpcProtocol protocol, Codec codec) {
        this.protocol = protocol;
        this.codec = codec;
    }
    
    @Override
    public ServerServiceDefinition bindService() {
        // 创建一个通用的服务定义，处理所有方法调用
        ServerServiceDefinition.Builder builder = ServerServiceDefinition.builder("matrix.rpc.GenericService");
        
        // 注册一个通用的方法处理器
        // 注意：gRPC 需要预先注册方法，这里我们注册一个通配符方法
        MethodDescriptor<byte[], byte[]> genericMethod = MethodDescriptor
            .<byte[], byte[]>newBuilder()
            .setType(MethodDescriptor.MethodType.UNARY)
            .setFullMethodName("matrix.rpc.GenericService/invoke")
            .setRequestMarshaller(new ByteArrayMarshaller())
            .setResponseMarshaller(new ByteArrayMarshaller())
            .build();
        
        ServerCallHandler<byte[], byte[]> callHandler = ServerCalls.asyncUnaryCall(
            (request, responseObserver) -> {
                try {
                    // 反序列化 Invocation
                    Invocation invocation = codec.decode(request, Invocation.class);
                    logger.debug("Received gRPC call: {}.{}", 
                        invocation.getServiceName(), invocation.methodName());
                    
                    // 构建 serviceKey 并查找 Exporter
                    String serviceKey = buildServiceKey(invocation);
                    Exporter<?> exporter = protocol.getExporter(serviceKey);
                    
                    if (exporter == null) {
                        String error = "Service not found: " + serviceKey;
                        logger.warn(error);
                        responseObserver.onError(
                            Status.NOT_FOUND.withDescription(error).asRuntimeException()
                        );
                        return;
                    }
                    
                    // 调用服务
                    Result result = exporter.getInvoker().invoke(invocation);
                    
                    // 处理结果
                    if (result.hasException()) {
                        Throwable exception = result.getException();
                        logger.error("Service invocation failed", exception);
                        responseObserver.onError(
                            Status.INTERNAL
                                .withDescription(exception.getMessage())
                                .withCause(exception)
                                .asRuntimeException()
                        );
                    } else {
                        // 序列化响应
                        Object value = result.getValue(Object.class);
                        byte[] responseBytes = value != null ? codec.encode(value) : new byte[0];
                        
                        // 发送响应
                        responseObserver.onNext(responseBytes);
                        responseObserver.onCompleted();
                        
                        logger.debug("gRPC call completed successfully");
                    }
                    
                } catch (Exception e) {
                    logger.error("Failed to handle gRPC request", e);
                    responseObserver.onError(
                        Status.INTERNAL
                            .withDescription("Internal error: " + e.getMessage())
                            .withCause(e)
                            .asRuntimeException()
                    );
                }
            }
        );
        
        builder.addMethod(genericMethod, callHandler);
        return builder.build();
    }
    
    /**
     * 构建服务 Key
     * 从 Invocation 的 attachments 中获取 group 和 version
     */
    private String buildServiceKey(Invocation invocation) {
        String serviceName = invocation.getServiceName();
        Map<String, String> attachments = invocation.getAttachments();
        String group = attachments.getOrDefault("group", "");
        String version = attachments.getOrDefault("version", "1.0.0");
        return serviceName + ":" + group + ":" + version;
    }
    
    /**
     * 字节数组序列化器
     */
    private static class ByteArrayMarshaller implements MethodDescriptor.Marshaller<byte[]> {
        @Override
        public InputStream stream(byte[] value) {
            return new ByteArrayInputStream(value);
        }
        
        @Override
        public byte[] parse(InputStream stream) {
            try {
                return stream.readAllBytes();
            } catch (IOException e) {
                throw new RuntimeException("Failed to parse bytes", e);
            }
        }
    }
}
