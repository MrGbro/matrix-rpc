package io.homeey.matrix.rpc.core.invocation;


import io.homeey.matrix.rpc.common.Result;
import io.homeey.matrix.rpc.core.Invocation;
import io.homeey.matrix.rpc.core.Invoker;

import java.lang.reflect.Method;

public class ServiceInvoker<T> implements Invoker<T> {

    private final T service;
    private final Class<T> type;

    public ServiceInvoker(T service, Class<T> type) {
        this.service = service;
        this.type = type;
    }

    @Override
    public Class<T> getInterface() {
        return type;
    }

    @Override
    public Result invoke(Invocation invocation) {
        try {
            Method method = type.getMethod(
                    invocation.methodName(),
                    invocation.parameterTypes()
            );
            Object value = method.invoke(service, invocation.arguments());
            return new Result(value);
        } catch (Throwable e) {
            return new Result(e);
        }
    }
}
