package io.homeey.matrix.rpc.proxy.jdk;

import io.homeey.matrix.rpc.core.Result;
import io.homeey.matrix.rpc.core.Invocation;
import io.homeey.matrix.rpc.core.Invoker;
import io.homeey.matrix.rpc.core.SimpleInvocation;
import io.homeey.matrix.rpc.core.URL;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

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
    private final URL url;

    public InvokerInvocationHandler(Invoker<?> invoker) {
        this(invoker, null);
    }

    public InvokerInvocationHandler(Invoker<?> invoker, URL url) {
        this.invoker = invoker;
        this.url = url;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        // 处理 Object 类的方法
        if (method.getDeclaringClass() == Object.class) {
            return handleObjectMethod(proxy, method, args);
        }

        // 构建 Invocation，并添加 URL 参数到 attachments
        Map<String, String> attachments = new HashMap<>();
        if (url != null) {
            // 从 URL 中提取标签路由相关参数
            String tag = url.getParameter("tag");
            if (tag != null && !tag.isEmpty()) {
                attachments.put("tag", tag);
                String tagForce = url.getParameter("tag.force");
                if (tagForce != null) {
                    attachments.put("tag.force", tagForce);
                }
            }
            
            // 添加其他可能需要的参数
            String timeout = url.getParameter("timeout");
            if (timeout != null) {
                attachments.put("timeout", timeout);
            }
            String retries = url.getParameter("retries");
            if (retries != null) {
                attachments.put("retries", retries);
            }
        }
        
        Invocation invocation = new SimpleInvocation(
                invoker.getInterface().getName(),
                method.getName(),
                method.getParameterTypes(),
                args == null ? new Object[0] : args,
                attachments
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
