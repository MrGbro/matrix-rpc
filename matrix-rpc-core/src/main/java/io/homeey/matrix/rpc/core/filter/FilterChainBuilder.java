package io.homeey.matrix.rpc.core.filter;


import io.homeey.matrix.rpc.core.invocation.Invocation;
import io.homeey.matrix.rpc.core.invocation.Result;
import io.homeey.matrix.rpc.core.invoker.Invoker;

import java.util.List;

/**
 * 构建 RPC 调用的 Filter 责任链
 */
public final class FilterChainBuilder {

    private FilterChainBuilder() {
    }

    /**
     * 构建 Invoker 调用链
     *
     * @param invoker 原始 Invoker（通常是 ServiceInvoker 或 RemoteInvoker）
     * @param filters 过滤器组
     */
    public static <T> Invoker<T> buildInvokerChain(
            Invoker<T> invoker,
            List<Filter> filters
    ) {

        Invoker<T> last = invoker;

        for (int i = filters.size() - 1; i >= 0; i--) {
            Filter filter = filters.get(i);
            Invoker<T> next = last;

            last = new Invoker<>() {

                @Override
                public Class<T> getInterface() {
                    return invoker.getInterface();
                }

                @Override
                public Result invoke(Invocation invocation) {
                    return filter.invoke(next, invocation);
                }
            };
        }

        return last;
    }
}
