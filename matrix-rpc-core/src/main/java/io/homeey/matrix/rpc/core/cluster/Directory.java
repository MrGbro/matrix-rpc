package io.homeey.matrix.rpc.core.cluster;


import io.homeey.matrix.rpc.core.Invocation;
import io.homeey.matrix.rpc.core.Invoker;

import java.util.List;

/**
 * 一组可用的Invoker
 *
 * @author jt4mrg@gmail.com
 * @since 2026-01-02
 **/
public interface Directory<T> {

    /**
     * 获取服务接口类型
     *
     * @return 服务接口的Class对象
     */
    Class<T> getInterface();

    /**
     * 获取可用的Invoker列表
     *
     * @return Invoker列表
     */
    List<Invoker<T>> list(Invocation invocation);
}
