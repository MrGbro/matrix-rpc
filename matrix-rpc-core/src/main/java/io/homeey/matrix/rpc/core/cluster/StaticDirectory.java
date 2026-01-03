package io.homeey.matrix.rpc.core.cluster;


import io.homeey.matrix.rpc.core.Invoker;

import java.util.List;

public class StaticDirectory<T> extends AbstractDirectory<T> {

    public StaticDirectory(List<Invoker<T>> invokers) {
        refreshInvokers(invokers);
    }

    @Override
    public Class<T> getInterface() {
        return null;
    }
}
