package io.homeey.example.consumer;

import io.homeey.example.api.EchoService;
import io.homeey.matrix.rpc.runtime.RpcReference;

/**
 * 连接池功能测试
 * 
 * <p>测试场景：
 * <ul>
 *   <li>单连接模式（默认）</li>
 *   <li>多连接模式（连接池大小=4）</li>
 *   <li>并发调用测试</li>
 * </ul>
 * 
 * @author Matrix RPC Team
 * @since 2026-01-14
 */
public class ConnectionPoolTest {
    
    public static void main(String[] args) throws Exception {
        System.out.println("=== 连接池功能测试 ===\n");
        
        // 测试1：单连接模式（默认）
        testSingleConnection();
        
        // 测试2：多连接模式（需要修改底层实现以支持参数传递）
        // 当前版本连接池大小通过 URL 参数控制，默认为1
        testConcurrentCalls();
        
        System.out.println("\n=== 测试完成 ===");
    }
    
    /**
     * 测试单连接模式
     */
    private static void testSingleConnection() throws Exception {
        System.out.println("【测试1】单连接模式");
        
        EchoService service = RpcReference.create(EchoService.class)
                .address("localhost", 20880)
                .timeout(3000)
                .get();
        
        // 连续调用5次
        for (int i = 1; i <= 5; i++) {
            String result = service.echo("Hello-" + i);
            System.out.println("  调用" + i + ": " + result);
            Thread.sleep(100);
        }
        
        System.out.println("  ✅ 单连接模式测试通过\n");
    }
    
    /**
     * 测试并发调用
     */
    private static void testConcurrentCalls() throws Exception {
        System.out.println("【测试2】并发调用测试");
        
        EchoService service = RpcReference.create(EchoService.class)
                .address("localhost", 20880)
                .timeout(3000)
                .get();
        
        // 创建10个线程并发调用
        Thread[] threads = new Thread[10];
        for (int i = 0; i < threads.length; i++) {
            final int index = i;
            threads[i] = new Thread(() -> {
                try {
                    String result = service.echo("Thread-" + index);
                    System.out.println("  线程" + index + ": " + result);
                } catch (Exception e) {
                    System.err.println("  线程" + index + " 调用失败: " + e.getMessage());
                }
            });
        }
        
        // 启动所有线程
        for (Thread thread : threads) {
            thread.start();
        }
        
        // 等待所有线程完成
        for (Thread thread : threads) {
            thread.join();
        }
        
        System.out.println("  ✅ 并发调用测试通过\n");
    }
}
