package io.homeey.matrix.rpc.core.impl;

import io.homeey.matrix.rpc.common.URL;
import io.homeey.matrix.rpc.core.Exporter;
import io.homeey.matrix.rpc.core.Invoker;
import io.homeey.matrix.rpc.core.Protocol;

public abstract class AbstractProtocol implements Protocol {
    @Override
    public <T> Exporter<T> export(Invoker<T> invoker, URL url) {
        return createExporter(invoker, url);
    }

    @Override
    public <T> Invoker<T> refer(Class<T> type, URL url) {
        return createInvoker(type, url);
    }

    protected abstract <T> Exporter<T> createExporter(Invoker<T> invoker, URL url);

    protected abstract <T> Invoker<T> createInvoker(Class<T> type, URL url);
}
