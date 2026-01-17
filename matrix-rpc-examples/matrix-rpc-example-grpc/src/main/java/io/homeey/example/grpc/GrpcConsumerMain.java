package io.homeey.example.grpc;

import io.homeey.example.api.EchoService;
import io.homeey.example.api.User;
import io.homeey.matrix.rpc.runtime.RpcReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * gRPC Consumer 独立测试
 * 
 * <p>用于测试 gRPC Consumer 连接到已启动的 Provider
 * <p>需要先启动 {@link GrpcProviderMain}
 * 
 * @author Matrix RPC Team
 * @since 1.0.0
 */
public class GrpcConsumerMain {
    
    private static final Logger logger = LoggerFactory.getLogger(GrpcConsumerMain.class);
    
    public static void main(String[] args) throws Exception {
        logger.info("========================================");
        logger.info("  Matrix RPC - gRPC Consumer");
        logger.info("========================================");
        logger.info("Connecting to gRPC Provider at localhost:9090...");
        
        // 创建 Consumer
        EchoService service = RpcReference.create(EchoService.class)
            .protocol("grpc")
            .address("localhost", 9090)
            .timeout(5000)
            .get();
        
        logger.info("gRPC Consumer connected successfully!");
        logger.info("");
        
        // 测试调用
        for (int i = 1; i <= 10; i++) {
            logger.info("========== Call {} ==========", i);
            
            // 简单调用
            String echoResult = service.echo("Hello gRPC - " + i);
            logger.info("echo result: {}", echoResult);
            
            // 复杂对象调用
            User user = new User();
            user.setId((long) i);
            user.setName("gRPC-User-" + i);
            user.setAge(30 + i);
            
            User savedUser = service.saveUser(user);
            logger.info("saveUser result: {}", savedUser);
            
            Thread.sleep(1000);
        }
        
        logger.info("");
        logger.info("========================================");
        logger.info("All tests completed successfully!");
        logger.info("========================================");
    }
}
