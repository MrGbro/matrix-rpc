package io.homeey.matrix.rpc.core.cluster;


import io.homeey.matrix.rpc.core.Invocation;
import io.homeey.matrix.rpc.core.Invoker;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

public abstract class AbstractDirectory<T> implements Directory<T> {

    private final AtomicReference<List<Invoker<T>>> invokers =
            new AtomicReference<>(Collections.emptyList());

    protected void refreshInvokers(List<Invoker<T>> newInvokers) {
        invokers.set(newInvokers);
    }

    protected List<Invoker<T>> getInvokers() {
        return invokers.get();
    }

    @Override
    public List<Invoker<T>> list(Invocation invocation) {
        return getInvokers();
    }
}
