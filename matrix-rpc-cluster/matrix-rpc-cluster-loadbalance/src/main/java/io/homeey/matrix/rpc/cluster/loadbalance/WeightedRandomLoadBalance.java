package io.homeey.matrix.rpc.cluster.loadbalance;

import io.homeey.matrix.rpc.cluster.api.LoadBalance;
import io.homeey.matrix.rpc.core.Invocation;
import io.homeey.matrix.rpc.core.URL;
import io.homeey.matrix.rpc.spi.Activate;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 加权随机负载均衡策略
 * 根据服务提供者的权重进行加权随机选择
 * 权重通过 URL 参数 "weight" 指定，默认为 100
 */
@Activate
public class WeightedRandomLoadBalance implements LoadBalance {
    
    @Override
    public URL select(List<URL> providers, Invocation invocation) {
        if (providers == null || providers.isEmpty()) {
            throw new IllegalStateException("No provider available");
        }
        
        if (providers.size() == 1) {
            return providers.get(0);
        }
        
        // 计算总权重并判断是否所有权重相同
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
        
        // 如果权重不同且总权重大于0，执行加权随机
        if (!sameWeight && totalWeight > 0) {
            int offset = ThreadLocalRandom.current().nextInt(totalWeight);
            
            for (URL provider : providers) {
                offset -= getWeight(provider);
                if (offset < 0) {
                    return provider;
                }
            }
        }
        
        // 权重相同或总权重为0时，退化为简单随机
        return providers.get(ThreadLocalRandom.current().nextInt(providers.size()));
    }
    
    /**
     * 获取服务提供者的权重
     * @param url 服务提供者URL
     * @return 权重值，默认为100
     */
    private int getWeight(URL url) {
        int weight = url.getParameter("weight", 100);
        return Math.max(weight, 0); // 确保权重非负
    }
}
