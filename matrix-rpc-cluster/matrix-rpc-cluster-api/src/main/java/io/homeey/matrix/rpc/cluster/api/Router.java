package io.homeey.matrix.rpc.cluster.api;

import io.homeey.matrix.rpc.core.Invocation;
import io.homeey.matrix.rpc.core.URL;
import io.homeey.matrix.rpc.spi.SPI;

import java.util.List;

/**
 * 路由接口：负责根据规则过滤服务提供者
 *
 * <p>与 Dubbo 的区别：
 * <ul>
 *   <li>更简单：只关注过滤逻辑，不涉及规则管理</li>
 *   <li>职责单一：不依赖配置中心，规则由外部传入</li>
 * </ul>
 *
 * <p>使用场景：
 * <ul>
 *   <li>标签路由：根据服务标签过滤实例（灰度发布、蓝绿部署）</li>
 *   <li>条件路由：根据请求参数过滤实例（A/B 测试、流量隔离）</li>
 *   <li>自定义路由：根据业务规则过滤实例</li>
 * </ul>
 *
 * @author jt4mrg@gmail.com
 * @since 2026-01-10
 */
@SPI("tag")
public interface Router {

    /**
     * 路由优先级（数字越小优先级越高）
     * <ul>
     *   <li>标签路由：10</li>
     *   <li>条件路由：20</li>
     *   <li>自定义路由：30+</li>
     * </ul>
     *
     * @return 优先级数值
     */
    int getPriority();

    /**
     * 根据路由规则过滤服务提供者
     *
     * @param providers  原始提供者列表
     * @param invocation 当前调用上下文（包含标签等信息）
     * @return 过滤后的提供者列表（不能为 null，可以为空列表）
     */
    List<URL> route(List<URL> providers, Invocation invocation);

    /**
     * 是否启用该路由规则
     *
     * @return true 启用，false 禁用
     */
    default boolean isEnabled() {
        return true;
    }
}
