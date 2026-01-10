package io.homeey.matrix.rpc.cluster.loadbalance;

import io.homeey.matrix.rpc.cluster.api.LoadBalance;
import io.homeey.matrix.rpc.core.Invocation;
import io.homeey.matrix.rpc.core.URL;
import io.homeey.matrix.rpc.spi.Activate;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 一致性哈希负载均衡策略
 * 相同参数的请求会路由到同一个节点，适用于有状态服务或需要缓存亲和性的场景
 */
@Activate
public class ConsistentHashLoadBalance implements LoadBalance {
    
    private final ConcurrentMap<String, ConsistentHashSelector> selectors = new ConcurrentHashMap<>();
    
    @Override
    public URL select(List<URL> providers, Invocation invocation) {
        if (providers == null || providers.isEmpty()) {
            throw new IllegalStateException("No provider available");
        }
        
        if (providers.size() == 1) {
            return providers.get(0);
        }
        
        // 基于服务名和方法名构建选择器key
        String key = invocation.getServiceName() + "." + invocation.methodName();
        int identityHashCode = System.identityHashCode(providers);
        
        // 获取或创建一致性哈希选择器
        ConsistentHashSelector selector = selectors.get(key);
        if (selector == null || selector.identityHashCode != identityHashCode) {
            selector = new ConsistentHashSelector(providers, invocation.methodName(), identityHashCode);
            selectors.put(key, selector);
        }
        
        return selector.select(invocation);
    }
    
    /**
     * 一致性哈希选择器
     */
    private static class ConsistentHashSelector {
        private final TreeMap<Long, URL> virtualNodes;
        private final int identityHashCode;
        private final int replicaNumber;
        
        ConsistentHashSelector(List<URL> providers, String methodName, int identityHashCode) {
            this.virtualNodes = new TreeMap<>();
            this.identityHashCode = identityHashCode;
            this.replicaNumber = 160; // 每个真实节点对应160个虚拟节点
            
            // 为每个服务提供者创建虚拟节点
            for (URL provider : providers) {
                String address = provider.getAddress();
                for (int i = 0; i < replicaNumber / 4; i++) {
                    byte[] digest = md5(address + i);
                    for (int h = 0; h < 4; h++) {
                        long m = hash(digest, h);
                        virtualNodes.put(m, provider);
                    }
                }
            }
        }
        
        public URL select(Invocation invocation) {
            String key = toKey(invocation.arguments());
            byte[] digest = md5(key);
            return selectForKey(hash(digest, 0));
        }
        
        private URL selectForKey(long hash) {
            Map.Entry<Long, URL> entry = virtualNodes.ceilingEntry(hash);
            if (entry == null) {
                entry = virtualNodes.firstEntry();
            }
            return entry.getValue();
        }
        
        private String toKey(Object[] args) {
            if (args == null || args.length == 0) {
                return "";
            }
            StringBuilder buf = new StringBuilder();
            for (Object arg : args) {
                if (arg != null) {
                    buf.append(arg.toString());
                }
            }
            return buf.toString();
        }
        
        private byte[] md5(String value) {
            try {
                MessageDigest md5 = MessageDigest.getInstance("MD5");
                return md5.digest(value.getBytes(StandardCharsets.UTF_8));
            } catch (NoSuchAlgorithmException e) {
                throw new IllegalStateException("MD5 algorithm not available", e);
            }
        }
        
        private long hash(byte[] digest, int number) {
            return (((long) (digest[3 + number * 4] & 0xFF) << 24)
                    | ((long) (digest[2 + number * 4] & 0xFF) << 16)
                    | ((long) (digest[1 + number * 4] & 0xFF) << 8)
                    | (digest[number * 4] & 0xFF))
                    & 0xFFFFFFFFL;
        }
    }
}
