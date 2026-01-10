package io.homeey.matrix.rpc.cluster.loadbalance;

import io.homeey.matrix.rpc.cluster.api.LoadBalance;
import io.homeey.matrix.rpc.core.Invocation;
import io.homeey.matrix.rpc.core.URL;
import io.homeey.matrix.rpc.spi.Activate;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 轮询负载均衡策略
 * 按顺序依次选择服务提供者，保证请求均匀分布
 */
@Activate
public class RoundRobinLoadBalance implements LoadBalance {
    
    private final ConcurrentMap<String, AtomicInteger> sequences = new ConcurrentHashMap<>();
    
    @Override
    public URL select(List<URL> providers, Invocation invocation) {
        if (providers == null || providers.isEmpty()) {
            throw new IllegalStateException("No provider available");
        }
        
        if (providers.size() == 1) {
            return providers.get(0);
        }
        
        // 基于服务名生成轮询序列号
        String key = invocation.getServiceName();
        AtomicInteger sequence = sequences.computeIfAbsent(key, k -> new AtomicInteger(0));
        
        // 获取下一个索引（取模确保不越界）
        int currentSequence = sequence.getAndIncrement();
        int index = Math.abs(currentSequence % providers.size());
        
        return providers.get(index);
    }
}
