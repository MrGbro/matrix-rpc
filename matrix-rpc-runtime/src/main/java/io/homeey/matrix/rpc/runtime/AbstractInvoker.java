package io.homeey.matrix.rpc.runtime;

import io.homeey.matrix.rpc.core.Invoker;

public abstract class AbstractInvoker<T> implements Invoker<T> {
    private final Class<T> type;

    public AbstractInvoker(Class<T> type) {
        this.type = type;
    }

    @Override
    public Class<T> getInterface() {
        return type;
    }
}