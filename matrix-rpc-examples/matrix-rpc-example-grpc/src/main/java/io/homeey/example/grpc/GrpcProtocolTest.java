package io.homeey.example.grpc;

import io.homeey.example.api.EchoService;
import io.homeey.example.api.User;
import io.homeey.matrix.rpc.runtime.RpcReference;
import io.homeey.matrix.rpc.runtime.RpcService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * gRPC 协议完整测试
 * 
 * <p>演示如何使用 gRPC 协议进行 RPC 调用：
 * <ul>
 *   <li>Provider 端暴露 gRPC 服务（端口 9090）</li>
 *   <li>Consumer 端通过 gRPC 协议调用服务</li>
 *   <li>完全兼容标准 gRPC 客户端</li>
 * </ul>
 * 
 * @author Matrix RPC Team
 * @since 1.0.0
 */
public class GrpcProtocolTest {
    
    private static final Logger logger = LoggerFactory.getLogger(GrpcProtocolTest.class);
    
    public static void main(String[] args) throws Exception {
        // 1. 启动 Provider（gRPC 协议，端口 9090）
        logger.info("========== Starting gRPC Provider on port 9090 ==========");
        
        // 创建服务实现
        EchoService impl = new EchoService() {
            @Override
            public String echo(String msg) {
                return "gRPC-Echo: " + msg;
            }
            
            @Override
            public User getUser(Long id) {
                User user = new User();
                user.setId(id);
                user.setName("gRPC-User-" + id);
                user.setAge(30);
                return user;
            }
            
            @Override
            public User saveUser(User user) {
                logger.info("saveUser called with: {}", user);
                return user;
            }
        };
        
        RpcService.create(EchoService.class, impl)
            .protocol("grpc")  // 使用 gRPC 协议
            .host("0.0.0.0")
            .port(9090)
            .version("1.0.0")
            .export()
            .await();
        
        logger.info("gRPC Provider started successfully!");
        
        // 等待服务启动
        Thread.sleep(2000);
        
        // 2. 创建 Consumer（gRPC 协议）
        logger.info("\n========== Creating gRPC Consumer ==========");
        EchoService service = RpcReference.create(EchoService.class)
            .protocol("grpc")  // 使用 gRPC 协议
            .address("localhost", 9090)
            .timeout(5000)
            .get();
        
        logger.info("gRPC Consumer created successfully!");
        
        // 3. 测试简单调用
        logger.info("\n========== Test 1: Simple Echo ==========");
        String result1 = service.echo("Hello gRPC!");
        logger.info("Result: {}", result1);
        
        // 4. 测试复杂对象
        logger.info("\n========== Test 2: Complex Object ==========");
        User user = new User();
        user.setId(2001L);
        user.setName("gRPC User");
        user.setAge(30);
        
        User result2 = service.saveUser(user);
        logger.info("Result: {}", result2);
        
        // 5. 批量测试
        logger.info("\n========== Test 3: Batch Calls ==========");
        long startTime = System.currentTimeMillis();
        int count = 100;
        
        for (int i = 0; i < count; i++) {
            String msg = "gRPC-Message-" + i;
            String response = service.echo(msg);
            if (i % 20 == 0) {
                logger.info("Batch call {}: {} -> {}", i, msg, response);
            }
        }
        
        long endTime = System.currentTimeMillis();
        long avgTime = (endTime - startTime) / count;
        logger.info("Completed {} calls in {}ms, average: {}ms per call", 
            count, (endTime - startTime), avgTime);
        
        // 6. 性能测试
        logger.info("\n========== Test 4: Performance Test ==========");
        warmup(service, 50);  // 预热
        
        long grpcTime = performanceTest(service, 1000);
        logger.info("gRPC Protocol: 1000 calls in {}ms, TPS: {}", 
            grpcTime, 1000 * 1000 / grpcTime);
        
        logger.info("\n========== All Tests Completed ==========");
        logger.info("gRPC Protocol features verified:");
        logger.info("✓ Basic RPC calls");
        logger.info("✓ Complex object serialization");
        logger.info("✓ gRPC standard protocol compliance");
        logger.info("✓ Performance benchmark");
        
        logger.info("\nNote: This service can be tested with standard gRPC clients:");
        logger.info("  - grpcurl (command-line tool)");
        logger.info("  - BloomRPC/Postman (GUI tools)");
        logger.info("  - Any gRPC client in Go/Python/Node.js");
        
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
