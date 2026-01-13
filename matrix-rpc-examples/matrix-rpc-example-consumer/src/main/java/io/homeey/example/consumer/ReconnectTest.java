package io.homeey.example.consumer;

import io.homeey.example.api.EchoService;
import io.homeey.matrix.rpc.runtime.RpcReference;

/**
 * 断线重连功能测试
 * 
 * <p>测试场景：
 * <ul>
 *   <li>正常连接测试</li>
 *   <li>服务端断开后自动重连</li>
 *   <li>重连延迟验证（需手动观察日志）</li>
 * </ul>
 * 
 * <p>测试步骤：
 * <ol>
 *   <li>启动 Provider</li>
 *   <li>运行此测试</li>
 *   <li>在测试运行期间，手动停止 Provider</li>
 *   <li>观察重连日志（1s → 2s → 4s → 8s...）</li>
 *   <li>重新启动 Provider</li>
 *   <li>观察重连成功日志</li>
 * </ol>
 * 
 * @author Matrix RPC Team
 * @since 2026-01-14
 */
public class ReconnectTest {
    
    public static void main(String[] args) throws Exception {
        System.out.println("=== 断线重连功能测试 ===\n");
        System.out.println("测试说明：");
        System.out.println("1. 确保 Provider 已启动");
        System.out.println("2. 程序将持续调用服务");
        System.out.println("3. 请在运行期间手动停止 Provider");
        System.out.println("4. 观察重连日志（延迟序列：1s → 2s → 4s → 8s → 16s → 30s）");
        System.out.println("5. 重新启动 Provider 观察自动重连\n");
        
        testReconnect();
    }
    
    /**
     * 测试重连功能
     */
    private static void testReconnect() throws Exception {
        System.out.println("【测试】断线重连");
        
        EchoService service = RpcReference.create(EchoService.class)
                .address("localhost", 20880)
                .timeout(3000)
                .get();
        
        int successCount = 0;
        int failCount = 0;
        
        // 持续调用60次，每次间隔2秒
        for (int i = 1; i <= 60; i++) {
            try {
                String result = service.echo("Call-" + i);
                System.out.println("  [" + i + "] ✅ 调用成功: " + result);
                successCount++;
            } catch (Exception e) {
                System.err.println("  [" + i + "] ❌ 调用失败: " + e.getMessage());
                failCount++;
            }
            
            // 每10次输出统计
            if (i % 10 == 0) {
                System.out.println("  --- 统计：成功=" + successCount + ", 失败=" + failCount + " ---\n");
            }
            
            Thread.sleep(2000);  // 每2秒调用一次
        }
        
        System.out.println("\n=== 测试完成 ===");
        System.out.println("总调用次数: 60");
        System.out.println("成功次数: " + successCount);
        System.out.println("失败次数: " + failCount);
        System.out.println("\n提示：如果中途停止过 Provider，应该能看到：");
        System.out.println("1. 连接断开时的调用失败");
        System.out.println("2. 重连日志（指数退避延迟）");
        System.out.println("3. 重连成功后调用恢复正常");
    }
}
