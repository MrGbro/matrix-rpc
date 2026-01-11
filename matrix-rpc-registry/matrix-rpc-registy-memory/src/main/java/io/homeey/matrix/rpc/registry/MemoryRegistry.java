package io.homeey.matrix.rpc.registry;


import io.homeey.matrix.rpc.core.URL;
import io.homeey.matrix.rpc.registry.api.NotifyListener;
import io.homeey.matrix.rpc.registry.api.Registry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 内存注册中心实现 - 用于本地测试
 */
public class MemoryRegistry implements Registry {
    private static final Logger logger = LoggerFactory.getLogger(MemoryRegistry.class);
    
    // 单例，保证Provider和Consumer共享注册信息
    private static final MemoryRegistry INSTANCE = new MemoryRegistry();
    
    // 服务接口 -> 提供者列表
    private final ConcurrentMap<String, List<URL>> services = new ConcurrentHashMap<>();
    
    // 服务接口 -> 订阅者列表
    private final ConcurrentMap<String, List<NotifyListener>> listeners = new ConcurrentHashMap<>();
    
    private MemoryRegistry() {
        // 私有构造函数
    }
    
    public static MemoryRegistry getInstance() {
        return INSTANCE;
    }
    
    @Override
    public void register(URL url) {
        String serviceKey = url.getPath();
        services.computeIfAbsent(serviceKey, k -> new CopyOnWriteArrayList<>()).add(url);
        logger.info("Service registered: {}", url);
        
        // 通知订阅者
        notifyListeners(serviceKey);
    }

    @Override
    public void unregister(URL url) {
        String serviceKey = url.getPath();
        List<URL> urls = services.get(serviceKey);
        if (urls != null) {
            urls.remove(url);
            logger.info("Service unregistered: {}", url);
            notifyListeners(serviceKey);
        }
    }

    @Override
    public List<URL> lookup(String serviceInterface, String group, String version) {
        List<URL> urls = services.get(serviceInterface);
        if (urls == null || urls.isEmpty()) {
            logger.debug("No providers found for: {}", serviceInterface);
            return new ArrayList<>();
        }
        logger.debug("Found {} providers for: {}", urls.size(), serviceInterface);
        return new ArrayList<>(urls);
    }

    @Override
    public void subscribe(String serviceInterface, NotifyListener listener) {
        listeners.computeIfAbsent(serviceInterface, k -> new CopyOnWriteArrayList<>()).add(listener);
        logger.debug("Service subscribed: {}", serviceInterface);
        
        // 立即通知当前已注册的服务
        List<URL> urls = services.get(serviceInterface);
        if (urls != null && !urls.isEmpty()) {
            listener.notify(new ArrayList<>(urls));
        }
    }
    
    private void notifyListeners(String serviceKey) {
        List<NotifyListener> subscribedListeners = listeners.get(serviceKey);
        if (subscribedListeners != null) {
            List<URL> urls = services.getOrDefault(serviceKey, new ArrayList<>());
            for (NotifyListener listener : subscribedListeners) {
                listener.notify(new ArrayList<>(urls));
            }
        }
    }
}
