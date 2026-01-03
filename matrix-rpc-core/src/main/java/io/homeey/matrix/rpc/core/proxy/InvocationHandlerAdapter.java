package io.homeey.matrix.rpc.core.proxy;


import io.homeey.matrix.rpc.common.Result;
import io.homeey.matrix.rpc.core.Invocation;
import io.homeey.matrix.rpc.core.Invoker;
import io.homeey.matrix.rpc.core.invocation.DefaultInvocation;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

public class InvocationHandlerAdapter implements InvocationHandler {

    private final Invoker<?> invoker;

    public InvocationHandlerAdapter(Invoker<?> invoker) {
        this.invoker = invoker;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args)
            throws Throwable {

        // Object 方法直通
        if (method.getDeclaringClass() == Object.class) {
            return method.invoke(invoker, args);
        }

        Invocation invocation = new DefaultInvocation(
                invoker.getInterface().getName(),
                method.getName(),
                method.getParameterTypes(),
                args
        );

        Result result = invoker.invoke(invocation);

        if (result.hasException()) {
            throw result.getException();
        }
        return result.getValue(method.getReturnType());
    }
}
