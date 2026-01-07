package io.homeey.matrix.rpc.transport.api;

import io.homeey.matrix.rpc.common.Result;
import io.homeey.matrix.rpc.common.URL;
import io.homeey.matrix.rpc.core.Invocation;
import io.homeey.matrix.rpc.spi.SPI;

import java.io.Closeable;

/**
 *
 * @author jt4mrg@gmail.com
 * @since 2026-01-03
 **/
@SPI("netty")
public interface TransportClient extends Closeable {

    /**
     * 初始化客户端（用于 SPI 加载后配置）
     *
     * @param url 服务端地址信息
     */
    void init(URL url);

    /**
     * 发送调用请求并返回结果
     *
     * @param invocation 调用信息，包含要调用的方法和参数
     * @param timeout 超时时间，单位毫秒
     * @return 调用结果
     */
    Result send(Invocation invocation, long timeout);
    
    /**
     * 连接传输客户端
     *
     * @throws Exception 连接过程中可能出现的异常
     */
    void connect() throws Exception;

    /**
     * 检查连接状态
     *
     * @return 是否已连接
     */
    boolean isConnected();
}
