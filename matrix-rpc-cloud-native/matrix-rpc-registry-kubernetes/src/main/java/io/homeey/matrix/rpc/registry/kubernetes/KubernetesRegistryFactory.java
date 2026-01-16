package io.homeey.matrix.rpc.registry.kubernetes;

import io.homeey.matrix.rpc.core.URL;
import io.homeey.matrix.rpc.registry.api.Registry;
import io.homeey.matrix.rpc.registry.api.RegistryFactory;

/**
 * Kubernetes Registry 工厂
 *
 * @author Matrix RPC Team
 */
public class KubernetesRegistryFactory implements RegistryFactory {

    @Override
    public Registry getRegistry(URL url) {
        return new KubernetesRegistry(url);
    }
}
