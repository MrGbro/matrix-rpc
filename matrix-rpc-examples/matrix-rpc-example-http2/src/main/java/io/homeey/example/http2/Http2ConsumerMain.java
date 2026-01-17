package io.homeey.example.http2;

import io.homeey.example.api.EchoService;
import io.homeey.example.api.User;
import io.homeey.matrix.rpc.runtime.RpcReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * HTTP/2 Consumer 独立测试
 * 
 * <p>用于测试 HTTP/2 Consumer 连接到已启动的 Provider
 * <p>需要先启动 {@link Http2ProviderMain}
 * 
 * @author Matrix RPC Team
 * @since 1.0.0
 */
public class Http2ConsumerMain {
    
    private static final Logger logger = LoggerFactory.getLogger(Http2ConsumerMain.class);
    
    public static void main(String[] args) throws Exception {
        logger.info("========================================");
        logger.info("  Matrix RPC - HTTP/2 Consumer");
        logger.info("========================================");
        logger.info("Connecting to HTTP/2 Provider at localhost:8080...");
        
        // 创建 Consumer
        EchoService service = RpcReference.create(EchoService.class)
            .protocol("http2")
            .address("localhost", 8080)
            .timeout(5000)
            .get();
        
        logger.info("HTTP/2 Consumer connected successfully!");
        logger.info("");
        
        // 测试调用
        for (int i = 1; i <= 10; i++) {
            logger.info("========== Call {} ==========", i);
            
            // 简单调用
            String echoResult = service.echo("Hello HTTP/2 - " + i);
            logger.info("echo result: {}", echoResult);
            
            // 复杂对象调用
            User user = new User();
            user.setId((long) i);
            user.setName("User-" + i);
            user.setAge(20 + i);
            
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
