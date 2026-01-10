package io.homeey.matrix.rpc.cluster.loadbalance;

import io.homeey.matrix.rpc.cluster.api.LoadBalance;
import io.homeey.matrix.rpc.core.Invocation;
import io.homeey.matrix.rpc.core.URL;
import io.homeey.matrix.rpc.spi.Activate;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 加权轮询负载均衡
 * 
 * <p>基于平滑加权轮询算法（Smooth Weighted Round-Robin），参考 Nginx 实现。
 * 
 * <p>权重配置：
 * <ul>
 *   <li>URL 参数：weight=200 （默认 100）</li>
 *   <li>权重范围：1-1000</li>
 * </ul>
 * 
 * <p>算法特点：
 * <ul>
 *   <li>平滑分布：权重大的实例会均匀分散在调用序列中</li>
 *   <li>无状态：每次选择都基于当前权重状态</li>
 *   <li>公平性：长期调用比例严格按权重分配</li>
 * </ul>
 * 
 * <p>使用示例：
 * <pre>
 * // Provider 端设置权重
 * RpcService.export(EchoService.class, new EchoServiceImpl(), 20880)
 *     .weight(200)  // 设置权重为 200（默认 100）
 *     .await();
 * 
 * // Consumer 端指定负载均衡策略
 * RpcReference.create(EchoService.class)
 *     .address("localhost", 20880)
 *     .loadbalance("weightedRoundRobin")  // 使用加权轮询
 *     .get();
 * </pre>
 * 
 * @author Matrix RPC Team
 * @since 2026-01-10
 */
@Activate
public class WeightedRoundRobinLoadBalance implements LoadBalance {
    
    private static final String WEIGHT_KEY = "weight";
    private static final int DEFAULT_WEIGHT = 100;
    
    // 每个服务维护一个权重状态缓存
    private final ConcurrentHashMap<String, WeightedRoundRobin> weightMap = new ConcurrentHashMap<>();
    
    @Override
    public URL select(List<URL> providers, Invocation invocation) {
        if (providers == null || providers.isEmpty()) {
            throw new IllegalStateException("No provider available");
        }
        
        if (providers.size() == 1) {
            return providers.get(0);
        }
        
        String serviceKey = invocation.getServiceName();
        WeightedRoundRobin roundRobin = weightMap.computeIfAbsent(serviceKey, k -> new WeightedRoundRobin());
        
        return roundRobin.select(providers);
    }
    
    /**
     * 获取 URL 的权重值
     */
    private int getWeight(URL url) {
        int weight = url.getParameter(WEIGHT_KEY, DEFAULT_WEIGHT);
        return Math.max(1, Math.min(weight, 1000));
    }
    
    /**
     * 平滑加权轮询实现
     */
    private class WeightedRoundRobin {
        // 存储每个 URL 的当前权重
        private final ConcurrentHashMap<String, AtomicLong> currentWeights = new ConcurrentHashMap<>();
        
        public URL select(List<URL> providers) {
            int totalWeight = 0;
            URL selectedURL = null;
            long maxCurrentWeight = Long.MIN_VALUE;
            
            // 1. 遍历所有提供者
            for (URL provider : providers) {
                String address = provider.getAddress();
                int weight = getWeight(provider);
                totalWeight += weight;
                
                // 获取或初始化当前权重
                AtomicLong currentWeight = currentWeights.computeIfAbsent(address, k -> new AtomicLong(0));
                
                // 2. 当前权重 += 配置权重
                long current = currentWeight.addAndGet(weight);
                
                // 3. 选择当前权重最大的实例
                if (current > maxCurrentWeight) {
                    maxCurrentWeight = current;
                    selectedURL = provider;
                }
            }
            
            // 4. 被选中的实例当前权重 -= 总权重
            if (selectedURL != null) {
                String address = selectedURL.getAddress();
                AtomicLong currentWeight = currentWeights.get(address);
                if (currentWeight != null) {
                    currentWeight.addAndGet(-totalWeight);
                }
            }
            
            // 5. 清理不再存在的实例
            cleanupStaleEntries(providers);
            
            return selectedURL != null ? selectedURL : providers.get(0);
        }
        
        /**
         * 清理已经不存在的实例的权重记录
         */
        private void cleanupStaleEntries(List<URL> providers) {
            if (currentWeights.size() > providers.size() * 2) {
                // 只保留当前存在的实例
                currentWeights.keySet().retainAll(
                    providers.stream()
                        .map(URL::getAddress)
                        .toList()
                );
            }
        }
    }
}
