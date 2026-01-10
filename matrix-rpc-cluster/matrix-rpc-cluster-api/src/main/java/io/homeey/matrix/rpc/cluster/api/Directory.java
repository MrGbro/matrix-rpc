package io.homeey.matrix.rpc.cluster.api;

import io.homeey.matrix.rpc.core.URL;

import java.util.List;

/**
 * 服务目录：提供服务提供者列表
 * 
 * <p>Directory 是 Cluster 和服务提供者列表之间的桥梁，
 * 它封装了获取服务提供者的逻辑，使得 Cluster 可以专注于容错策略。
 * 
 * @param <T> 服务接口类型
 */
public interface Directory<T> {
    
    /**
     * 获取服务接口类型
     * 
     * @return 服务接口 Class
     */
    Class<T> getInterface();
    
    /**
     * 获取当前可用的服务提供者列表
     * 
     * @return 服务提供者 URL 列表
     */
    List<URL> list();
}
