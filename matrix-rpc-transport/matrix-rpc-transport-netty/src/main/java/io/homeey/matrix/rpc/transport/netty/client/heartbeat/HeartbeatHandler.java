package io.homeey.matrix.rpc.transport.netty.client.heartbeat;

import io.netty.channel.Channel;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 心跳处理器 - 在连接空闲时发送心跳包保持连接活跃
 * 
 * <p>工作原理：
 * <ul>
 *   <li>监听 IdleStateEvent 事件（由 IdleStateHandler 触发）</li>
 *   <li>在写空闲时发送心跳包</li>
 *   <li>在读空闲时记录日志（可能连接已断开）</li>
 *   <li>心跳包可以是简单的 ping 消息或空消息</li>
 * </ul>
 * 
 * <p>使用示例：
 * <pre>
 * pipeline.addLast(new IdleStateHandler(60, 30, 0, TimeUnit.SECONDS));
 * pipeline.addLast(new HeartbeatHandler());
 * </pre>
 * 
 * @author Matrix RPC Team
 * @since 2026-01-14
 */
public class HeartbeatHandler extends ChannelDuplexHandler {
    
    private static final Logger logger = LoggerFactory.getLogger(HeartbeatHandler.class);
    
    /**
     * 心跳包内容（空字节数组，占用最小带宽）
     */
    private static final byte[] HEARTBEAT_PAYLOAD = new byte[0];
    
    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent) {
            IdleStateEvent event = (IdleStateEvent) evt;
            
            if (event.state() == IdleState.WRITER_IDLE) {
                // 写空闲：发送心跳包
                sendHeartbeat(ctx.channel());
            } else if (event.state() == IdleState.READER_IDLE) {
                // 读空闲：可能连接已断开
                logger.warn("No data received from {} for a long time, connection may be broken", 
                        ctx.channel().remoteAddress());
            }
        } else {
            super.userEventTriggered(ctx, evt);
        }
    }
    
    /**
     * 发送心跳包
     * 
     * @param channel 目标 Channel
     */
    private void sendHeartbeat(Channel channel) {
        if (channel.isActive()) {
            logger.debug("Sending heartbeat to {}", channel.remoteAddress());
            
            // 发送心跳包（空消息）
            // 注意：实际项目中可能需要定义专门的心跳消息格式
            channel.writeAndFlush(HEARTBEAT_PAYLOAD).addListener(future -> {
                if (future.isSuccess()) {
                    logger.debug("Heartbeat sent to {}", channel.remoteAddress());
                } else {
                    logger.warn("Failed to send heartbeat to {}: {}", 
                            channel.remoteAddress(), future.cause().getMessage());
                }
            });
        } else {
            logger.warn("Channel {} is not active, skip heartbeat", channel.remoteAddress());
        }
    }
    
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        logger.error("Exception in heartbeat handler for {}", 
                ctx.channel().remoteAddress(), cause);
        super.exceptionCaught(ctx, cause);
    }
}
