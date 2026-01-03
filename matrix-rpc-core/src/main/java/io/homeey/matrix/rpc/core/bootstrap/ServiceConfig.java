package io.homeey.matrix.rpc.core.bootstrap;

public class ServiceConfig<T> {

    private final Class<T> interfaceClass;
    private final T ref;
    private final int port;
    private final String serviceName;

    public ServiceConfig(Class<T> interfaceClass, T ref, int port) {
        this.interfaceClass = interfaceClass;
        this.ref = ref;
        this.port = port;
        this.serviceName = interfaceClass.getName();
    }

    public Class<T> getInterfaceClass() {
        return interfaceClass;
    }

    public T getRef() {
        return ref;
    }

    public int getPort() {
        return port;
    }

    public String getServiceName() {
        return serviceName;
    }
}
