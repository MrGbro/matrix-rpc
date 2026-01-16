package io.homeey.matrix.rpc.config;

/**
 * Configuration Change Listener - 配置变更监听器
 */
@FunctionalInterface
public interface ConfigChangeListener {

    /**
     * 配置变更回调
     * 
     * @param event 配置变更事件
     */
    void onConfigChange(ConfigChangeEvent event);
}
