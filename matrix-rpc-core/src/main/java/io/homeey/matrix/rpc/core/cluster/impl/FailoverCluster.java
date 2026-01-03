package io.homeey.matrix.rpc.core.cluster.impl;


import io.homeey.matrix.rpc.core.Invoker;
import io.homeey.matrix.rpc.core.cluster.Cluster;
import io.homeey.matrix.rpc.core.cluster.Directory;

public class FailoverCluster implements Cluster {

    @Override
    public <T> Invoker<T> join(Directory<T> directory) {
        return new FailoverClusterInvoker<>(
                directory,
                new RoundRobinInvokerSelector(),
                new DefaultRetryPolicy(2)
        );
    }
}
