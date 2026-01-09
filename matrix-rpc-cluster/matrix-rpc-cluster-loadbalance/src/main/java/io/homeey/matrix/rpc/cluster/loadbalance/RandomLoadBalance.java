package io.homeey.matrix.rpc.cluster.loadbalance;

import io.homeey.matrix.rpc.cluster.api.LoadBalance;
import io.homeey.matrix.rpc.core.URL;
import io.homeey.matrix.rpc.core.Invocation;
import io.homeey.matrix.rpc.spi.Activate;

import java.util.List;
import java.util.Random;

@Activate
public class RandomLoadBalance implements LoadBalance {
    private final Random random = new Random();

    @Override
    public URL select(List<URL> providers, Invocation invocation) {
        if (providers == null || providers.isEmpty()) {
            throw new IllegalStateException("No provider available");
        }
        if (providers.size() == 1) {
            return providers.get(0);
        }
        return providers.get(random.nextInt(providers.size()));
    }
}