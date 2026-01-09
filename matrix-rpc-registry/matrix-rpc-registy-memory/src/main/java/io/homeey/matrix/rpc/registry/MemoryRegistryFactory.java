package io.homeey.matrix.rpc.registry;

import io.homeey.matrix.rpc.core.URL;
import io.homeey.matrix.rpc.registry.api.Registry;
import io.homeey.matrix.rpc.registry.api.RegistryFactory;
import io.homeey.matrix.rpc.spi.Activate;

/**
 * 内存注册中心工厂 - 用于本地测试
 *
 * @author jt4mrg@gmail.com
 * @since 2026-01-06
 **/
@Activate
public class MemoryRegistryFactory implements RegistryFactory {
    @Override
    public Registry getRegistry(URL url) {
        return MemoryRegistry.getInstance();
    }
}
