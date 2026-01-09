package io.homeey.matrix.rpc.registry.nacos;


import io.homeey.matrix.rpc.core.URL;
import io.homeey.matrix.rpc.registry.api.Registry;
import io.homeey.matrix.rpc.registry.api.RegistryFactory;
import io.homeey.matrix.rpc.spi.Activate;

/**
 * nacos注册中心工厂接口
 *
 * @author jt4mrg@gmail.com
 * @since 2026/01/03
 */
@Activate
public class NacosRegistryFactory implements RegistryFactory {
    @Override
    public Registry getRegistry(URL url) {
        return new NacosRegistry(url);
    }
}