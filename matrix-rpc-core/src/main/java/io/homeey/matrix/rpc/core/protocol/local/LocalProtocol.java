package io.homeey.matrix.rpc.core.protocol.local;

import io.homeey.matrix.rpc.common.URL;
import io.homeey.matrix.rpc.core.invoker.Invoker;
import io.homeey.matrix.rpc.core.protocol.Exporter;
import io.homeey.matrix.rpc.core.protocol.Protocol;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class LocalProtocol implements Protocol {

    private final Map<String, Exporter<?>> exporterMap = new ConcurrentHashMap<>();

    @Override
    public <T> Exporter<T> export(Invoker<T> invoker) {
        String serviceKey = invoker.getInterface().getName();
        LocalExporter<T> exporter = new LocalExporter<>(invoker);
        exporterMap.put(serviceKey, exporter);
        return exporter;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> Invoker<T> refer(Class<T> type, URL url) {
        Exporter<T> exporter =
                (Exporter<T>) exporterMap.get(type.getName());
        if (exporter == null) {
            throw new IllegalStateException("No such service: " + type.getName());
        }
        return new LocalInvoker<>(exporter.getInvoker());
    }
}
