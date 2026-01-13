package io.homeey.example.resilience4j;

import io.homeey.example.api.EchoService;
import io.homeey.matrix.rpc.runtime.RpcReference;

/**
 * Resilience4j 限流测试
 *
 * <p>测试场景:
 * 1. 正常调用 (QPS < 10)
 * 2. 超限调用 (QPS > 10, 触发限流)
 *
 * <p>注意: 由于已独立模块,无需禁用Sentinel (已隔离)
 *
 * <p>运行方式:
 * 1. 先启动 matrix-rpc-example-provider 模块的 ProviderMain
 * 2. 再启动本测试类
 *
 * @author Matrix RPC Team
 * @since 2026-01-11
 */
public class Resilience4jRateLimitTest {

    public static void main(String[] args) throws InterruptedException {
        System.out.println("========================================");
        System.out.println("Resilience4j Rate Limit Test");
        System.out.println("========================================");

        // 创建 RPC 引用 (直连模式)
        EchoService echoService = RpcReference.create(EchoService.class)
                .address("localhost", 20880)
                .get();

        // 测试1: 正常调用 (不超限)
        System.out.println("\n--- Test 1: Normal Calls (Within Limit) ---");
        for (int i = 1; i <= 5; i++) {
            try {
                String result = echoService.echo("Normal-" + i);
                System.out.println("[SUCCESS] Call " + i + ": " + result);
                Thread.sleep(200); // 每次调用间隔200ms (QPS=5, 不超限)
            } catch (Exception e) {
                System.err.println("[FAILED] Call " + i + ": " + e.getMessage());
            }
        }

        // 测试2: 快速调用 (超限)
        System.out.println("\n--- Test 2: Rapid Calls (Exceed Limit) ---");
        System.out.println("Expected: Some calls will be rate limited (QPS limit = 10)");

        int successCount = 0;
        int rateLimitedCount = 0;

        for (int i = 1; i <= 20; i++) {
            try {
                String result = echoService.echo("Rapid-" + i);
                System.out.println("[SUCCESS] Call " + i + ": " + result);
                successCount++;
            } catch (Exception e) {
                if (e.getMessage() != null && e.getMessage().contains("rate limit")) {
                    System.out.println("[RATE LIMITED] Call " + i + ": " + e.getMessage());
                    rateLimitedCount++;
                } else {
                    System.err.println("[ERROR] Call " + i + ": " + e.getMessage());
                }
            }
            Thread.sleep(50); // 间隔50ms (QPS=20, 远超限)
        }

        // 统计结果
        System.out.println("\n========================================");
        System.out.println("Test Results:");
        System.out.println("  Success Count: " + successCount);
        System.out.println("  Rate Limited Count: " + rateLimitedCount);
        System.out.println("  Total: " + (successCount + rateLimitedCount));
        System.out.println("========================================");

        if (rateLimitedCount > 0) {
            System.out.println("\n✅ Resilience4j Rate Limiter is working!");
        } else {
            System.out.println("\n⚠️ Warning: No rate limiting detected!");
        }
    }
}
