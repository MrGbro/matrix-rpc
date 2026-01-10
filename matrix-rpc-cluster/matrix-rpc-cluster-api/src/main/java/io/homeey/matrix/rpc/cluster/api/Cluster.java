package io.homeey.matrix.rpc.cluster.api;

import io.homeey.matrix.rpc.core.Invoker;
import io.homeey.matrix.rpc.spi.SPI;
import io.homeey.matrix.rpc.transport.api.TransportClient;

import java.util.concurrent.ConcurrentMap;

/**
 * 容错策略 SPI 接口
 * 负责将普通 Invoker 包装为具有容错能力的 ClusterInvoker
 * 
 * <p>支持的容错策略：
 * <ul>
 *   <li>failover - 失败重试：失败后自动切换到其他节点重试</li>
 *   <li>failfast - 快速失败：失败后立即抛出异常，不重试</li>
 *   <li>failsafe - 失败安全：失败后忽略异常，返回空结果</li>
 * </ul>
 */
@SPI("failover")
public interface Cluster {
    
    /**
     * 将 Directory 和 LoadBalance 组合成具有容错能力的 ClusterInvoker
     * 
     * @param directory 服务目录，提供服务提供者列表
     * @param loadBalance 负载均衡器
     * @param clients 客户端连接池
     * @param <T> 服务接口类型
     * @return ClusterInvoker
     */
    <T> Invoker<T> join(Directory<T> directory, LoadBalance loadBalance, ConcurrentMap<String, TransportClient> clients);
}
