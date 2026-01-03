package io.homeey.matrix.rpc.core.remoting;

import java.util.concurrent.TimeUnit;

/**
 *
 * @author jt4mrg@gmail.com
 * @since 2026-01-02
 **/
public interface ResponseFuture {

    /**
     * 获取响应结果
     *
     * @return 响应结果对象
     * @throws Exception 获取过程中发生的异常
     */
    Object get() throws Exception;

    /**
     * 在指定超时时间内获取响应结果
     *
     * @param timeout 超时时间
     * @param unit    时间单位
     * @return 响应结果对象
     * @throws Exception 获取过程中发生的异常
     */
    Object get(long timeout, TimeUnit unit) throws Exception;

    /**
     * 判断请求是否已完成
     *
     * @return 如果已完成则返回true，否则返回false
     */
    boolean isDone();
}
