package io.homeey.matrix.rpc.registry.zookeeper;

import io.homeey.matrix.rpc.core.URL;
import io.homeey.matrix.rpc.registry.api.Registry;
import io.homeey.matrix.rpc.registry.api.RegistryFactory;
import io.homeey.matrix.rpc.spi.Activate;

/**
 * Zookeeper 注册中心工厂实现
 * 
 * @author Matrix RPC Team
 * @since 1.0.0
 */
@Activate(order = 100)
public class ZookeeperRegistryFactory implements RegistryFactory {
    
    @Override
    public Registry getRegistry(URL url) {
        return new ZookeeperRegistry(url);
    }
}
