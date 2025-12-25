package io.homeey.nexa.core.protocol;

import io.homeey.nexa.common.URL;
import io.homeey.nexa.core.rpc.Invoker;

/**
 *
 * @author jt4mrg@gmail.com
 * @since 2025-12-24 23:29
 **/
public interface Protocol {
    /**
     * 获取默认端口
     * @return 默认端口
     */
    int getDefaultPort();

    /**
     * 获取 Exporter
     * @param <T> 泛型类型
     * @return Exporter 实例
     */
    <T> Exporter<T> getExporter();

    /**
     * 创建一个 Invoker
     * @param type 服务接口类型
     * @param url 服务地址
     * @param <T> 泛型类型
     * @return Invoker 实例
     */
    <T> Invoker<T> refer(Class<T> type, URL url);

    /**
     * 销毁协议
     */
    void destroy();
}
