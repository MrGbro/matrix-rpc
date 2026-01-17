package io.homeey.matrix.rpc.registry.consul;

import io.homeey.matrix.rpc.core.URL;
import io.homeey.matrix.rpc.registry.api.Registry;
import io.homeey.matrix.rpc.registry.api.RegistryFactory;
import io.homeey.matrix.rpc.spi.Activate;

/**
 * Consul 注册中心工厂
 * 
 * @author Matrix RPC Team
 * @since 1.0.0
 */
@Activate(order = 150)
public class ConsulRegistryFactory implements RegistryFactory {
    
    @Override
    public Registry getRegistry(URL url) {
        return new ConsulRegistry(url);
    }
}
