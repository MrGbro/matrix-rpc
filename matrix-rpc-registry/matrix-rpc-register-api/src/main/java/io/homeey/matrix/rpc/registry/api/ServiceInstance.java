package io.homeey.matrix.rpc.registry.api;

import java.util.Map;

public class ServiceInstance {

    private final String serviceName;
    private final String host;
    private final int port;
    private final Map<String, String> metadata;

    public ServiceInstance(
            String serviceName,
            String host,
            int port,
            Map<String, String> metadata
    ) {
        this.serviceName = serviceName;
        this.host = host;
        this.port = port;
        this.metadata = metadata;
    }

    public String getServiceName() {
        return serviceName;
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public Map<String, String> getMetadata() {
        return metadata;
    }
}
