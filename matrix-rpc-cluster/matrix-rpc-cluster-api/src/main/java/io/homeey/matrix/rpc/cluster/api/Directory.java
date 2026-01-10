package io.homeey.matrix.rpc.cluster.api;

import io.homeey.matrix.rpc.core.Invocation;
import io.homeey.matrix.rpc.core.URL;
import io.homeey.matrix.rpc.spi.ExtensionLoader;

import java.util.Comparator;
import java.util.List;

/**
 * 服务目录：提供服务提供者列表
 *
 * <p>Directory 是 Cluster 和服务提供者列表之间的桥梁，
 * 它封装了获取服务提供者的逻辑，使得 Cluster 可以专注于容错策略。
 *
 * <p>增强功能：支持路由过滤
 * <ul>
 *   <li>list() - 获取原始提供者列表（未经路由过滤）</li>
 *   <li>list(Invocation) - 获取经过路由过滤的提供者列表</li>
 * </ul>
 *
 * @param <T> 服务接口类型
 * @author jt4mrg@gmail.com
 * @since 2026-01-10
 */
public interface Directory<T> {

    /**
     * 获取服务接口类型
     *
     * @return 服务接口 Class
     */
    Class<T> getInterface();

    /**
     * 获取原始提供者列表（未经路由过滤）
     *
     * @return 服务提供者 URL 列表
     */
    List<URL> list();

    /**
     * 根据调用上下文获取路由后的提供者列表
     *
     * <p>路由过程：
     * <ol>
     *   <li>获取原始提供者列表</li>
     *   <li>加载所有 Router 实现（按优先级排序）</li>
     *   <li>依次应用每个路由规则进行过滤</li>
     *   <li>返回最终过滤后的列表</li>
     * </ol>
     *
     * @param invocation 调用上下文（包含标签等信息）
     * @return 经过路由过滤的提供者列表
     */
    default List<URL> list(Invocation invocation) {
        List<URL> providers = list();

        if (providers == null || providers.isEmpty()) {
            return providers;
        }

        // 加载所有路由器并按优先级排序
        ExtensionLoader<Router> loader = ExtensionLoader.getExtensionLoader(Router.class);
        List<Router> routers;
        try {
            // 获取所有启用的 Router 实现
            routers = loader.getActivateExtensions("CONSUMER");
        } catch (Exception e) {
            // 如果没有找到任何 Router，直接返回原始列表
            return providers;
        }

        if (routers == null || routers.isEmpty()) {
            return providers;
        }

        // 按优先级排序（数字越小优先级越高）
        routers.sort(Comparator.comparingInt(Router::getPriority));

        // 应用所有路由规则
        for (Router router : routers) {
            if (router.isEnabled()) {
                providers = router.route(providers, invocation);
                if (providers == null || providers.isEmpty()) {
                    // 某个路由器过滤后无可用实例，提前退出
                    break;
                }
            }
        }

        return providers;
    }
}
