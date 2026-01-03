package io.homeey.matrix.rpc.core.cluster;


import io.homeey.matrix.rpc.common.Result;
import io.homeey.matrix.rpc.core.Invocation;
import io.homeey.matrix.rpc.core.Invoker;

/**
 *
 * @author jt4mrg@gmail.com
 * @since 2026-01-03
 **/
public interface ClusterInvoker<T> extends Invoker<T> {
    /**
     * 调用服务
     *
     * @param invocation 调用参数
     * @return 调用结果
     */
    Result invoke(Invocation invocation);
}
