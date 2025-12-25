package io.homeey.nexa.core.transport;

/**
 *
 * @author jt4mrg@gmail.com
 * @since 2025-12-25 23:22
 **/
public interface ChannelHandler {

    /**
     * 当通道连接时调用
     * @param channel 连接的通道
     */
    void connected(Channel channel);
    
    /**
     * 当通道断开连接时调用
     * @param channel 断开连接的通道
     */
    void disconnected(Channel channel);
    
    /**
     * 当通道接收到消息时调用
     * @param channel 接收消息的通道
     * @param message 接收到的消息
     */
    void received(Channel channel, Object message);
    
    /**
     * 当通道捕获到异常时调用
     * @param channel 发生异常的通道
     * @param cause 异常原因
     */
    void caught(Channel channel, Throwable cause);
}
