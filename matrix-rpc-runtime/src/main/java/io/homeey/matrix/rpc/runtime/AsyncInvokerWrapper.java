package io.homeey.matrix.rpc.runtime;

import io.homeey.matrix.rpc.core.*;

import java.util.concurrent.CompletableFuture;

/**
 * 异步调用装饰器 - 将同步 Invoker 包装为支持异步调用
 * 
 * <p>工作原理：
 * <ul>
 *   <li>检查当前线程是否启用异步模式（通过 AsyncContext.isAsyncMode()）</li>
 *   <li>如果启用异步，返回 null 并将 CompletableFuture 存入 AsyncContext</li>
 *   <li>如果未启用异步，直接同步调用并返回结果</li>
 * </ul>
 * 
 * <p>使用示例：
 * <pre>
 * Invoker&lt;EchoService&gt; asyncInvoker = new AsyncInvokerWrapper&lt;&gt;(originalInvoker);
 * 
 * // 异步调用
 * AsyncContext.enableAsync();
 * try {
 *     service.echo("hello"); // 立即返回 null
 *     CompletableFuture&lt;String&gt; future = AsyncContext.getAsyncFuture();
 *     future.thenAccept(result -&gt; System.out.println(result));
 * } finally {
 *     AsyncContext.disableAsync();
 * }
 * </pre>
 * 
 * @param <T> 服务接口类型
 * @author Matrix RPC Team
 * @since 2026-01-14
 */
public class AsyncInvokerWrapper<T> implements Invoker<T> {
    
    /**
     * 被装饰的原始 Invoker
     */
    private final Invoker<T> delegate;
    
    /**
     * 创建异步调用装饰器
     * 
     * @param delegate 原始 Invoker
     */
    public AsyncInvokerWrapper(Invoker<T> delegate) {
        if (delegate == null) {
            throw new IllegalArgumentException("Delegate invoker cannot be null");
        }
        this.delegate = delegate;
    }
    
    @Override
    public Class<T> getInterface() {
        return delegate.getInterface();
    }
    
    @Override
    public Result invoke(Invocation invocation) throws RpcException {
        // 检查是否启用异步模式
        if (AsyncContext.isAsyncMode()) {
            // 异步模式：创建 CompletableFuture 并异步执行
            CompletableFuture<Object> future = CompletableFuture.supplyAsync(() -> {
                try {
                    Result result = delegate.invoke(invocation);
                    if (result.hasException()) {
                        throw new RuntimeException(result.getException());
                    }
                    return result.getValue();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
            
            // 将 Future 存入 AsyncContext
            AsyncContext.setAsyncFuture(future);
            
            // 返回 null（用户应从 AsyncContext 获取 Future）
            return new Result(null);
        } else {
            // 同步模式：直接调用
            return delegate.invoke(invocation);
        }
    }
}
