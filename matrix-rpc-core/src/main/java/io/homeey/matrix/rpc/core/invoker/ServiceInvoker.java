package io.homeey.matrix.rpc.core.invoker;

import io.homeey.matrix.rpc.core.ExecuteResult;
import io.homeey.matrix.rpc.core.invocation.Invocation;
import io.homeey.matrix.rpc.core.invocation.Result;

import java.lang.reflect.Method;

/**
 *
 * @author jt4mrg@gmail.com
 * @since 2026-01-02
 **/
public class ServiceInvoker<T> extends AbstractInvoker<T> {
    private final T service;

    public ServiceInvoker(Class<T> type, T service) {
        super(type);
        this.service = service;
    }

    @Override
    public Result invoke(Invocation invocation) {
        try {
            Method method = service.getClass()
                    .getMethod(invocation.methodName(),
                            invocation.parameterTypes());

            Object value =
                    method.invoke(service, invocation.arguments());

            return ExecuteResult.success(value);
        } catch (Throwable t) {
            return ExecuteResult.fail(t);
        }
    }
}
