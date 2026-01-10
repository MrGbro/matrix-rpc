package io.homeey.matrix.rpc.cluster.failover;

import io.homeey.matrix.rpc.cluster.api.Cluster;
import io.homeey.matrix.rpc.cluster.api.Directory;
import io.homeey.matrix.rpc.cluster.api.LoadBalance;
import io.homeey.matrix.rpc.core.Invoker;
import io.homeey.matrix.rpc.spi.Activate;
import io.homeey.matrix.rpc.transport.api.TransportClient;

import java.util.concurrent.ConcurrentMap;

/**
 * Failover 容错策略工厂
 * 失败后自动切换到其他节点重试，适用于幂等性查询操作
 */
@Activate
public class FailoverCluster implements Cluster {
    
    @Override
    public <T> Invoker<T> join(Directory<T> directory, LoadBalance loadBalance, ConcurrentMap<String, TransportClient> clients) {
        return new FailoverClusterInvoker<>(directory, loadBalance, clients);
    }
}
