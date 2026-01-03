package io.homeey.matrix.rpc.core.bootstrap;

public class ReferenceConfig<T> {

    private final Class<T> interfaceClass;
    private final String serviceName;

    public ReferenceConfig(Class<T> interfaceClass) {
        this.interfaceClass = interfaceClass;
        this.serviceName = interfaceClass.getName();
    }

    public Class<T> getInterfaceClass() {
        return interfaceClass;
    }

    public String getServiceName() {
        return serviceName;
    }
}
