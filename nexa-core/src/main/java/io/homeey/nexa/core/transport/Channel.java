package io.homeey.nexa.core.transport;

import io.homeey.nexa.common.URL;

import java.net.InetSocketAddress;

/**
 *
 * @author jt4mrg@gmail.com
 * @since 2025-12-25 23:20
 **/
public interface Channel {
    /**
     * 获取通道的URL
     *
     * @return 通道的URL
     */
    URL getURL();

    /**
     * 获取本地地址
     *
     * @return 本地地址
     */
    InetSocketAddress getLocalAddress();

    /**
     * 获取远程地址
     *
     * @return 远程地址
     */
    InetSocketAddress getRemoteAddress();

    /**
     * 检查通道是否已连接
     *
     * @return 如果通道已连接返回true，否则返回false
     */
    boolean isConnected();

    /**
     * 发送消息
     *
     * @param message 要发送的消息
     * @param sent    表示是否等待消息发送完成
     */
    void send(Object message, boolean sent);

    /**
     * 关闭通道
     */
    void close();

    /**
     * 检查通道是否已关闭
     *
     * @return 如果通道已关闭返回true，否则返回false
     */
    boolean isClosed();
}
