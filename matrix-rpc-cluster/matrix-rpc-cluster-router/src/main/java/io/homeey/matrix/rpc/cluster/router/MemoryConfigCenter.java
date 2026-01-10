package io.homeey.matrix.rpc.cluster.router;

import io.homeey.matrix.rpc.cluster.api.ConfigCenter;
import io.homeey.matrix.rpc.cluster.api.RouteRule;
import io.homeey.matrix.rpc.spi.Activate;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * 内存配置中心实现
 *
 * <p>用于测试和演示，支持动态添加和更新路由规则。
 *
 * <p>使用示例：
 * <pre>
 * MemoryConfigCenter configCenter = new MemoryConfigCenter();
 * configCenter.init("memory://localhost");
 *
 * // 创建路由规则
 * RouteRule rule = new RouteRule();
 * rule.setName("gray-routing");
 * rule.setPriority(10);
 * rule.setEnabled(true);
 *
 * // 发布规则
 * configCenter.publishRouteRules("io.homeey.matrix.rpc.example.EchoService",
 *     Collections.singletonList(rule));
 * </pre>
 *
 * @author jt4mrg@gmail.com
 * @since 2026-01-11
 */
@Activate
public class MemoryConfigCenter implements ConfigCenter {

    // 存储每个服务的路由规则
    private final ConcurrentHashMap<String, List<RouteRule>> ruleStore = new ConcurrentHashMap<>();

    // 存储每个服务的监听器
    private final ConcurrentHashMap<String, List<Consumer<List<RouteRule>>>> listeners = new ConcurrentHashMap<>();

    @Override
    public void init(String address) {
        System.out.println("[MemoryConfigCenter] Initialized with address: " + address);
    }

    @Override
    public List<RouteRule> getRouteRules(String serviceName) {
        List<RouteRule> rules = ruleStore.get(serviceName);
        return rules != null ? new ArrayList<>(rules) : new ArrayList<>();
    }

    @Override
    public void addListener(String serviceName, Consumer<List<RouteRule>> listener) {
        listeners.computeIfAbsent(serviceName, k -> new CopyOnWriteArrayList<>()).add(listener);
        System.out.println("[MemoryConfigCenter] Added listener for service: " + serviceName);
    }

    @Override
    public void removeListener(String serviceName) {
        listeners.remove(serviceName);
        System.out.println("[MemoryConfigCenter] Removed listeners for service: " + serviceName);
    }

    @Override
    public void publishRouteRules(String serviceName, List<RouteRule> rules) {
        // 更新规则
        ruleStore.put(serviceName, new ArrayList<>(rules));
        System.out.println("[MemoryConfigCenter] Published " + rules.size() + " rules for service: " + serviceName);

        // 通知所有监听器
        List<Consumer<List<RouteRule>>> serviceListeners = listeners.get(serviceName);
        if (serviceListeners != null) {
            for (Consumer<List<RouteRule>> listener : serviceListeners) {
                try {
                    listener.accept(new ArrayList<>(rules));
                } catch (Exception e) {
                    System.err.println("[MemoryConfigCenter] Failed to notify listener: " + e.getMessage());
                }
            }
        }
    }

    @Override
    public void close() {
        ruleStore.clear();
        listeners.clear();
        System.out.println("[MemoryConfigCenter] Closed");
    }
}
