package io.homeey.matrix.rpc.core.cluster.impl;

import io.homeey.matrix.rpc.core.Invocation;
import io.homeey.matrix.rpc.core.Invoker;
import io.homeey.matrix.rpc.core.cluster.InvokerSelector;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class RoundRobinInvokerSelector implements InvokerSelector {

    private final AtomicInteger index = new AtomicInteger(0);

    @Override
    public <T> Invoker<T> select(List<Invoker<T>> invokers, Invocation invocation) {
        if (invokers == null || invokers.isEmpty()) {
            throw new IllegalStateException("No available invokers");
        }
        int pos = Math.abs(index.getAndIncrement());
        return invokers.get(pos % invokers.size());
    }
}
