package io.homeey.matrix.rpc.cluster.api;

import io.homeey.matrix.rpc.core.URL;

import java.util.List;
import java.util.function.Supplier;

/**
 * 基于注册中心的服务目录实现
 * 
 * <p>通过 Supplier 延迟获取服务提供者列表，避免在创建时就需要完整的服务列表
 */
public class RegistryDirectory<T> implements Directory<T> {
    
    private final Class<T> interfaceClass;
    private final Supplier<List<URL>> providerSupplier;
    
    public RegistryDirectory(Class<T> interfaceClass, Supplier<List<URL>> providerSupplier) {
        this.interfaceClass = interfaceClass;
        this.providerSupplier = providerSupplier;
    }
    
    @Override
    public Class<T> getInterface() {
        return interfaceClass;
    }
    
    @Override
    public List<URL> list() {
        return providerSupplier.get();
    }
}
