package io.homeey.matrix.rpc.core.cluster.impl;


import io.homeey.matrix.rpc.common.Result;
import io.homeey.matrix.rpc.core.Invocation;
import io.homeey.matrix.rpc.core.Invoker;
import io.homeey.matrix.rpc.core.cluster.Cluster;
import io.homeey.matrix.rpc.core.cluster.Directory;

import java.util.List;

public class FailfastCluster implements Cluster {

    @Override
    public <T> Invoker<T> join(Directory<T> directory) {
        return new Invoker<T>() {
            @Override
            public Class<T> getInterface() {
                return directory.getInterface();
            }

            @Override
            public Result invoke(Invocation invocation) {
                List<Invoker<T>> list = directory.list(invocation);
                if (list.isEmpty()) {
                    throw new IllegalStateException("No available invokers");
                }
                return list.get(0).invoke(invocation);
            }
        };
    }
}
