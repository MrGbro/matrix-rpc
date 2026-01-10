package io.homeey.matrix.rpc.cluster.failover;

import io.homeey.matrix.rpc.cluster.api.Directory;
import io.homeey.matrix.rpc.cluster.api.LoadBalance;
import io.homeey.matrix.rpc.core.Invocation;
import io.homeey.matrix.rpc.core.Result;
import io.homeey.matrix.rpc.core.RpcException;
import io.homeey.matrix.rpc.core.URL;
import io.homeey.matrix.rpc.transport.api.TransportClient;

import java.util.List;
import java.util.concurrent.ConcurrentMap;

/**
 * 快速失败容错策略
 * 调用失败后立即抛出异常，不进行重试
 * 适用于非幂等操作（如写操作）或对实时性要求高的场景
 */
public class FailfastClusterInvoker<T> extends ClusterInvoker<T> {
    
    public FailfastClusterInvoker(Directory<T> directory,
                                  LoadBalance loadBalance,
                                  ConcurrentMap<String, TransportClient> clients) {
        super(directory, loadBalance, clients);
    }
    
    @Override
    public Result invoke(Invocation invocation) throws RpcException {
        List<URL> providers = getProviders(invocation);
        if (providers == null || providers.isEmpty()) {
            throw new RpcException("No provider available for service: " + invocation.getServiceName());
        }
        
        // 使用负载均衡选择一个节点
        URL provider = select(invocation, providers);
        
        // 直接调用，失败立即抛出异常
        return invokeProvider(provider, invocation, getTimeout(invocation));
    }
}
