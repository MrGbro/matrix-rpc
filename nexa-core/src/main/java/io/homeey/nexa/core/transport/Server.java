package io.homeey.nexa.core.transport;

import io.homeey.nexa.common.URL;

import java.net.InetSocketAddress;
import java.util.Collection;

/**
 *
 * @author jt4mrg@gmail.com
 * @since 2025-12-25 23:26
 **/
public interface Server {
    /**
     * 获取服务器的URL
     * @return 服务器的URL
     */
    URL getUrl();

    /**
     * 获取服务器本地绑定的地址
     * @return 本地地址
     */
    InetSocketAddress getLocalAddress();

    /**
     * 获取所有活动的通道
     * @return 通道集合
     */
    Collection<Channel> getChannels();

    /**
     * 根据远程地址获取对应的通道
     * @param remoteAddress 远程地址
     * @return 通道，如果不存在则返回null
     */
    Channel getChannel(InetSocketAddress remoteAddress);

    /**
     * 检查服务器是否已绑定到地址
     * @return 如果已绑定返回true，否则返回false
     */
    boolean isBound();

    /**
     * 关闭服务器，释放相关资源
     */
    void close();

    /**
     * 检查服务器是否已关闭
     * @return 如果已关闭返回true，否则返回false
     */
    boolean isClosed();
}
