package io.homeey.matrix.rpc.registry.api;


import io.homeey.matrix.rpc.core.URL;
import io.homeey.matrix.rpc.spi.SPI;

/**
 * 注册中心工厂接口
 *
 * @author jt4mrg@gmail.com
 * @since 2026/01/03
 */
@SPI("nacos")
public interface RegistryFactory {
    /**
     * 根据URL获取注册中心实例
     *
     * @param url 注册中心地址
     * @return 注册中心实例
     */
    Registry getRegistry(URL url);
}