package io.homeey.nexa.core.rpc;

import io.homeey.nexa.common.URL;

/**
 *
 * @author jt4mrg@gmail.com
 * @since 2025-12-24 22:44
 **/
public interface Invoker<T> {

    /**
     * 获取接口类型
     *
     * @return 接口类型
     */
    Class<T> getInterface();

    /**
     * 执行远程调用
     *
     * @param invocation 调用信息
     * @return 调用结果
     */
    Result invoke(Invocation invocation);

    /**
     * 获取调用者的URL
     *
     * @return URL
     */
    URL getURL();

    /**
     * 检查是否可用
     *
     * @return 是否可用
     */
    boolean isAvailable();

    /**
     * 销毁调用者
     */
    void destroy();
}
