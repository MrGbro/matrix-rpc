package io.homeey.matrix.rpc.proxy.jdk;

import io.homeey.matrix.rpc.core.Invoker;
import io.homeey.matrix.rpc.proxy.api.ProxyFactory;
import io.homeey.matrix.rpc.spi.Activate;

import java.lang.reflect.Proxy;

/**
 * 基于 JDK 动态代理的 ProxyFactory 实现
 * <p>
 * 特点：
 * - 零外部依赖，使用 JDK 原生 Proxy
 * - 仅支持接口代理（RPC 场景下足够）
 * - 性能优秀，适合大多数场景
 * </p>
 *
 * @author jt4mrg@gmail.com
 * @since 2026-01-08
 */
@Activate
public class JdkProxyFactory implements ProxyFactory {

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getProxy(Invoker<T> invoker) {
        Class<T> interfaceClass = invoker.getInterface();
        return getProxy(invoker, new Class<?>[]{interfaceClass});
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getProxy(Invoker<T> invoker, Class<?>[] interfaces) {
        ClassLoader classLoader = getClassLoader(invoker.getInterface());

        return (T) Proxy.newProxyInstance(
                classLoader,
                interfaces,
                new InvokerInvocationHandler(invoker)
        );
    }

    /**
     * 获取类加载器
     * <p>
     * 优先使用接口的类加载器，fallback 到线程上下文类加载器
     * </p>
     */
    private ClassLoader getClassLoader(Class<?> interfaceClass) {
        ClassLoader classLoader = interfaceClass.getClassLoader();
        if (classLoader == null) {
            classLoader = Thread.currentThread().getContextClassLoader();
        }
        if (classLoader == null) {
            classLoader = JdkProxyFactory.class.getClassLoader();
        }
        return classLoader;
    }
}
