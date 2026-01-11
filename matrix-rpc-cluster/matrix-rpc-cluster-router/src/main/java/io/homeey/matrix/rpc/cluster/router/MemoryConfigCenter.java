package io.homeey.matrix.rpc.cluster.router;

import io.homeey.matrix.rpc.cluster.api.ConfigCenter;
import io.homeey.matrix.rpc.cluster.api.RouteRule;
import io.homeey.matrix.rpc.spi.Activate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    private static final Logger logger = LoggerFactory.getLogger(MemoryConfigCenter.class);

    // 存储每个服务的路由规则
    private final ConcurrentHashMap<String, List<RouteRule>> ruleStore = new ConcurrentHashMap<>();

    // 存储每个服务的监听器
    private final ConcurrentHashMap<String, List<Consumer<List<RouteRule>>>> listeners = new ConcurrentHashMap<>();

    @Override
    public void init(String address) {
        logger.info("MemoryConfigCenter initialized with address: {}", address);
    }

    @Override
    public List<RouteRule> getRouteRules(String serviceName) {
        List<RouteRule> rules = ruleStore.get(serviceName);
        return rules != null ? new ArrayList<>(rules) : new ArrayList<>();
    }

    @Override
    public void addListener(String serviceName, Consumer<List<RouteRule>> listener) {
        listeners.computeIfAbsent(serviceName, k -> new CopyOnWriteArrayList<>()).add(listener);
        logger.debug("Added listener for service: {}", serviceName);
    }

    @Override
    public void removeListener(String serviceName) {
        listeners.remove(serviceName);
        logger.debug("Removed listeners for service: {}", serviceName);
    }

    @Override
    public void publishRouteRules(String serviceName, List<RouteRule> rules) {
        // 更新规则
        ruleStore.put(serviceName, new ArrayList<>(rules));
        logger.info("Published {} rules for service: {}", rules.size(), serviceName);

        // 通知所有监听器
        List<Consumer<List<RouteRule>>> serviceListeners = listeners.get(serviceName);
        if (serviceListeners != null) {
            for (Consumer<List<RouteRule>> listener : serviceListeners) {
                try {
                    listener.accept(new ArrayList<>(rules));
                } catch (Exception e) {
                    logger.warn("Failed to notify listener for service: {}", serviceName, e);
                }
            }
        }
    }

    @Override
    public void close() {
        ruleStore.clear();
        listeners.clear();
        logger.info("MemoryConfigCenter closed");
    }
}
