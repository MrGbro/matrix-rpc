package io.homeey.nexa.core.proxy;

import io.homeey.nexa.common.URL;
import io.homeey.nexa.core.rpc.Invoker;

/**
 *
 * @author jt4mrg@gmail.com
 * @since 2025-12-25 23:56
 **/
public interface ProxyFactory {
    /**
     * 根据 Invoker 创建代理对象
     *
     * @param invoker 远程调用器
     * @param <T>     代理对象类型
     * @return 代理对象
     */
    <T> T getProxy(Invoker<T> invoker);

    /**
     * 根据代理对象创建 Invoker
     *
     * @param proxy 代理对象
     * @param type  代理对象类型
     * @param url   远程调用地址
     * @param <T>   代理对象类型
     * @return 远程调用器
     */
    <T> Invoker<T> getInvoker(T proxy, Class<T> type, URL url);
}
