package io.homeey.matrix.rpc.core.invoker;

import io.homeey.matrix.rpc.common.Result;
import io.homeey.matrix.rpc.common.URL;
import io.homeey.matrix.rpc.core.Invocation;
import io.homeey.matrix.rpc.core.Invoker;

/**
 *
 * @author jt4mrg@gmail.com
 * @since 2026-01-02
 **/
public record InvokerWrapper<T>(Invoker<T> delegate, URL url) implements Invoker<T> {

    @Override
    public Class<T> getInterface() {
        return delegate.getInterface();
    }

    @Override
    public Result invoke(Invocation invocation) {
        return delegate.invoke(invocation);
    }
}
