package io.homeey.matrix.rpc.registry.api;


import io.homeey.matrix.rpc.core.URL;

import java.util.List;

public interface NotifyListener {

    void notify(List<URL> urls);
}
