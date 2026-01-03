package io.homeey.matrix.rpc.core.proxy;


import io.homeey.matrix.rpc.core.Invoker;
import io.homeey.matrix.rpc.spi.SPI;

@SPI
public interface ProxyFactory {

    <T> T getProxy(Invoker<T> invoker);

    <T> Invoker<T> getInvoker(T proxy, Class<T> type);
}
