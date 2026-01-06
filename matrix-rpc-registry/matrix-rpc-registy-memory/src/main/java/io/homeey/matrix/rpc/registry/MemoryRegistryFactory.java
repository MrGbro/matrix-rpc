package io.homeey.matrix.rpc.registry;

import io.homeey.matrix.rpc.common.URL;
import io.homeey.matrix.rpc.registry.api.Registry;
import io.homeey.matrix.rpc.registry.api.RegistryFactory;

/**
 *
 * @author jt4mrg@gmail.com
 * @since 2026-01-06
 **/
public class MemoryRegistryFactory implements RegistryFactory {
    @Override
    public Registry getRegistry(URL url) {
        return null;
    }
}
