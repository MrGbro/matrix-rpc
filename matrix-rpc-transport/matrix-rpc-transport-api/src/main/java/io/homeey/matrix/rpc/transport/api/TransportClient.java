package io.homeey.matrix.rpc.transport.api;

import io.homeey.matrix.rpc.common.Result;
import io.homeey.matrix.rpc.core.Invocation;

import java.io.Closeable;

/**
 *
 * @author jt4mrg@gmail.com
 * @since 2026-01-03
 **/
public interface TransportClient extends Closeable {

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
}
