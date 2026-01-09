package io.homeey.matrix.rpc.runtime.support;

import io.homeey.matrix.rpc.core.Result;
import io.homeey.matrix.rpc.core.RpcException;
import io.homeey.matrix.rpc.core.Invocation;
import io.homeey.matrix.rpc.core.Invoker;
import io.homeey.matrix.rpc.filter.Filter;
import io.homeey.matrix.rpc.spi.Activate;
import io.homeey.matrix.rpc.spi.ExtensionLoader;

import java.util.Comparator;
import java.util.List;

public class FilterChainBuilder {

    /**
     * 构建过滤器责任链
     *
     * @param invoker 调用者
     * @param group   过滤器分组
     * @return 包装了过滤器链的调用者
     */
    public static <T> Invoker<T> buildInvokerChain(Invoker<T> invoker, String group) {
        List<Filter> filters = ExtensionLoader.getExtensionLoader(Filter.class)
                .getActivateExtensions(group);

        // 按order排序
        filters = filters.stream()
                .sorted(Comparator.comparingInt(f -> f.getClass().getAnnotation(Activate.class).order()))
                .toList();

        // 构建责任链
        Invoker<T> last = invoker;
        for (int i = filters.size() - 1; i >= 0; i--) {
            Filter filter = filters.get(i);
            last = new FilterInvoker<>(last, filter);
        }
        return last;
    }

    private record FilterInvoker<T>(Invoker<T> invoker, Filter filter) implements Invoker<T> {

        @Override
        public Class<T> getInterface() {
            return invoker.getInterface();
        }

        @Override
        public Result invoke(Invocation invocation) throws RpcException {
            return filter.invoke(invoker, invocation);
        }
    }
}