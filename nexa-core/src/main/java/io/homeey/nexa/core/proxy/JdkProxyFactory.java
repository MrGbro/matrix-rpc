package io.homeey.nexa.core.proxy;

import io.homeey.nexa.common.URL;
import io.homeey.nexa.core.rpc.AbstractInvoker;
import io.homeey.nexa.core.rpc.Invocation;
import io.homeey.nexa.core.rpc.Invoker;
import io.homeey.nexa.core.rpc.Result;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/**
 *
 * @author jt4mrg@gmail.com
 * @since 2025-12-26 0:05
 **/
public class JdkProxyFactory implements ProxyFactory {
    @Override
    @SuppressWarnings("unchecked")
    public <T> T getProxy(Invoker<T> invoker) {
        Class<?>[] interfaces = new Class<?>[]{invoker.getInterface()};
        return (T) Proxy.newProxyInstance(invoker.getInterface().getClassLoader(),
                interfaces, new InvokerInvocationHandler(invoker));
    }

    @Override
    public <T> Invoker<T> getInvoker(T proxy, Class<T> type, URL url) {
        return new AbstractInvoker<T>(type, url) {
            @Override
            protected Result doInvoke(Invocation invocation) throws Throwable {
                Method method = proxy.getClass().getMethod(invocation.getMethodName(), invocation.getParameterTypes());
                Object value = method.invoke(proxy, invocation.getArguments());
                return new Result(value);
            }
        };
    }

    private static class InvokerInvocationHandler implements InvocationHandler {
        private final Invoker<?> invoker;

        public InvokerInvocationHandler(Invoker<?> invoker) {
            this.invoker = invoker;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            if (Object.class == method.getDeclaringClass()) {
                return method.invoke(invoker, args);
            }
            String name = method.getName();
            Class<?>[] parameterTypes = method.getParameterTypes();
            if ("toString".equals(name) && parameterTypes.length == 0) {
                return invoker.toString();
            }
            if ("hashCode".equals(name) && parameterTypes.length == 0) {
                return invoker.hashCode();
            }
            if ("equals".equals(name) && parameterTypes.length == 1) {
                return invoker.equals(args[0]);
            }
            Invocation invocation = new Invocation(invoker.getInterface().getName(), name, parameterTypes, args);
            Result invoke = invoker.invoke(invocation);
            return invoke.recreate();
        }
    }
}
