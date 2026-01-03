package io.homeey.matrix.rpc.core;

import io.homeey.matrix.rpc.common.URL;
import io.homeey.matrix.rpc.spi.SPI;

@SPI("matrix") // 默认协议
public interface Protocol {
    /**
     * 导出远程服务
     *
     * @param invoker 服务调用器
     * @param url     服务地址
     * @return 导出的服务对象
     */
    <T> Exporter<T> export(Invoker<T> invoker, URL url);

    /**
     * 引用远程服务
     *
     * @param type 服务类型
     * @param url  服务地址
     * @return 远程服务代理
     */
    <T> Invoker<T> refer(Class<T> type, URL url);
}