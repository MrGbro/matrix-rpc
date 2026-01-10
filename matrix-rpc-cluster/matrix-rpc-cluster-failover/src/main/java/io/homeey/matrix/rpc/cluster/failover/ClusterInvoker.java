package io.homeey.matrix.rpc.cluster.failover;

import io.homeey.matrix.rpc.cluster.api.Directory;
import io.homeey.matrix.rpc.cluster.api.LoadBalance;
import io.homeey.matrix.rpc.core.Invocation;
import io.homeey.matrix.rpc.core.Invoker;
import io.homeey.matrix.rpc.core.Result;
import io.homeey.matrix.rpc.core.RpcException;
import io.homeey.matrix.rpc.core.URL;
import io.homeey.matrix.rpc.transport.api.TransportClient;

import java.util.List;
import java.util.concurrent.ConcurrentMap;

/**
 * 容错 Invoker 抽象基类
 * 提供负载均衡、客户端管理等通用能力，子类实现具体的容错策略
 * 
 * @param <T> 服务接口类型
 */
public abstract class ClusterInvoker<T> implements Invoker<T> {
    
    protected final Directory<T> directory;
    protected final LoadBalance loadBalance;
    protected final ConcurrentMap<String, TransportClient> clients;
    
    public ClusterInvoker(Directory<T> directory,
                          LoadBalance loadBalance,
                          ConcurrentMap<String, TransportClient> clients) {
        this.directory = directory;
        this.loadBalance = loadBalance;
        this.clients = clients;
    }
    
    @Override
    public Class<T> getInterface() {
        return directory.getInterface();
    }
    
    /**
     * 使用负载均衡策略选择一个服务提供者
     */
    protected URL select(Invocation invocation, List<URL> availableProviders) {
        if (availableProviders == null || availableProviders.isEmpty()) {
            throw new RpcException("No provider available for service: " + invocation.getServiceName());
        }
        return loadBalance.select(availableProviders, invocation);
    }
    
    /**
     * 调用指定的服务提供者
     */
    protected Result invokeProvider(URL provider, Invocation invocation, long timeout) {
        TransportClient client = clients.get(provider.getAddress());
        if (client == null) {
            throw new RpcException("No client available for provider: " + provider.getAddress());
        }
        return client.send(invocation, timeout);
    }
    
    /**
     * 获取超时时间（毫秒）
     */
    protected long getTimeout(Invocation invocation) {
        String timeout = invocation.getAttachments().get("timeout");
        return timeout != null ? Long.parseLong(timeout) : 3000L;
    }
    
    /**
     * 获取当前可用的服务提供者列表
     */
    protected List<URL> getProviders() {
        return directory.list();
    }
}
