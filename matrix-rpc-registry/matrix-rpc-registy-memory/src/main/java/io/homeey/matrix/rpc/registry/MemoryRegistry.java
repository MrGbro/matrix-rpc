package io.homeey.matrix.rpc.registry;


import io.homeey.matrix.rpc.common.URL;
import io.homeey.matrix.rpc.registry.api.NotifyListener;
import io.homeey.matrix.rpc.registry.api.Registry;

import java.util.List;

public class MemoryRegistry implements Registry {
    @Override
    public void register(URL url) {

    }

    @Override
    public void unregister(URL url) {

    }

    @Override
    public List<URL> lookup(String serviceInterface, String group, String version) {
        return List.of();
    }

    @Override
    public void subscribe(String serviceInterface, NotifyListener listener) {

    }
}
