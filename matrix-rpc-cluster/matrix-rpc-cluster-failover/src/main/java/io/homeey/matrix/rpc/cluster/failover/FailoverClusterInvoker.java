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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentMap;

/**
 * 失败重试容错策略
 * 调用失败后自动切换到其他节点重试，适用于幂等性查询操作
 */
public class FailoverClusterInvoker<T> extends ClusterInvoker<T> {
    
    private static final Logger logger = LoggerFactory.getLogger(FailoverClusterInvoker.class);
    private static final int DEFAULT_RETRIES = 2;
    
    public FailoverClusterInvoker(Directory<T> directory,
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
        
        // 获取重试次数配置
        int retries = getRetries(invocation);
        List<URL> availableProviders = new ArrayList<>(providers);
        RpcException lastException = null;
        
        // 尝试调用（初次调用 + 重试）
        for (int i = 0; i <= retries; i++) {
            if (availableProviders.isEmpty()) {
                String msg = String.format("No provider available for service %s after %d attempts",
                        invocation.getServiceName(), i);
                logger.error(msg);
                throw new RpcException(msg, lastException);
            }
            
            // 使用负载均衡选择节点
            URL provider = select(invocation, availableProviders);
            
            try {
                if (i > 0) {
                    logger.warn("Failover retry {} for service {} on provider {}",
                            i, invocation.getServiceName(), provider.getAddress());
                }
                
                Result result = invokeProvider(provider, invocation, getTimeout(invocation));
                
                // 如果调用成功（没有异常），直接返回
                if (!result.hasException()) {
                    if (i > 0) {
                        logger.info("Failover succeeded after {} retries for service {}",
                                i, invocation.getServiceName());
                    }
                    return result;
                }
                
                // 业务异常也当作失败
                lastException = new RpcException("Business exception", result.getException());
                logger.warn("Business exception on provider {}: {}",
                        provider.getAddress(), result.getException().getMessage());
                
            } catch (RpcException e) {
                lastException = e;
                logger.warn("RPC exception on provider {}: {}",
                        provider.getAddress(), e.getMessage());
            }
            
            // 失败后移除该节点，下次重试选择其他节点
            availableProviders.remove(provider);
        }
        
        // 所有重试都失败
        String msg = String.format("Failover failed for service %s after %d retries",
                invocation.getServiceName(), retries);
        logger.error(msg, lastException);
        throw lastException != null ? lastException : new RpcException(msg);
    }
    
    /**
     * 获取重试次数配置
     * 从调用参数中获取，默认为 2 次
     */
    private int getRetries(Invocation invocation) {
        String retriesStr = invocation.getAttachments().get("retries");
        if (retriesStr != null) {
            try {
                return Integer.parseInt(retriesStr);
            } catch (NumberFormatException e) {
                logger.warn("Invalid retries value: {}, use default {}", retriesStr, DEFAULT_RETRIES);
            }
        }
        return DEFAULT_RETRIES;
    }
}
