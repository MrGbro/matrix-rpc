package io.homeey.matrix.rpc.transport.api;

import io.homeey.matrix.rpc.common.Result;
import io.homeey.matrix.rpc.core.Invocation;

/**
 *
 * @author jt4mrg@gmail.com
 * @since 2026-01-03
 **/
@FunctionalInterface
public interface RequestHandler {
    /**
     * 处理RPC请求
     *
     * @param invocation 调用信息
     * @return 调用结果
     */
    Result handle(Invocation invocation);
}