package io.homeey.matrix.rpc.registry.api;


import io.homeey.matrix.rpc.core.URL;

import java.util.List;

public interface Registry {
    /**
     * 服务注册
     *
     * @param url 服务提供者URL
     */
    void register(URL url);

    /**
     * 服务注销
     *
     * @param url 服务提供者URL
     */
    void unregister(URL url);

    /**
     * 服务发现
     *
     * @param serviceInterface 服务接口
     * @param group            服务分组
     * @param version          服务版本
     * @return 可用服务提供者列表
     */
    List<URL> lookup(String serviceInterface, String group, String version);

    /**
     * 订阅服务变化
     *
     * @param serviceInterface 服务接口
     * @param listener         事件监听器
     */
    void subscribe(String serviceInterface, NotifyListener listener);
}