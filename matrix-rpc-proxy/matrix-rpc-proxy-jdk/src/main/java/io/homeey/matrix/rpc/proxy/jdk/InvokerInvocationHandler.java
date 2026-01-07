package io.homeey.matrix.rpc.proxy.jdk;

import io.homeey.matrix.rpc.common.Result;
import io.homeey.matrix.rpc.core.Invocation;
import io.homeey.matrix.rpc.core.Invoker;
import io.homeey.matrix.rpc.core.SimpleInvocation;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

/**
 * JDK 动态代理的 InvocationHandler 实现
 * <p>
 * 负责将 Method 调用转换为 RPC Invocation，并委托给 Invoker 执行
 * </p>
 *
 * @author jt4mrg@gmail.com
 * @since 2026-01-08
 */
public class InvokerInvocationHandler implements InvocationHandler {

    private final Invoker<?> invoker;

    public InvokerInvocationHandler(Invoker<?> invoker) {
        this.invoker = invoker;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        // 处理 Object 类的方法
        if (method.getDeclaringClass() == Object.class) {
            return handleObjectMethod(proxy, method, args);
        }

        // 构建 Invocation
        Invocation invocation = new SimpleInvocation(
                invoker.getInterface().getName(),
                method.getName(),
                method.getParameterTypes(),
                args == null ? new Object[0] : args
        );

        // 执行远程调用
        Result result = invoker.invoke(invocation);

        // 处理结果
        if (result.hasException()) {
            throw result.getException();
        }

        return result.getValue();
    }

    /**
     * 处理 Object 类的方法（toString, hashCode, equals）
     */
    private Object handleObjectMethod(Object proxy, Method method, Object[] args) {
        String methodName = method.getName();

        if ("toString".equals(methodName)) {
            return invoker.toString();
        }
        if ("hashCode".equals(methodName)) {
            return invoker.hashCode();
        }
        if ("equals".equals(methodName)) {
            return proxy == args[0];
        }

        throw new UnsupportedOperationException("Unsupported Object method: " + methodName);
    }
}
