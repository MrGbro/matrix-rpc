package io.homeey.matrix.rpc.core.proxy;


import io.homeey.matrix.rpc.core.Invoker;

import java.lang.reflect.Proxy;

public class JdkProxyFactory implements ProxyFactory {

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getProxy(Invoker<T> invoker) {
        return (T) Proxy.newProxyInstance(
                invoker.getInterface().getClassLoader(),
                new Class<?>[]{invoker.getInterface()},
                new InvocationHandlerAdapter(invoker)
        );
    }

    @Override
    public <T> Invoker<T> getInvoker(T proxy, Class<T> type) {
        return new ProxyInvoker<>(proxy, type);
    }
}
