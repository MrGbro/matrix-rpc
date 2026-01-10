package io.homeey.matrix.rpc.cluster.failover;

import io.homeey.matrix.rpc.cluster.api.Directory;
import io.homeey.matrix.rpc.cluster.api.LoadBalance;
import io.homeey.matrix.rpc.core.Invocation;
import io.homeey.matrix.rpc.core.Result;
import io.homeey.matrix.rpc.core.RpcException;
import io.homeey.matrix.rpc.core.URL;
import io.homeey.matrix.rpc.transport.api.TransportClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Supplier;

/**
 * 失败安全容错策略
 * 调用失败后忽略异常，返回空结果
 * 适用于对结果不敏感的场景，如日志上报、监控数据收集等
 */
public class FailsafeClusterInvoker<T> extends ClusterInvoker<T> {
    
    private static final Logger logger = LoggerFactory.getLogger(FailsafeClusterInvoker.class);
    
    public FailsafeClusterInvoker(Directory<T> directory,
                                  LoadBalance loadBalance,
                                  ConcurrentMap<String, TransportClient> clients) {
        super(directory, loadBalance, clients);
    }
    
    @Override
    public Result invoke(Invocation invocation) throws RpcException {
        List<URL> providers = getProviders();
        if (providers == null || providers.isEmpty()) {
            logger.warn("[Failsafe] No provider available for service: {}, return empty result",
                    invocation.getServiceName());
            return new Result((Object) null);
        }
        
        // 使用负载均衡选择一个节点
        URL provider = select(invocation, providers);
        
        try {
            // 尝试调用
            return invokeProvider(provider, invocation, getTimeout(invocation));
        } catch (RpcException e) {
            // 捕获异常但不抛出，记录日志后返回空结果
            logger.warn("[Failsafe] Ignore exception for service {} on provider {}: {}",
                    invocation.getServiceName(), provider.getAddress(), e.getMessage());
            return new Result((Object) null);
        }
    }
}
