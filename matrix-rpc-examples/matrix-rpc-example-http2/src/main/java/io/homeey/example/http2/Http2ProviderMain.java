package io.homeey.example.http2;

import io.homeey.example.api.EchoService;
import io.homeey.example.api.User;
import io.homeey.matrix.rpc.runtime.RpcService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * HTTP/2 Provider 独立启动
 * 
 * <p>仅启动 HTTP/2 Provider，用于：
 * <ul>
 *   <li>独立测试 HTTP/2 服务端</li>
 *   <li>使用标准 HTTP/2 客户端（如 curl、h2load）测试</li>
 *   <li>验证 HTTP/2 协议兼容性</li>
 * </ul>
 * 
 * <p>测试命令示例：
 * <pre>
 * # 使用 curl 测试（需要 HTTP/2 支持）
 * curl -v --http2-prior-knowledge \
 *   -H "matrix-service: io.homeey.example.api.EchoService" \
 *   -H "matrix-method: echo" \
 *   -H "matrix-version: 1.0.0" \
 *   -H "matrix-group: " \
 *   -d "binary-data" \
 *   http://localhost:8080/io.homeey.example.api.EchoService/echo
 * 
 * # 使用 h2load 压测
 * h2load -n 10000 -c 100 http://localhost:8080/EchoService/echo
 * </pre>
 * 
 * @author Matrix RPC Team
 * @since 1.0.0
 */
public class Http2ProviderMain {
    
    private static final Logger logger = LoggerFactory.getLogger(Http2ProviderMain.class);
    
    public static void main(String[] args) throws Exception {
        logger.info("========================================");
        logger.info("  Matrix RPC - HTTP/2 Provider");
        logger.info("========================================");
        logger.info("Starting HTTP/2 service on port 8080...");
        
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
        
        // 启动 HTTP/2 Provider
        RpcService.create(EchoService.class, impl)
            .protocol("http2")
            .host("0.0.0.0")
            .port(8080)
            .version("1.0.0")
            .group("default")
            .export()
            .await();
        
        logger.info("========================================");
        logger.info("  HTTP/2 Provider Started Successfully!");
        logger.info("========================================");
        logger.info("");
        logger.info("Service Information:");
        logger.info("  Protocol: HTTP/2");
        logger.info("  Address:  0.0.0.0:8080");
        logger.info("  Service:  io.homeey.example.api.EchoService");
        logger.info("  Version:  1.0.0");
        logger.info("  Group:    default");
        logger.info("");
        logger.info("Service Methods:");
        logger.info("  - String echo(String message)");
        logger.info("  - User getUser(Long id)");
        logger.info("  - User saveUser(User user)");
        logger.info("");
        logger.info("Test with standard HTTP/2 clients:");
        logger.info("  Matrix Consumer: RpcReference.create(EchoService.class)");
        logger.info("                     .protocol(\"http2\")");
        logger.info("                     .address(\"localhost\", 8080)");
        logger.info("                     .get()");
        logger.info("");
        logger.info("Press Ctrl+C to stop the server...");
        logger.info("========================================");
        
        // 保持服务运行
        Thread.sleep(Long.MAX_VALUE);
    }
}
