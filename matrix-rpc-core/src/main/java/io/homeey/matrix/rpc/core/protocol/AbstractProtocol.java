package io.homeey.matrix.rpc.core.protocol;

import io.homeey.matrix.rpc.common.URL;
import io.homeey.matrix.rpc.core.filter.Filter;
import io.homeey.matrix.rpc.core.filter.FilterChainBuilder;
import io.homeey.matrix.rpc.core.filter.FilterLoader;
import io.homeey.matrix.rpc.core.filter.FilterScope;
import io.homeey.matrix.rpc.core.invoker.Invoker;

import java.util.List;

/**
 *
 * @author jt4mrg@gmail.com
 * @since 2026-01-02
 **/
public abstract class AbstractProtocol implements Protocol {
    @Override
    public <T> Invoker<T> refer(Class<T> type, URL url) {
        Invoker<T> invoker = createInvoker(type, url);
        // 👇 Consumer Filter 注入点
        List<Filter> filters =
                FilterLoader.loadFilters(FilterScope.CONSUMER);

        return FilterChainBuilder.buildInvokerChain(invoker, filters);
    }

    protected abstract <T> Invoker<T> createInvoker(Class<T> type, URL url);
}
