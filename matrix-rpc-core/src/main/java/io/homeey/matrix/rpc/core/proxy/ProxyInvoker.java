package io.homeey.matrix.rpc.core.proxy;

import io.homeey.matrix.rpc.common.Result;
import io.homeey.matrix.rpc.core.Invocation;
import io.homeey.matrix.rpc.core.invoker.AbstractInvoker;

import java.lang.reflect.Method;

public class ProxyInvoker<T> extends AbstractInvoker<T> {

    private final T proxy;

    public ProxyInvoker(T proxy, Class<T> type) {
        super(type);
        this.proxy = proxy;
    }

    @Override
    public Result invoke(Invocation invocation) {
        try {
            Method method = proxy.getClass()
                    .getMethod(invocation.methodName(),
                            invocation.parameterTypes());

            Object value =
                    method.invoke(proxy, invocation.arguments());

            return Result.success(value);
        } catch (Throwable t) {
            return Result.fail(t);
        }
    }
}
