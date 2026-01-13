package io.homeey.example.sentinel;

import io.homeey.example.api.EchoService;
import io.homeey.matrix.rpc.runtime.RpcReference;

/**
 * Sentinel 熔断测试
 * 
 * <p>测试场景:
 * 1. 正常调用
 * 2. 高频异常调用 (触发熔断)
 * 3. 熔断期间调用 (直接拒绝)
 * 4. 等待恢复 (半开状态测试)
 * 
 * <p>运行方式:
 * 1. 先启动 matrix-rpc-example-provider 模块的 ProviderMain
 * 2. 再启动本测试类
 * 
 * @author Matrix RPC Team
 * @since 2026-01-11
 */
public class SentinelCircuitBreakerTest {
    
    public static void main(String[] args) throws InterruptedException {
        System.out.println("========================================");
        System.out.println("Sentinel Circuit Breaker Test");
        System.out.println("========================================");
        
        // 创建 RPC 引用 (直连模式)
        EchoService echoService = RpcReference.create(EchoService.class)
                .address("localhost", 20880)
                .get();
        
        // 测试1: 正常调用
        System.out.println("\n--- Test 1: Normal Calls ---");
        for (int i = 1; i <= 3; i++) {
            try {
                String result = echoService.echo("Normal-" + i);
                System.out.println("[SUCCESS] Call " + i + ": " + result);
            } catch (Exception e) {
                System.err.println("[FAILED] Call " + i + ": " + e.getMessage());
            }
        }
        
        // 测试2: 触发异常 (50%异常率, 触发熔断)
        System.out.println("\n--- Test 2: High Error Rate Calls (Trigger Circuit Breaker) ---");
        System.out.println("Expected: After 5 calls with 50% error rate, circuit breaker should open");
        
        int successCount = 0;
        int errorCount = 0;
        
        for (int i = 1; i <= 10; i++) {
            try {
                // 发送 ERROR- 前缀的消息, Provider会模拟50%的异常
                String result = echoService.echo("ERROR-" + i);
                System.out.println("[SUCCESS] Call " + i + ": " + result);
                successCount++;
            } catch (Exception e) {
                if (e.getMessage() != null && e.getMessage().contains("circuit breaker")) {
                    System.out.println("[CIRCUIT BREAKER] Call " + i + ": Circuit breaker is OPEN!");
                    break; // 熔断器打开, 停止测试
                } else {
                    System.err.println("[ERROR] Call " + i + ": " + e.getMessage());
                    errorCount++;
                }
            }
            Thread.sleep(100);
        }
        
        System.out.println("\nError statistics:");
        System.out.println("  Success: " + successCount);
        System.out.println("  Error: " + errorCount);
        System.out.println("  Error Rate: " + (errorCount * 100.0 / (successCount + errorCount)) + "%");
        
        // 测试3: 熔断期间调用
        System.out.println("\n--- Test 3: Calls During Circuit Breaker Open ---");
        System.out.println("Expected: All calls should be rejected by circuit breaker");
        
        int rejectedCount = 0;
        for (int i = 1; i <= 5; i++) {
            try {
                String result = echoService.echo("Test-" + i);
                System.out.println("[SUCCESS] Call " + i + ": " + result);
            } catch (Exception e) {
                if (e.getMessage() != null && e.getMessage().contains("circuit breaker")) {
                    System.out.println("[REJECTED] Call " + i + ": Circuit breaker is still OPEN");
                    rejectedCount++;
                } else {
                    System.err.println("[ERROR] Call " + i + ": " + e.getMessage());
                }
            }
            Thread.sleep(100);
        }
        
        // 测试4: 等待恢复
        System.out.println("\n--- Test 4: Waiting for Circuit Breaker Recovery ---");
        System.out.println("Waiting 12 seconds for circuit breaker to enter HALF_OPEN state...");
        Thread.sleep(12000); // 等待12秒 (熔断时长10秒)
        
        System.out.println("\n--- Test 5: Recovery Test ---");
        System.out.println("Expected: Circuit breaker should be HALF_OPEN, allowing test calls");
        
        for (int i = 1; i <= 5; i++) {
            try {
                String result = echoService.echo("Recovery-" + i);
                System.out.println("[SUCCESS] Call " + i + ": " + result + " (Circuit breaker recovering)");
            } catch (Exception e) {
                System.err.println("[FAILED] Call " + i + ": " + e.getMessage());
            }
            Thread.sleep(200);
        }
        
        // 总结
        System.out.println("\n========================================");
        System.out.println("Test Summary:");
        System.out.println("  Normal calls: 3");
        System.out.println("  Error triggered calls: " + (successCount + errorCount));
        System.out.println("  Rejected by circuit breaker: " + rejectedCount);
        System.out.println("  Recovery test calls: 5");
        System.out.println("========================================");
        
        if (rejectedCount > 0) {
            System.out.println("\n✅ Sentinel Circuit Breaker is working!");
        } else {
            System.out.println("\n⚠️ Warning: Circuit breaker not triggered!");
        }
    }
}
