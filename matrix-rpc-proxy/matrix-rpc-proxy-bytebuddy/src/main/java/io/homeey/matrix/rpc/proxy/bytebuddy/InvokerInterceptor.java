package io.homeey.matrix.rpc.proxy.bytebuddy;

import io.homeey.matrix.rpc.core.Invocation;
import io.homeey.matrix.rpc.core.Invoker;
import io.homeey.matrix.rpc.core.Result;
import io.homeey.matrix.rpc.core.SimpleInvocation;
import net.bytebuddy.implementation.bind.annotation.*;

import java.lang.reflect.Method;
import java.util.HashMap;

/**
 * ByteBuddy 方法拦截器
 * 
 * <p>负责将方法调用转换为 RPC Invocation，职责与 InvokerInvocationHandler 一致
 * 
 * @author Matrix RPC Team
 * @since 1.0.0
 */
public class InvokerInterceptor {
    
    private final Invoker<?> invoker;
    
    public InvokerInterceptor(Invoker<?> invoker) {
        this.invoker = invoker;
    }
    
    /**
     * 拦截所有方法调用
     * 
     * @RuntimeType 表示返回值类型动态匹配
     */
    @RuntimeType
    public Object intercept(
        @Origin Method method,              // 原始方法
        @AllArguments Object[] args,        // 方法参数
        @This Object proxy                  // 代理对象本身
    ) throws Throwable {
        
        // 处理 Object 类的方法
        if (method.getDeclaringClass() == Object.class) {
            return handleObjectMethod(proxy, method, args);
        }
        
        // 构建 Invocation
        Invocation invocation = new SimpleInvocation(
            invoker.getInterface().getName(),
            method.getName(),
            method.getParameterTypes(),
            args == null ? new Object[0] : args,
            new HashMap<>()  // 可扩展：从 ThreadLocal 或 URL 获取 attachments
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
