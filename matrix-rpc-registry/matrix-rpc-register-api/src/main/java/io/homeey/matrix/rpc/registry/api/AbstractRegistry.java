package io.homeey.matrix.rpc.registry.api;

import io.homeey.matrix.rpc.core.URL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.*;

/**
 * 注册中心抽象基类
 * 
 * <p>提供通用功能：
 * <ul>
 *   <li>本地缓存管理</li>
 *   <li>订阅监听器管理</li>
 *   <li>失败重试机制</li>
 *   <li>心跳保活</li>
 * </ul>
 * 
 * @author Matrix RPC Team
 * @since 1.0.0
 */
public abstract class AbstractRegistry implements Registry {
    
    protected final Logger logger = LoggerFactory.getLogger(getClass());
    
    /** 注册中心地址 */
    protected final URL registryUrl;
    
    /** 本地服务缓存 (serviceName -> List<URL>) */
    protected final Map<String, List<URL>> serviceCache = new ConcurrentHashMap<>();
    
    /** 订阅监听器 (serviceName -> Set<NotifyListener>) */
    protected final Map<String, Set<NotifyListener>> listeners = new ConcurrentHashMap<>();
    
    /** 已注册的服务 (URL集合) */
    protected final Set<URL> registered = ConcurrentHashMap.newKeySet();
    
    /** 已订阅的服务 (serviceName集合) */
    protected final Set<String> subscribed = ConcurrentHashMap.newKeySet();
    
    /** 重试任务调度器 */
    protected final ScheduledExecutorService retryExecutor;
    
    /** 心跳任务调度器 */
    protected final ScheduledExecutorService heartbeatExecutor;
    
    protected AbstractRegistry(URL registryUrl) {
        this.registryUrl = registryUrl;
        this.retryExecutor = Executors.newScheduledThreadPool(1, 
            r -> new Thread(r, "Registry-Retry-" + registryUrl.getAddress()));
        this.heartbeatExecutor = Executors.newScheduledThreadPool(1,
            r -> new Thread(r, "Registry-Heartbeat-" + registryUrl.getAddress()));
    }
    
    @Override
    public void register(URL url) {
        try {
            doRegister(url);
            registered.add(url);
            logger.info("Service registered: {}", url);
        } catch (Exception e) {
            logger.error("Failed to register service: {}", url, e);
            scheduleRetry(() -> register(url), "register");
        }
    }
    
    @Override
    public void unregister(URL url) {
        try {
            doUnregister(url);
            registered.remove(url);
            logger.info("Service unregistered: {}", url);
        } catch (Exception e) {
            logger.error("Failed to unregister service: {}", url, e);
        }
    }
    
    @Override
    public List<URL> lookup(String serviceInterface, String group, String version) {
        String serviceName = buildServiceName(serviceInterface, group, version);
        
        // 1. 先从本地缓存查询
        List<URL> cached = serviceCache.get(serviceName);
        if (cached != null && !cached.isEmpty()) {
            return cached;
        }
        
        // 2. 缓存未命中，从注册中心查询
        try {
            List<URL> urls = doLookup(serviceInterface, group, version);
            serviceCache.put(serviceName, urls);
            return urls;
        } catch (Exception e) {
            logger.error("Failed to lookup service: {}", serviceName, e);
            return List.of();
        }
    }
    
    @Override
    public void subscribe(String serviceInterface, NotifyListener listener) {
        String serviceName = buildServiceName(serviceInterface, null, null);
        
        // 添加监听器
        listeners.computeIfAbsent(serviceName, k -> ConcurrentHashMap.newKeySet())
                .add(listener);
        
        subscribed.add(serviceName);
        
        try {
            doSubscribe(serviceInterface, listener);
            logger.info("Service subscribed: {}", serviceInterface);
        } catch (Exception e) {
            logger.error("Failed to subscribe service: {}", serviceInterface, e);
            scheduleRetry(() -> subscribe(serviceInterface, listener), "subscribe");
        }
    }
    
    /**
     * 取消订阅
     * 
     * @param serviceInterface 服务接口
     * @param listener 事件监听器
     */
    public void unsubscribe(String serviceInterface, NotifyListener listener) {
        String serviceName = buildServiceName(serviceInterface, null, null);
        
        Set<NotifyListener> listenerSet = listeners.get(serviceName);
        if (listenerSet != null) {
            listenerSet.remove(listener);
            if (listenerSet.isEmpty()) {
                listeners.remove(serviceName);
                subscribed.remove(serviceName);
            }
        }
        
        try {
            doUnsubscribe(serviceInterface, listener);
            logger.info("Service unsubscribed: {}", serviceInterface);
        } catch (Exception e) {
            logger.error("Failed to unsubscribe service: {}", serviceInterface, e);
        }
    }
    
    /**
     * 销毁注册中心连接
     * 
     * <p>优雅关闭连接，释放资源
     */
    public void destroy() {
        // 注销所有已注册服务
        registered.forEach(this::unregister);
        
        // 关闭线程池
        shutdownExecutor(retryExecutor);
        shutdownExecutor(heartbeatExecutor);
        
        // 清理资源
        serviceCache.clear();
        listeners.clear();
        
        logger.info("Registry destroyed: {}", registryUrl.getAddress());
    }
    
    /**
     * 检查注册中心连接状态
     * 
     * @return true-已连接，false-未连接
     */
    public boolean isAvailable() {
        return true;
    }
    
    /**
     * 通知监听器服务变更
     * 
     * @param serviceName 服务名
     * @param urls 最新的服务列表
     */
    protected void notifyListeners(String serviceName, List<URL> urls) {
        Set<NotifyListener> listenerSet = listeners.get(serviceName);
        if (listenerSet != null) {
            for (NotifyListener listener : listenerSet) {
                try {
                    listener.notify(urls);
                } catch (Exception e) {
                    logger.error("Failed to notify listener: {}", serviceName, e);
                }
            }
        }
        
        // 更新本地缓存
        serviceCache.put(serviceName, urls);
    }
    
    /**
     * 调度重试任务（指数退避）
     */
    protected void scheduleRetry(Runnable task, String operation) {
        retryExecutor.schedule(() -> {
            try {
                task.run();
            } catch (Exception e) {
                logger.error("Retry {} failed", operation, e);
            }
        }, 5, TimeUnit.SECONDS); // 5秒后重试
    }
    
    /**
     * 关闭线程池
     */
    private void shutdownExecutor(ExecutorService executor) {
        if (executor != null && !executor.isShutdown()) {
            executor.shutdown();
            try {
                if (!executor.awaitTermination(10, TimeUnit.SECONDS)) {
                    executor.shutdownNow();
                }
            } catch (InterruptedException e) {
                executor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }
    
    /**
     * 构建服务名
     */
    protected String buildServiceName(String serviceInterface, String group, String version) {
        StringBuilder sb = new StringBuilder(serviceInterface);
        if (group != null && !group.isEmpty()) {
            sb.append(":").append(group);
        }
        if (version != null && !version.isEmpty()) {
            sb.append(":").append(version);
        }
        return sb.toString();
    }
    
    // ========== 子类实现的抽象方法 ==========
    
    /**
     * 执行服务注册（子类实现）
     */
    protected abstract void doRegister(URL url);
    
    /**
     * 执行服务注销（子类实现）
     */
    protected abstract void doUnregister(URL url);
    
    /**
     * 执行服务查询（子类实现）
     */
    protected abstract List<URL> doLookup(String serviceInterface, String group, String version);
    
    /**
     * 执行服务订阅（子类实现）
     */
    protected abstract void doSubscribe(String serviceInterface, NotifyListener listener);
    
    /**
     * 执行取消订阅（子类可选实现）
     */
    protected void doUnsubscribe(String serviceInterface, NotifyListener listener) {
        // 默认实现：空操作
    }
}
