package io.homeey.matrix.rpc.cluster.router;

import io.homeey.matrix.rpc.cluster.api.ConfigCenter;
import io.homeey.matrix.rpc.cluster.api.RouteRule;
import io.homeey.matrix.rpc.cluster.api.Router;
import io.homeey.matrix.rpc.core.Invocation;
import io.homeey.matrix.rpc.core.URL;
import io.homeey.matrix.rpc.spi.Activate;
import io.homeey.matrix.rpc.spi.ExtensionLoader;

import java.util.List;

/**
 * 动态路由器
 *
 * <p>从配置中心动态加载路由规则，支持规则热更新。
 *
 * <p>架构设计：
 * <ul>
 *   <li>初始化时从配置中心加载规则</li>
 *   <li>监听配置中心的规则变化</li>
 *   <li>规则变化时自动更新内部 ConditionRouter</li>
 * </ul>
 *
 * <p>使用示例：
 * <pre>
 * // 在系统启动时配置配置中心地址
 * System.setProperty("matrix.config.center", "memory://localhost");
 *
 * // DynamicRouter 会自动从配置中心加载规则
 * // 当配置中心规则变化时，会自动更新
 * </pre>
 *
 * @author jt4mrg@gmail.com
 * @since 2026-01-11
 */
@Activate(order = 15)
public class DynamicRouter implements Router {

    private final ConditionRouter conditionRouter = new ConditionRouter();
    private ConfigCenter configCenter;
    private boolean enabled = true;
    private String currentServiceName;

    public DynamicRouter() {
        // 初始化配置中心
        initConfigCenter();
    }

    /**
     * 初始化配置中心连接
     */
    private void initConfigCenter() {
        try {
            // 从系统属性获取配置中心地址
            String configCenterAddress = System.getProperty("matrix.config.center", "memory://localhost");
            String[] parts = configCenterAddress.split("://");
            String protocol = parts.length > 0 ? parts[0] : "memory";

            // 通过 SPI 加载配置中心
            configCenter = ExtensionLoader.getExtensionLoader(ConfigCenter.class)
                    .getExtension(protocol);
            configCenter.init(configCenterAddress);

            System.out.println("[DynamicRouter] Initialized with config center: " + configCenterAddress);
        } catch (Exception e) {
            System.err.println("[DynamicRouter] Failed to initialize config center: " + e.getMessage());
            configCenter = null;
        }
    }

    /**
     * 为指定服务订阅路由规则
     */
    public void subscribeRules(String serviceName) {
        if (configCenter == null || serviceName == null) {
            return;
        }

        this.currentServiceName = serviceName;

        // 首次加载规则
        loadRules(serviceName);

        // 添加监听器，规则变化时自动更新
        configCenter.addListener(serviceName, rules -> {
            System.out.println("[DynamicRouter] Rules updated for service: " + serviceName + ", count: " + rules.size());
            conditionRouter.setRules(rules);
        });
    }

    /**
     * 从配置中心加载规则
     */
    private void loadRules(String serviceName) {
        try {
            List<RouteRule> rules = configCenter.getRouteRules(serviceName);
            if (rules != null && !rules.isEmpty()) {
                conditionRouter.setRules(rules);
                System.out.println("[DynamicRouter] Loaded " + rules.size() + " rules for service: " + serviceName);
            }
        } catch (Exception e) {
            System.err.println("[DynamicRouter] Failed to load rules: " + e.getMessage());
        }
    }

    @Override
    public int getPriority() {
        return 15;
    }

    @Override
    public List<URL> route(List<URL> providers, Invocation invocation) {
        if (!enabled || configCenter == null) {
            return providers;
        }

        // 如果还没有订阅当前服务的规则，先订阅
        if (currentServiceName == null) {
            String serviceName = invocation.getServiceName();
            subscribeRules(serviceName);
        }

        // 委托给 ConditionRouter 执行路由
        return conditionRouter.route(providers, invocation);
    }

    @Override
    public boolean isEnabled() {
        return enabled && configCenter != null && conditionRouter.getRuleCount() > 0;
    }

    /**
     * 设置是否启用
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * 获取当前规则数量
     */
    public int getRuleCount() {
        return conditionRouter.getRuleCount();
    }

    /**
     * 获取配置中心实例（用于测试）
     */
    public ConfigCenter getConfigCenter() {
        return configCenter;
    }
}
