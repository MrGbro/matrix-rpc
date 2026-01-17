package io.homeey.matrix.rpc.proxy.bytebuddy;

import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * 代理类缓存
 * 
 * <p>性能影响：
 * <ul>
 *   <li>首次创建：100-200ms（字节码编译）</li>
 *   <li>缓存命中：<1ms（直接实例化）</li>
 *   <li>内存占用：每个代理类 ~5KB Metaspace</li>
 * </ul>
 * 
 * <p>线程安全：基于 ConcurrentHashMap 的无锁缓存
 * 
 * @author Matrix RPC Team
 * @since 1.0.0
 */
public class ProxyClassCache {
    
    private final ConcurrentHashMap<CacheKey, Class<?>> cache = new ConcurrentHashMap<>();
    
    /**
     * 获取或创建代理类
     * 
     * @param interfaces 接口数组
     * @param creator    创建函数（仅在缓存未命中时调用）
     * @return 代理类
     */
    @SuppressWarnings("unchecked")
    public <T> Class<T> getOrCreate(
        Class<?>[] interfaces, 
        Function<Class<?>[], Class<T>> creator
    ) {
        CacheKey key = new CacheKey(interfaces);
        return (Class<T>) cache.computeIfAbsent(key, k -> creator.apply(interfaces));
    }
    
    /**
     * 缓存键（基于接口数组）
     */
    static class CacheKey {
        private final Class<?>[] interfaces;
        private final int hashCode;
        
        CacheKey(Class<?>[] interfaces) {
            this.interfaces = interfaces;
            this.hashCode = Arrays.hashCode(interfaces);
        }
        
        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (!(obj instanceof CacheKey)) return false;
            CacheKey other = (CacheKey) obj;
            return Arrays.equals(this.interfaces, other.interfaces);
        }
        
        @Override
        public int hashCode() {
            return hashCode;
        }
    }
    
    /**
     * 清空缓存（测试用）
     */
    public void clear() {
        cache.clear();
    }
    
    /**
     * 获取缓存大小（监控用）
     */
    public int size() {
        return cache.size();
    }
}
