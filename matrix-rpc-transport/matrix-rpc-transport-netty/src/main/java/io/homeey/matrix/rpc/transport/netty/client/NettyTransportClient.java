package io.homeey.matrix.rpc.transport.netty.client;

import com.google.protobuf.ByteString;
import io.homeey.matrix.rpc.codec.protobuf.RpcProto;
import io.homeey.matrix.rpc.common.Result;
import io.homeey.matrix.rpc.common.URL;
import io.homeey.matrix.rpc.core.Invocation;
import io.homeey.matrix.rpc.spi.Activate;
import io.homeey.matrix.rpc.transport.api.TransportClient;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.handler.codec.MessageToByteEncoder;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicLong;

@Activate(order = 100)
public class NettyTransportClient implements TransportClient {
    private URL url;
    private EventLoopGroup group;
    private Bootstrap bootstrap;
    private Channel channel;
    private final ConcurrentHashMap<Long, CompletableFuture<RpcProto.RpcResponse>> pendingRequests
            = new ConcurrentHashMap<>();
    private final AtomicLong requestIdGenerator = new AtomicLong(0);

    /**
     * 无参构造，用于 SPI 加载
     */
    public NettyTransportClient() {
    }

    /**
     * 有参构造，用于直接实例化
     */
    public NettyTransportClient(URL url) {
        init(url);
    }

    @Override
    public void init(URL url) {
        this.url = url;
        this.group = new NioEventLoopGroup();
        this.bootstrap = new Bootstrap();

        bootstrap.group(group)
                .channel(NioSocketChannel.class)
                .option(ChannelOption.TCP_NODELAY, true)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 3000)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) {
                        ChannelPipeline pipeline = ch.pipeline();
                        pipeline.addLast(new RpcResponseDecoder());
                        pipeline.addLast(new RpcRequestEncoder());
                        pipeline.addLast(new RpcClientHandler());
                    }
                });
    }

    @Override
    public void connect() throws Exception {
        ChannelFuture future = bootstrap.connect(url.getHost(), url.getPort()).sync();
        this.channel = future.channel();
        System.out.println("[Netty] Connected to server: " + url.getHost() + ":" + url.getPort());
    }

    @Override
    public boolean isConnected() {
        return channel != null && channel.isActive();
    }

    @Override
    public void close() {
        if (channel != null) {
            channel.close();
        }
        group.shutdownGracefully();
        pendingRequests.forEach((id, future) ->
                future.completeExceptionally(new IllegalStateException("Connection closed")));
    }

    @Override
    public Result send(Invocation invocation, long timeoutMillis) {
        try {
            // 1. 生成唯一请求ID
            long requestId = requestIdGenerator.incrementAndGet();

            // 2. 创建CompletableFuture等待响应
            CompletableFuture<RpcProto.RpcResponse> future = new CompletableFuture<>();
            pendingRequests.put(requestId, future);

            try {
                // 3. 构建请求
                RpcProto.RpcRequest request = buildRequest(invocation, requestId);

                // 4. 发送请求
                if (channel == null || !channel.isActive()) {
                    throw new IllegalStateException("Connection is not active");
                }
                channel.writeAndFlush(request).sync();

                // 5. 等待响应 (带超时)
                RpcProto.RpcResponse response = future.get(timeoutMillis, TimeUnit.MILLISECONDS);
                return buildResult(response);
            } finally {
                pendingRequests.remove(requestId);
            }
        } catch (Exception e) {
            if (e instanceof TimeoutException) {
                throw new RuntimeException("Request timeout after " + timeoutMillis + "ms", e);
            }
            throw new RuntimeException("Failed to send RPC request", e);
        }
    }

    private RpcProto.RpcRequest buildRequest(Invocation invocation, long requestId) {
        RpcProto.RpcRequest.Builder builder = RpcProto.RpcRequest.newBuilder()
                .setRequestId(requestId)
                .setServiceName(invocation.getServiceName())
                .setMethodName(invocation.methodName());

        // 添加参数类型
        for (Class<?> paramType : invocation.parameterTypes()) {
            builder.addParameterTypes(paramType.getName());
        }

        // 添加参数值 (简化版，只处理String)
        for (Object arg : invocation.arguments()) {
            if (arg instanceof String) {
                builder.addArguments(ByteString.copyFromUtf8((String) arg));
            } else {
                throw new UnsupportedOperationException(
                        "Only String arguments are supported in Phase 1. Actual type: " +
                                arg.getClass().getName());
            }
        }

        return builder.build();
    }

    private Result buildResult(RpcProto.RpcResponse response) {
        // protobuf中string类型默认是空字符串，不是null
        if (response.getException() != null && !response.getException().isEmpty()) {
            return new Result(new RuntimeException(response.getException()));
        }
        return new Result(response.getResult().toStringUtf8());
    }

    // =============== 内部处理器 ===============

    private class RpcClientHandler extends SimpleChannelInboundHandler<RpcProto.RpcResponse> {
        @Override
        protected void channelRead0(ChannelHandlerContext ctx, RpcProto.RpcResponse response) {
            CompletableFuture<RpcProto.RpcResponse> future = pendingRequests.get(response.getRequestId());
            if (future != null) {
                future.complete(response);
            } else {
                System.err.println("[Netty] No pending request for ID: " + response.getRequestId());
            }
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
            System.err.println("[Netty] Client exception: " + cause.getMessage());
            pendingRequests.forEach((id, future) ->
                    future.completeExceptionally(cause));
            ctx.close();
        }
    }

    // =============== 编解码器 ===============

    private static class RpcResponseDecoder extends ByteToMessageDecoder {
        @Override
        protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) {
            if (in.readableBytes() < 4) return;

            in.markReaderIndex();
            int dataLength = in.readInt();

            if (in.readableBytes() < dataLength) {
                in.resetReaderIndex();
                return;
            }

            byte[] data = new byte[dataLength];
            in.readBytes(data);
            try {
                out.add(RpcProto.RpcResponse.parseFrom(data));
            } catch (Exception e) {
                throw new RuntimeException("Failed to decode RpcResponse", e);
            }
        }
    }

    private static class RpcRequestEncoder extends MessageToByteEncoder<RpcProto.RpcRequest> {
        @Override
        protected void encode(ChannelHandlerContext ctx, RpcProto.RpcRequest request, ByteBuf out) {
            byte[] data = request.toByteArray();
            out.writeInt(data.length);
            out.writeBytes(data);
        }
    }
}