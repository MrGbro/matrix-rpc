package io.homeey.matrix.rpc.transport.netty.server;

import com.google.protobuf.ByteString;
import io.homeey.matrix.rpc.codec.protobuf.RpcProto;
import io.homeey.matrix.rpc.common.Result;
import io.homeey.matrix.rpc.common.URL;
import io.homeey.matrix.rpc.core.Invocation;
import io.homeey.matrix.rpc.core.SimpleInvocation;
import io.homeey.matrix.rpc.spi.Activate;
import io.homeey.matrix.rpc.transport.api.RequestHandler;
import io.homeey.matrix.rpc.transport.api.TransportServer;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.handler.codec.MessageToByteEncoder;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

@Activate(order = 100)
public class NettyTransportServer implements TransportServer {

    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;
    private Channel serverChannel;
    private final AtomicBoolean started = new AtomicBoolean(false);
    private RequestHandler requestHandler;

    @Override
    public synchronized void start(URL url, RequestHandler requestHandler) {
        if (started.get()) {
            throw new IllegalStateException("Server already started");
        }

        this.requestHandler = requestHandler;
        int port = url.getPort();

        bossGroup = new NioEventLoopGroup(1);
        workerGroup = new NioEventLoopGroup();

        try {
            ServerBootstrap bootstrap = new ServerBootstrap();
            bootstrap.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) {
                            ChannelPipeline pipeline = ch.pipeline();
                            pipeline.addLast(new RpcRequestDecoder());
                            pipeline.addLast(new RpcResponseEncoder());
                            pipeline.addLast(new RpcServerHandler());
                        }
                    })
                    .option(ChannelOption.SO_BACKLOG, 1024)
                    .childOption(ChannelOption.SO_KEEPALIVE, true)
                    .childOption(ChannelOption.TCP_NODELAY, true);

            serverChannel = bootstrap.bind(port).sync().channel();
            started.set(true);
            System.out.println("[Matrix RPC] Netty server started on port: " + port);
        } catch (InterruptedException e) {
            throw new RuntimeException("Failed to start Netty server on port: " + port, e);
        }
    }

    @Override
    public synchronized void close() {
        if (!started.getAndSet(false)) {
            return;
        }

        if (serverChannel != null) {
            serverChannel.close();
        }
        if (workerGroup != null) {
            workerGroup.shutdownGracefully();
        }
        if (bossGroup != null) {
            bossGroup.shutdownGracefully();
        }
        System.out.println("[Matrix RPC] Netty server stopped");
    }

    // =============== 内部处理器 ===============

    private static class RpcRequestDecoder extends ByteToMessageDecoder {
        @Override
        protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) {
            if (in.readableBytes() < 4) {
                return;
            }

            in.markReaderIndex();
            int dataLength = in.readInt();

            if (in.readableBytes() < dataLength) {
                in.resetReaderIndex();
                return;
            }

            byte[] data = new byte[dataLength];
            in.readBytes(data);
            try {
                out.add(RpcProto.RpcRequest.parseFrom(data));
            } catch (Exception e) {
                throw new RuntimeException("Failed to decode RpcRequest", e);
            }
        }
    }

    private static class RpcResponseEncoder extends MessageToByteEncoder<RpcProto.RpcResponse> {
        @Override
        protected void encode(ChannelHandlerContext ctx, RpcProto.RpcResponse response, ByteBuf out) {
            byte[] data = response.toByteArray();
            out.writeInt(data.length);
            out.writeBytes(data);
        }
    }

    private class RpcServerHandler extends SimpleChannelInboundHandler<RpcProto.RpcRequest> {
        @Override
        protected void channelRead0(ChannelHandlerContext ctx, RpcProto.RpcRequest request) {
            long requestId = request.getRequestId();
            try {
                // 1. 将Protobuf请求转为Invocation
                Invocation invocation = convertToInvocation(request);

                // 2. 处理请求
                Result result = requestHandler.handle(invocation);

                // 3. 构建响应
                RpcProto.RpcResponse response = buildResponse(requestId, result);

                // 4. 发送响应
                ctx.writeAndFlush(response);
            } catch (Exception e) {
                // 构建错误响应
                RpcProto.RpcResponse errorResponse = RpcProto.RpcResponse.newBuilder()
                        .setRequestId(requestId)
                        .setException("Internal error: " + e.getMessage())
                        .build();
                ctx.writeAndFlush(errorResponse);
            }
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
            System.err.println("[Matrix RPC] Server exception: " + cause.getMessage());
            cause.printStackTrace();
            ctx.close();
        }

        private Invocation convertToInvocation(RpcProto.RpcRequest request) {
            try {
                // 1. 获取参数类型
                Class<?>[] parameterTypes = request.getParameterTypesList().stream()
                        .map(this::loadClass)
                        .toArray(Class<?>[]::new);

                // 2. 反序列化参数 (Phase 2 将使用 Codec SPI)
                Object[] arguments = new Object[request.getArgumentsCount()];
                for (int i = 0; i < arguments.length; i++) {
                    // 简化版：只处理 String 类型
                    if (parameterTypes[i] == String.class) {
                        arguments[i] = request.getArguments(i).toStringUtf8();
                    } else {
                        // Phase 2 将集成完整的编解码器
                        throw new UnsupportedOperationException(
                                "Only String type is supported in Phase 1. Unsupported parameter type: " +
                                        parameterTypes[i].getName());
                    }
                }

                // 3. 转换 attachments
                Map<String, String> attachments = request.getAttachmentsMap().entrySet().stream()
                        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

                return new SimpleInvocation(
                        request.getServiceName(),
                        request.getMethodName(),
                        parameterTypes,
                        arguments,
                        attachments
                );
            } catch (Exception e) {
                throw new RuntimeException("Failed to load parameter class", e);
            }
        }

        private Class<?> loadClass(String className) {
            // 简化版：只支持基本类型和 String
            switch (className) {
                case "java.lang.String":
                    return String.class;
                case "int":
                    return int.class;
                case "long":
                    return long.class;
                case "boolean":
                    return boolean.class;
                // Phase 2 扩展更多类型
                default:
                    throw new UnsupportedOperationException("Unsupported parameter type in Phase 1: " + className);
            }
        }

        private RpcProto.RpcResponse buildResponse(long requestId, Result result) {
            RpcProto.RpcResponse.Builder builder = RpcProto.RpcResponse.newBuilder()
                    .setRequestId(requestId);
            if (result.hasException()) {
                builder.setException(result.getException().getMessage());
            } else {
                try {
                    // 简化版：只处理 String 返回值
                    Object value = result.getValue(Object.class);
                    if (value instanceof String) {
                        builder.setResult(ByteString.copyFromUtf8((String) value));
                    } else {
                        // Phase 2 将集成完整的编解码器
                        throw new UnsupportedOperationException(
                                "Only String return type is supported in Phase 1. Actual type: " +
                                        value.getClass().getName());
                    }
                } catch (Exception e) {
                    builder.setException("Serialization error: " + e.getMessage());
                }
            }
            return builder.build();
        }
    }
}