package io.homeey.matrix.rpc.cluster.api;

import io.homeey.matrix.rpc.spi.SPI;

import java.util.List;
import java.util.function.Consumer;

/**
 * 配置中心接口
 * 
 * <p>用于动态获取和监听路由规则配置。
 * 
 * <p>支持的配置中心：
 * <ul>
 *   <li>Memory - 内存配置（用于测试）</li>
 *   <li>Nacos - Nacos 配置中心</li>
 *   <li>Apollo - Apollo 配置中心</li>
 *   <li>ZooKeeper - ZooKeeper 配置中心</li>
 * </ul>
 * 
 * <p>使用示例：
 * <pre>
 * ConfigCenter configCenter = ExtensionLoader.getExtensionLoader(ConfigCenter.class)
 *     .getExtension("memory");
 * 
 * // 获取路由规则
 * List&lt;RouteRule&gt; rules = configCenter.getRouteRules("io.homeey.matrix.rpc.example.EchoService");
 * 
 * // 监听规则变化
 * configCenter.addListener("io.homeey.matrix.rpc.example.EchoService", newRules -> {
 *     System.out.println("Rules updated: " + newRules.size());
 * });
 * </pre>
 * 
 * @author Matrix RPC Team
 * @since 2026-01-11
 */
@SPI("memory")
public interface ConfigCenter {
    
    /**
     * 初始化配置中心
     * 
     * @param address 配置中心地址（如：nacos://127.0.0.1:8848）
     */
    void init(String address);
    
    /**
     * 获取指定服务的路由规则
     * 
     * @param serviceName 服务名称
     * @return 路由规则列表
     */
    List<RouteRule> getRouteRules(String serviceName);
    
    /**
     * 添加规则变化监听器
     * 
     * @param serviceName 服务名称
     * @param listener 监听器（接收新的规则列表）
     */
    void addListener(String serviceName, Consumer<List<RouteRule>> listener);
    
    /**
     * 移除规则变化监听器
     * 
     * @param serviceName 服务名称
     */
    void removeListener(String serviceName);
    
    /**
     * 发布路由规则（用于动态更新）
     * 
     * @param serviceName 服务名称
     * @param rules 路由规则列表
     */
    void publishRouteRules(String serviceName, List<RouteRule> rules);
    
    /**
     * 关闭配置中心连接
     */
    void close();
}
