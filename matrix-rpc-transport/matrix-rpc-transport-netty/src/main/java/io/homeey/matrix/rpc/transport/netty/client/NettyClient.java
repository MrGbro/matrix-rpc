package io.homeey.matrix.rpc.transport.netty.client;

import io.homeey.matrix.rpc.common.Result;
import io.homeey.matrix.rpc.core.Invocation;
import io.homeey.matrix.rpc.core.remoting.Request;
import io.homeey.matrix.rpc.core.remoting.ResponseFuture;
import io.homeey.matrix.rpc.transport.api.ExchangeClient;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.serialization.ClassResolvers;
import io.netty.handler.codec.serialization.ObjectDecoder;
import io.netty.handler.codec.serialization.ObjectEncoder;

import java.util.concurrent.CompletableFuture;

public class NettyClient implements ExchangeClient {

    @Override
    public ResponseFuture send(Request request) {

        DefaultResponseFuture future =
                new DefaultResponseFuture(request.getRequestId());

        // Netty writeAndFlush(request)
        // response 在 channelRead 时回调 received()

//        NettyClient.send(request.get, port, invocation)

        return future;
    }

    private static Result send(String host,
                               int port,
                               Invocation invocation) {

        EventLoopGroup group = new NioEventLoopGroup(1);
        CompletableFuture<Result> future = new CompletableFuture<>();

        try {
            Bootstrap bootstrap = new Bootstrap();
            bootstrap.group(group)
                    .channel(NioSocketChannel.class)
                    .handler(new ChannelInitializer<Channel>() {
                        @Override
                        protected void initChannel(Channel ch) {
                            ch.pipeline()
                                    .addLast(new ObjectEncoder())
                                    .addLast(new ObjectDecoder(
                                            1024 * 1024,
                                            ClassResolvers.cacheDisabled(null)
                                    ))
                                    .addLast(
                                            new NettyClientHandler(future)
                                    );
                        }
                    });

            Channel channel = bootstrap
                    .connect(host, port)
                    .sync()
                    .channel();

            channel.writeAndFlush(invocation).sync();

            return future.get();
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            group.shutdownGracefully();
        }
    }
}
