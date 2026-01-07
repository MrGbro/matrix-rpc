package io.homeey.matrix.rpc.proxy.api;

import io.homeey.matrix.rpc.core.Invoker;
import io.homeey.matrix.rpc.spi.SPI;

/**
 * 代理工厂接口 - 将 Invoker 转换为业务接口代理对象
 * <p>
 * 使用场景：消费端通过代理对象调用远程服务，
 * 代理对象内部将方法调用转换为 Invocation 并委托给 Invoker 执行
 * </p>
 *
 * @author jt4mrg@gmail.com
 * @since 2026-01-08
 */
@SPI("jdk")
public interface ProxyFactory {

    /**
     * 创建代理对象
     * <p>
     * 将 Invoker 包装为指定接口的代理实例，用户可像调用本地方法一样调用远程服务
     * </p>
     *
     * @param invoker 服务调用器，封装了远程调用逻辑
     * @param <T>     服务接口类型
     * @return 代理对象，实现了服务接口
     */
    <T> T getProxy(Invoker<T> invoker);

    /**
     * 创建代理对象（支持多接口）
     * <p>
     * 支持代理对象同时实现多个接口（如服务接口 + 泛化接口）
     * </p>
     *
     * @param invoker    服务调用器
     * @param interfaces 需要实现的接口列表
     * @param <T>        主服务接口类型
     * @return 代理对象
     */
    <T> T getProxy(Invoker<T> invoker, Class<?>[] interfaces);
}
