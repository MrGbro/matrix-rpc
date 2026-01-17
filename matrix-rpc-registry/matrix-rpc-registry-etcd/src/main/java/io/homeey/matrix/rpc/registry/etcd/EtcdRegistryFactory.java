package io.homeey.matrix.rpc.registry.etcd;

import io.homeey.matrix.rpc.core.URL;
import io.homeey.matrix.rpc.registry.api.Registry;
import io.homeey.matrix.rpc.registry.api.RegistryFactory;
import io.homeey.matrix.rpc.spi.Activate;

/**
 * Etcd 注册中心工厂实现
 * 
 * @author Matrix RPC Team
 * @since 1.0.0
 */
@Activate(order = 50)
public class EtcdRegistryFactory implements RegistryFactory {
    
    @Override
    public Registry getRegistry(URL url) {
        return new EtcdRegistry(url);
    }
}
