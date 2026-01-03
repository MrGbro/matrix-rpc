package io.homeey.matrix.rpc.core.invoker;

import io.homeey.matrix.rpc.core.Invoker;

/**
 *
 * @author jt4mrg@gmail.com
 * @since 2026-01-02
 **/
public abstract class AbstractInvoker<T> implements Invoker<T> {

    private final Class<T> type;

    protected AbstractInvoker(Class<T> type) {
        this.type = type;
    }

    @Override
    public Class<T> getInterface() {
        return type;
    }
}
