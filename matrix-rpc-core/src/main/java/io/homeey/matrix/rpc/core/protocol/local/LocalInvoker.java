package io.homeey.matrix.rpc.core.protocol.local;

import io.homeey.matrix.rpc.core.invocation.Invocation;
import io.homeey.matrix.rpc.core.invocation.Result;
import io.homeey.matrix.rpc.core.invoker.Invoker;

public class LocalInvoker<T> implements Invoker<T> {

    private final Invoker<T> delegate;

    public LocalInvoker(Invoker<T> delegate) {
        this.delegate = delegate;
    }

    @Override
    public Class<T> getInterface() {
        return delegate.getInterface();
    }

    @Override
    public Result invoke(Invocation invocation) {
        return delegate.invoke(invocation);
    }
}
