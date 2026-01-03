package io.homeey.matrix.rpc.transport.netty.client;


import io.homeey.matrix.rpc.common.Result;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

import java.util.concurrent.CompletableFuture;

public class NettyClientHandler
        extends SimpleChannelInboundHandler<Result> {

    private final CompletableFuture<Result> future;

    public NettyClientHandler(CompletableFuture<Result> future) {
        this.future = future;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx,
                                Result result) {
        future.complete(result);
        ctx.close();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx,
                                Throwable cause) {
        future.completeExceptionally(cause);
        ctx.close();
    }
}
