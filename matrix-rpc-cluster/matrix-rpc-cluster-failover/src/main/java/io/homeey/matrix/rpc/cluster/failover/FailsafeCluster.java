package io.homeey.matrix.rpc.cluster.failover;

import io.homeey.matrix.rpc.cluster.api.Cluster;
import io.homeey.matrix.rpc.cluster.api.Directory;
import io.homeey.matrix.rpc.cluster.api.LoadBalance;
import io.homeey.matrix.rpc.core.Invoker;
import io.homeey.matrix.rpc.spi.Activate;
import io.homeey.matrix.rpc.transport.api.TransportClient;

import java.util.concurrent.ConcurrentMap;

/**
 * Failsafe 容错策略工厂
 * 失败后忽略异常返回空结果，适用于日志上报等对结果不敏感的场景
 */
@Activate
public class FailsafeCluster implements Cluster {
    
    @Override
    public <T> Invoker<T> join(Directory<T> directory, LoadBalance loadBalance, ConcurrentMap<String, TransportClient> clients) {
        return new FailsafeClusterInvoker<>(directory, loadBalance, clients);
    }
}
