package io.homeey.example.consumer;

import io.homeey.example.api.EchoService;
import io.homeey.matrix.rpc.core.AsyncContext;
import io.homeey.matrix.rpc.runtime.RpcReference;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * 异步调用功能测试
 * 
 * <p>测试场景：
 * <ul>
 *   <li>同步调用（默认模式）</li>
 *   <li>单次异步调用</li>
 *   <li>全局异步模式</li>
 *   <li>异步调用链式处理</li>
 * </ul>
 * 
 * @author Matrix RPC Team
 * @since 2026-01-14
 */
public class AsyncCallTest {
    
    public static void main(String[] args) throws Exception {
        System.out.println("=== 异步调用功能测试 ===\n");
        
        // 测试1：同步调用
        testSyncCall();
        
        // 测试2：单次异步调用
        testSingleAsyncCall();
        
        // 测试3：全局异步模式
        testGlobalAsyncMode();
        
        // 测试4：异步调用链式处理
        testAsyncChain();
        
        System.out.println("\n=== 测试完成 ===");
    }
    
    /**
     * 测试同步调用
     */
    private static void testSyncCall() throws Exception {
        System.out.println("【测试1】同步调用");
        
        EchoService service = RpcReference.create(EchoService.class)
                .address("localhost", 20880)
                .get();
        
        long startTime = System.currentTimeMillis();
        String result = service.echo("Sync Call");
        long endTime = System.currentTimeMillis();
        
        System.out.println("  结果: " + result);
        System.out.println("  耗时: " + (endTime - startTime) + "ms");
        System.out.println("  ✅ 同步调用测试通过\n");
    }
    
    /**
     * 测试单次异步调用
     */
    private static void testSingleAsyncCall() throws Exception {
        System.out.println("【测试2】单次异步调用");
        
        EchoService service = RpcReference.create(EchoService.class)
                .address("localhost", 20880)
                .get();
        
        // 启用异步
        AsyncContext.enableAsync();
        try {
            long startTime = System.currentTimeMillis();
            
            // 调用立即返回
            service.echo("Async Call");
            
            // 获取 Future
            CompletableFuture<String> future = AsyncContext.getAsyncFuture();
            System.out.println("  调用已发起，立即返回（非阻塞）");
            
            // 异步等待结果
            String result = future.get(5, TimeUnit.SECONDS);
            long endTime = System.currentTimeMillis();
            
            System.out.println("  结果: " + result);
            System.out.println("  耗时: " + (endTime - startTime) + "ms");
            System.out.println("  ✅ 单次异步调用测试通过\n");
        } finally {
            AsyncContext.disableAsync();
        }
    }
    
    /**
     * 测试全局异步模式
     */
    private static void testGlobalAsyncMode() throws Exception {
        System.out.println("【测试3】全局异步模式");
        
        // 创建全局异步的服务引用
        EchoService service = RpcReference.create(EchoService.class)
                .address("localhost", 20880)
                .async()  // 启用全局异步
                .get();
        
        // 启用异步上下文
        AsyncContext.enableAsync();
        try {
            // 发起3个异步调用
            service.echo("Async-1");
            CompletableFuture<String> future1 = AsyncContext.getAsyncFuture();
            
            AsyncContext.enableAsync();  // 重新启用
            service.echo("Async-2");
            CompletableFuture<String> future2 = AsyncContext.getAsyncFuture();
            
            AsyncContext.enableAsync();  // 重新启用
            service.echo("Async-3");
            CompletableFuture<String> future3 = AsyncContext.getAsyncFuture();
            
            // 等待所有结果
            System.out.println("  结果1: " + future1.get(5, TimeUnit.SECONDS));
            System.out.println("  结果2: " + future2.get(5, TimeUnit.SECONDS));
            System.out.println("  结果3: " + future3.get(5, TimeUnit.SECONDS));
            System.out.println("  ✅ 全局异步模式测试通过\n");
        } finally {
            AsyncContext.disableAsync();
        }
    }
    
    /**
     * 测试异步调用链式处理
     */
    private static void testAsyncChain() throws Exception {
        System.out.println("【测试4】异步调用链式处理");
        
        EchoService service = RpcReference.create(EchoService.class)
                .address("localhost", 20880)
                .get();
        
        AsyncContext.enableAsync();
        try {
            service.echo("Chain Call");
            CompletableFuture<String> future = AsyncContext.getAsyncFuture();
            
            // 链式处理
            future.thenApply(result -> {
                System.out.println("  第1步处理: " + result);
                return result.toUpperCase();
            }).thenApply(result -> {
                System.out.println("  第2步处理: " + result);
                return result + " [已处理]";
            }).thenAccept(result -> {
                System.out.println("  最终结果: " + result);
            }).get(5, TimeUnit.SECONDS);
            
            System.out.println("  ✅ 异步调用链式处理测试通过\n");
        } finally {
            AsyncContext.disableAsync();
        }
    }
}
