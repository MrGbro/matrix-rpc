package io.homeey.matrix.rpc.runtime;

import io.homeey.matrix.rpc.core.Exporter;
import io.homeey.matrix.rpc.core.Invoker;

public abstract class AbstractExporter<T> implements Exporter<T> {
    private final Invoker<T> invoker;

    public AbstractExporter(Invoker<T> invoker) {
        this.invoker = invoker;
    }

    @Override
    public Invoker<T> getInvoker() {
        return invoker;
    }
}