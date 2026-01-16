package io.homeey.matrix.rpc.config;

import io.homeey.matrix.rpc.spi.SPI;

import java.util.Map;

/**
 * Configuration Center Interface - 配置中心接口
 * <p>
 * 支持配置热更新、版本管理、灰度发布
 * 
 * <h3>⚡ 核心能力</h3>
 * <ul>
 *   <li><b>配置热更新</b>: 基于 Informer 机制实时监听配置变更</li>
 *   <li><b>版本管理</b>: ConfigSnapshot 支持配置快照和回滚</li>
 *   <li><b>灰度发布</b>: PublishStrategy 支持按百分比/IP/环境灰度</li>
 *   <li><b>变更通知</b>: ConfigChangeListener 异步通知增量变更</li>
 * </ul>
 * 
 * <h3>📦 SPI 扩展</h3>
 * <p>
 * 支持多种配置中心实现：
 * <ul>
 *   <li>KubernetesConfigCenter: 基于 ConfigMap</li>
 *   <li>EtcdConfigCenter: 基于 etcd</li>
 *   <li>NacosConfigCenter: 基于 Nacos</li>
 * </ul>
 */
@SPI("kubernetes")
public interface ConfigCenter {

    /**
     * 获取配置项
     * 
     * @param key 配置键（支持命名空间，如 "matrix.rpc.timeout"）
     * @return 配置值，不存在返回 null
     */
    String getConfig(String key);

    /**
     * 获取所有配置
     * 
     * @return 配置项 Map
     */
    Map<String, String> getAllConfig();

    /**
     * 发布配置变更
     * 
     * @param key 配置键
     * @param value 配置值
     * @param version 配置版本（用于回滚）
     */
    void publishConfig(String key, String value, String version);

    /**
     * 发布配置变更（支持发布策略）
     * 
     * @param key 配置键
     * @param value 配置值
     * @param version 配置版本
     * @param strategy 发布策略（全量/灰度/定时）
     */
    default void publishConfig(String key, String value, String version, PublishStrategy strategy) {
        // 默认实现：忽略策略，直接全量发布
        publishConfig(key, value, version);
    }

    /**
     * 删除配置
     * 
     * @param key 配置键
     */
    void removeConfig(String key);

    /**
     * 添加配置变更监听器
     * 
     * @param listener 监听器
     */
    void addListener(ConfigChangeListener listener);

    /**
     * 移除配置变更监听器
     * 
     * @param listener 监听器
     */
    void removeListener(ConfigChangeListener listener);

    /**
     * 获取配置快照（用于回滚）
     * 
     * @param version 版本号
     * @return 配置快照
     */
    ConfigSnapshot getSnapshot(String version);

    /**
     * 保存配置快照
     * 
     * @param version 版本号
     * @param snapshot 配置快照
     */
    void saveSnapshot(String version, ConfigSnapshot snapshot);

    /**
     * 回滚到指定版本
     * 
     * @param version 目标版本号
     * @return 是否回滚成功
     */
    boolean rollback(String version);

    /**
     * 启动配置中心（开始监听变更）
     */
    void start();

    /**
     * 关闭配置中心
     */
    void shutdown();
}
