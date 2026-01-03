package io.homeey.matrix.rpc.core.cluster;


import io.homeey.matrix.rpc.core.Invoker;

/**
 * 容错策略
 *
 * @author jt4mrg@gmail.com
 * @since 2026-01-02
 **/
public interface Cluster {
    /**
     * 加入集群
     *
     * @param directory 目录
     * @param <T>       服务接口类型
     * @return Invoker
     */
    <T> Invoker<T> join(Directory<T> directory);
}
