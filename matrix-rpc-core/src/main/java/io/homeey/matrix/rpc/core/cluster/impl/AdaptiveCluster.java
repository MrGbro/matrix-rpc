package io.homeey.matrix.rpc.core.cluster.impl;


import io.homeey.matrix.rpc.common.Result;
import io.homeey.matrix.rpc.core.Invocation;
import io.homeey.matrix.rpc.core.Invoker;
import io.homeey.matrix.rpc.core.cluster.Cluster;
import io.homeey.matrix.rpc.core.cluster.Directory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class AdaptiveCluster implements Cluster {

    private final Map<String, Cluster> clusterMap = new ConcurrentHashMap<>();

    public AdaptiveCluster() {
        clusterMap.put("failover", new FailoverCluster());
        clusterMap.put("failfast", new FailfastCluster());
        clusterMap.put("failsafe", new FailsafeCluster());
    }

    @Override
    public <T> Invoker<T> join(Directory<T> directory) {
        return new Invoker<T>() {
            @Override
            public Class<T> getInterface() {
                return directory.getInterface();
            }

            @Override
            public Result invoke(Invocation invocation) {
                String clusterName = invocation.getAttachments().get("cluster");
                if (clusterName == null) {
                    clusterName = "failover";
                }
                Cluster cluster = clusterMap.get(clusterName);
                if (cluster == null) {
                    throw new IllegalStateException("Unknown cluster: " + clusterName);
                }
                return cluster.join(directory).invoke(invocation);
            }
        };
    }
}
