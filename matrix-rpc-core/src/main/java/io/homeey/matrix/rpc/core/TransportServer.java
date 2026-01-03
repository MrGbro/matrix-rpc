package io.homeey.matrix.rpc.core;

import io.homeey.matrix.rpc.common.URL;
import io.homeey.matrix.rpc.spi.SPI;

import java.io.Closeable;

/**
 *
 * @author jt4mrg@gmail.com
 * @since 2026-01-03
 **/
@SPI("netty")
public interface TransportServer extends Closeable {
    /**
     * 启动传输服务器
     *
     * @param url 服务器启动的 URL 地址
     */
    void start(URL url, RequestHandler requestHandler);
}

