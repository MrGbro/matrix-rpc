package io.homeey.matrix.rpc.core.cluster;


import io.homeey.matrix.rpc.core.Invocation;
import io.homeey.matrix.rpc.core.Invoker;

import java.util.List;

public interface InvokerSelector {

    /**
     * 从可用的调用者列表中选择一个调用者来执行指定的调用
     *
     * @param invokers 可用的调用者列表
     * @param invocation 要执行的调用
     * @return 选中的调用者，如果没有可用的调用者则返回 null
     */
    <T> Invoker<T> select(List<Invoker<T>> invokers, Invocation invocation);
}
