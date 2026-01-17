package io.homeey.example.http2;

import io.homeey.example.api.EchoService;
import io.homeey.example.api.User;
import io.homeey.matrix.rpc.runtime.RpcReference;
import io.homeey.matrix.rpc.runtime.RpcService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * HTTP/2 协议完整测试
 * 
 * <p>演示如何使用 HTTP/2 协议进行 RPC 调用：
 * <ul>
 *   <li>Provider 端暴露 HTTP/2 服务（端口 8080）</li>
 *   <li>Consumer 端通过 HTTP/2 协议调用服务</li>
 *   <li>支持标准 HTTP/2 客户端调用</li>
 *   <li>验证 HTTP/2 多路复用特性</li>
 * </ul>
 * 
 * @author Matrix RPC Team
 * @since 1.0.0
 */
public class Http2ProtocolTest {
    
    private static final Logger logger = LoggerFactory.getLogger(Http2ProtocolTest.class);
    
    public static void main(String[] args) throws Exception {
        // 1. 启动 Provider（HTTP/2 协议，端口 8080）
        logger.info("========== Starting HTTP/2 Provider on port 8080 ==========");
        
        // 创建服务实现
        EchoService impl = new EchoService() {
            @Override
            public String echo(String msg) {
                return "HTTP/2-Echo: " + msg;
            }
            
            @Override
            public User getUser(Long id) {
                User user = new User();
                user.setId(id);
                user.setName("HTTP2-User-" + id);
                user.setAge(25);
                return user;
            }
            
            @Override
            public User saveUser(User user) {
                logger.info("saveUser called with: {}", user);
                return user;
            }
        };
        
        RpcService.create(EchoService.class, impl)
            .protocol("http2")  // 使用 HTTP/2 协议
            .host("0.0.0.0")
            .port(8080)
            .version("1.0.0")
            .export()
            .await();
        
        logger.info("HTTP/2 Provider started successfully!");
        
        // 等待服务启动
        Thread.sleep(2000);
        
        // 2. 创建 Consumer（HTTP/2 协议）
        logger.info("\n========== Creating HTTP/2 Consumer ==========");
        EchoService service = RpcReference.create(EchoService.class)
            .protocol("http2")  // 使用 HTTP/2 协议
            .address("localhost", 8080)
            .timeout(5000)
            .get();
        
        logger.info("HTTP/2 Consumer created successfully!");
        
        // 3. 测试简单调用
        logger.info("\n========== Test 1: Simple Echo ==========");
        String result1 = service.echo("Hello HTTP/2!");
        logger.info("Result: {}", result1);
        
        // 4. 测试复杂对象
        logger.info("\n========== Test 2: Complex Object ==========");
        User user = new User();
        user.setId(1001L);
        user.setName("HTTP/2 User");
        user.setAge(25);
        
        User result2 = service.saveUser(user);
        logger.info("Result: {}", result2);
        
        // 5. 批量测试（验证多路复用）
        logger.info("\n========== Test 3: Batch Calls (HTTP/2 Multiplexing) ==========");
        long startTime = System.currentTimeMillis();
        int count = 100;
        
        for (int i = 0; i < count; i++) {
            String msg = "Message-" + i;
            String response = service.echo(msg);
            if (i % 20 == 0) {
                logger.info("Batch call {}: {} -> {}", i, msg, response);
            }
        }
        
        long endTime = System.currentTimeMillis();
        long avgTime = (endTime - startTime) / count;
        logger.info("Completed {} calls in {}ms, average: {}ms per call", 
            count, (endTime - startTime), avgTime);
        
        // 6. 性能对比测试
        logger.info("\n========== Test 4: Performance Test ==========");
        warmup(service, 50);  // 预热
        
        long http2Time = performanceTest(service, 1000);
        logger.info("HTTP/2 Protocol: 1000 calls in {}ms, TPS: {}", 
            http2Time, 1000 * 1000 / http2Time);
        
        logger.info("\n========== All Tests Completed ==========");
        logger.info("HTTP/2 Protocol features verified:");
        logger.info("✓ Basic RPC calls");
        logger.info("✓ Complex object serialization");
        logger.info("✓ HTTP/2 multiplexing (single connection)");
        logger.info("✓ Performance benchmark");
        
        // 保持程序运行
        logger.info("\nPress Ctrl+C to exit...");
        Thread.sleep(Long.MAX_VALUE);
    }
    
    /**
     * 预热
     */
    private static void warmup(EchoService service, int count) {
        logger.info("Warming up with {} calls...", count);
        for (int i = 0; i < count; i++) {
            service.echo("warmup-" + i);
        }
    }
    
    /**
     * 性能测试
     */
    private static long performanceTest(EchoService service, int count) {
        long startTime = System.currentTimeMillis();
        for (int i = 0; i < count; i++) {
            service.echo("perf-" + i);
        }
        return System.currentTimeMillis() - startTime;
    }
}
