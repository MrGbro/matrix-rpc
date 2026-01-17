package io.homeey.matrix.rpc.proxy.cglib;

import io.homeey.matrix.rpc.core.Invocation;
import io.homeey.matrix.rpc.core.Invoker;
import io.homeey.matrix.rpc.core.Result;
import io.homeey.matrix.rpc.core.SimpleInvocation;
import net.sf.cglib.proxy.MethodInterceptor;
import net.sf.cglib.proxy.MethodProxy;

import java.lang.reflect.Method;
import java.util.HashMap;

/**
 * CGLIB 方法拦截器
 * 
 * <p>负责将方法调用转换为 RPC Invocation，是 CGLIB 代理的核心逻辑。
 * 
 * <h3>职责：</h3>
 * <ul>
 *   <li>拦截所有非 Object 方法的调用</li>
 *   <li>构建 {@link Invocation} 对象</li>
 *   <li>委托给 {@link Invoker#invoke} 执行远程调用</li>
 *   <li>处理调用结果或异常</li>
 * </ul>
 * 
 * <h3>性能特点：</h3>
 * <p>CGLIB 使用 FastClass 机制，避免 JDK 反射的 Method.invoke()，
 * 直接通过索引调用方法，性能比 JDK Proxy 高约 40%。
 * 
 * @author Matrix RPC Team
 * @since 1.0.0
 */
public class InvokerMethodInterceptor implements MethodInterceptor {
    
    private final Invoker<?> invoker;
    
    /**
     * 构造方法拦截器
     * 
     * @param invoker RPC 调用器
     */
    public InvokerMethodInterceptor(Invoker<?> invoker) {
        this.invoker = invoker;
    }
    
    /**
     * 拦截方法调用
     * 
     * @param obj 代理对象
     * @param method 被调用的方法
     * @param args 方法参数
     * @param proxy CGLIB MethodProxy（可用于调用父类方法）
     * @return 方法返回值
     * @throws Throwable 方法执行异常
     */
    @Override
    public Object intercept(Object obj, Method method, Object[] args, MethodProxy proxy) 
            throws Throwable {
        
        // 处理 Object 类的方法（toString, hashCode, equals）
        if (method.getDeclaringClass() == Object.class) {
            return handleObjectMethod(obj, method, args);
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
     * 处理 Object 类的方法
     * 
     * <p>这些方法不需要进行远程调用，直接在本地处理
     * 
     * @param obj 代理对象
     * @param method 方法
     * @param args 参数
     * @return 方法返回值
     */
    private Object handleObjectMethod(Object obj, Method method, Object[] args) {
        String methodName = method.getName();
        
        if ("toString".equals(methodName)) {
            return invoker.toString();
        }
        if ("hashCode".equals(methodName)) {
            return invoker.hashCode();
        }
        if ("equals".equals(methodName)) {
            return obj == args[0];
        }
        
        throw new UnsupportedOperationException("Unsupported Object method: " + methodName);
    }
}
