package io.homeey.matrix.rpc.core.protocol.local;

import io.homeey.matrix.rpc.core.invoker.Invoker;
import io.homeey.matrix.rpc.core.protocol.Exporter;

public class LocalExporter<T> implements Exporter<T> {

    private final Invoker<T> invoker;

    public LocalExporter(Invoker<T> invoker) {
        this.invoker = invoker;
    }

    @Override
    public Invoker<T> getInvoker() {
        return invoker;
    }

    @Override
    public void unexport() {
        // 本地协议可先不实现
    }
}
