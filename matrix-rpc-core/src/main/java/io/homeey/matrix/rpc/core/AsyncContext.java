package io.homeey.matrix.rpc.core;

import java.util.concurrent.CompletableFuture;

/**
 * 异步调用上下文 - 管理当前线程的异步调用状态
 * 
 * <p>使用 ThreadLocal 实现线程隔离，确保多线程环境下的安全性
 * 
 * <p>使用示例：
 * <pre>
 * try {
 *     AsyncContext.enableAsync();
 *     service.echo("hello"); // 触发调用
 *     CompletableFuture&lt;String&gt; future = AsyncContext.getAsyncFuture();
 *     future.thenAccept(result -&gt; System.out.println(result));
 * } finally {
 *     AsyncContext.disableAsync(); // 必须清理
 * }
 * </pre>
 * 
 * @author Matrix RPC Team
 * @since 2026-01-14
 */
public class AsyncContext {
    
    /**
     * 当前线程的异步 Future
     */
    private static final ThreadLocal<CompletableFuture<?>> ASYNC_FUTURE = new ThreadLocal<>();
    
    /**
     * 当前线程是否为异步模式
     */
    private static final ThreadLocal<Boolean> ASYNC_MODE = ThreadLocal.withInitial(() -> false);
    
    /**
     * 启用异步模式（当前线程）
     * 
     * <p>启用后，所有 RPC 调用将返回 CompletableFuture
     */
    public static void enableAsync() {
        ASYNC_MODE.set(true);
    }
    
    /**
     * 禁用异步模式并清理上下文
     * 
     * <p>⚠️ 重要：必须在 finally 块中调用，避免内存泄漏
     */
    public static void disableAsync() {
        ASYNC_MODE.set(false);
        ASYNC_FUTURE.remove();
    }
    
    /**
     * 判断当前线程是否为异步模式
     * 
     * @return true 表示异步模式
     */
    public static boolean isAsyncMode() {
        return ASYNC_MODE.get();
    }
    
    /**
     * 获取当前线程的异步 Future
     * 
     * @param <T> 返回值类型
     * @return 异步 Future，如果不是异步模式则返回 null
     */
    @SuppressWarnings("unchecked")
    public static <T> CompletableFuture<T> getAsyncFuture() {
        return (CompletableFuture<T>) ASYNC_FUTURE.get();
    }
    
    /**
     * 设置当前线程的异步 Future
     * 
     * <p>⚠️ 内部使用，用户不应直接调用
     * 
     * @param future 异步 Future
     */
    public static void setAsyncFuture(CompletableFuture<?> future) {
        ASYNC_FUTURE.set(future);
    }
}
