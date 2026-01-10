package io.homeey.matrix.rpc.cluster.failover;

import io.homeey.matrix.rpc.cluster.api.Cluster;
import io.homeey.matrix.rpc.cluster.api.Directory;
import io.homeey.matrix.rpc.cluster.api.LoadBalance;
import io.homeey.matrix.rpc.core.Invoker;
import io.homeey.matrix.rpc.spi.Activate;
import io.homeey.matrix.rpc.transport.api.TransportClient;

import java.util.concurrent.ConcurrentMap;

/**
 * Failfast 容错策略工厂
 * 失败后立即抛出异常，不重试，适用于非幂等写操作
 */
@Activate
public class FailfastCluster implements Cluster {
    
    @Override
    public <T> Invoker<T> join(Directory<T> directory, LoadBalance loadBalance, ConcurrentMap<String, TransportClient> clients) {
        return new FailfastClusterInvoker<>(directory, loadBalance, clients);
    }
}
