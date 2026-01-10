package io.homeey.matrix.rpc.cluster.loadbalance;

import io.homeey.matrix.rpc.cluster.api.LoadBalance;
import io.homeey.matrix.rpc.core.Invocation;
import io.homeey.matrix.rpc.core.URL;
import io.homeey.matrix.rpc.spi.Activate;

import java.util.List;
import java.util.Random;

/**
 * 加权随机负载均衡
 * 
 * <p>权重配置：
 * <ul>
 *   <li>URL 参数：weight=200 （默认 100）</li>
 *   <li>权重范围：1-1000</li>
 * </ul>
 * 
 * <p>算法说明：
 * <ol>
 *   <li>计算所有实例的总权重</li>
 *   <li>如果所有实例权重相同，直接随机选择</li>
 *   <li>否则，按权重比例随机选择</li>
 * </ol>
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
 *     .loadbalance("weightedRandom")  // 使用加权随机
 *     .get();
 * </pre>
 * 
 * @author Matrix RPC Team
 * @since 2026-01-10
 */
@Activate
public class WeightedRandomLoadBalance implements LoadBalance {
    
    private final Random random = new Random();
    private static final String WEIGHT_KEY = "weight";
    private static final int DEFAULT_WEIGHT = 100;
    
    @Override
    public URL select(List<URL> providers, Invocation invocation) {
        if (providers == null || providers.isEmpty()) {
            throw new IllegalStateException("No provider available");
        }
        
        if (providers.size() == 1) {
            return providers.get(0);
        }
        
        // 1. 计算总权重并判断权重是否相同
        int totalWeight = 0;
        boolean sameWeight = true;
        int firstWeight = getWeight(providers.get(0));
        
        for (URL provider : providers) {
            int weight = getWeight(provider);
            totalWeight += weight;
            if (sameWeight && weight != firstWeight) {
                sameWeight = false;
            }
        }
        
        // 2. 如果权重都相同，直接随机选择（性能优化）
        if (sameWeight) {
            return providers.get(random.nextInt(providers.size()));
        }
        
        // 3. 按权重随机选择
        int offset = random.nextInt(totalWeight);
        for (URL provider : providers) {
            offset -= getWeight(provider);
            if (offset < 0) {
                return provider;
            }
        }
        
        // 兜底：返回第一个
        return providers.get(0);
    }
    
    /**
     * 获取 URL 的权重值
     * 
     * @param url 服务提供者 URL
     * @return 权重值（范围 1-1000，默认 100）
     */
    private int getWeight(URL url) {
        int weight = url.getParameter(WEIGHT_KEY, DEFAULT_WEIGHT);
        // 限制权重范围在 [1, 1000]
        return Math.max(1, Math.min(weight, 1000));
    }
}
