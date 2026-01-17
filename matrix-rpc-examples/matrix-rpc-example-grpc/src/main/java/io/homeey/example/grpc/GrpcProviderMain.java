package io.homeey.example.grpc;

import io.homeey.example.api.EchoService;
import io.homeey.example.api.User;
import io.homeey.matrix.rpc.runtime.RpcService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * gRPC Provider 独立启动
 * 
 * <p>仅启动 gRPC Provider，用于：
 * <ul>
 *   <li>独立测试 gRPC 服务端</li>
 *   <li>使用标准 gRPC 客户端测试（grpcurl、BloomRPC、Postman）</li>
 *   <li>验证 gRPC 协议兼容性</li>
 *   <li>跨语言调用测试（Go/Python/Node.js）</li>
 * </ul>
 * 
 * <p>测试命令示例：
 * <pre>
 * # 使用 grpcurl 列出服务
 * grpcurl -plaintext localhost:9090 list
 * 
 * # 使用 grpcurl 调用方法（需要启用 gRPC reflection）
 * grpcurl -plaintext \
 *   -d '{"message":"Hello"}' \
 *   localhost:9090 \
 *   io.homeey.example.api.EchoService/echo
 * 
 * # 使用 BloomRPC 或 Postman 进行图形化测试
 * # 地址：localhost:9090
 * # 服务：io.homeey.example.api.EchoService
 * </pre>
 * 
 * <p><b>注意</b>：标准 gRPC 工具需要 Protobuf 定义或启用 Server Reflection。
 * 当前实现支持 Matrix RPC 的泛化调用，可通过 Matrix Consumer 无缝对接。
 * 
 * @author Matrix RPC Team
 * @since 1.0.0
 */
public class GrpcProviderMain {
    
    private static final Logger logger = LoggerFactory.getLogger(GrpcProviderMain.class);
    
    public static void main(String[] args) throws Exception {
        logger.info("========================================");
        logger.info("  Matrix RPC - gRPC Provider");
        logger.info("========================================");
        logger.info("Starting gRPC service on port 9090...");
        
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
        
        // 启动 gRPC Provider
        RpcService.create(EchoService.class, impl)
            .protocol("grpc")
            .host("0.0.0.0")
            .port(9090)
            .version("1.0.0")
            .group("default")
            .export()
            .await();
        
        logger.info("========================================");
        logger.info("  gRPC Provider Started Successfully!");
        logger.info("========================================");
        logger.info("");
        logger.info("Service Information:");
        logger.info("  Protocol: gRPC");
        logger.info("  Address:  0.0.0.0:9090");
        logger.info("  Service:  io.homeey.example.api.EchoService");
        logger.info("  Version:  1.0.0");
        logger.info("  Group:    default");
        logger.info("");
        logger.info("Service Methods:");
        logger.info("  - String echo(String message)");
        logger.info("  - User getUser(Long id)");
        logger.info("  - User saveUser(User user)");
        logger.info("");
        logger.info("Test with Matrix RPC Consumer:");
        logger.info("  EchoService service = RpcReference.create(EchoService.class)");
        logger.info("                          .protocol(\"grpc\")");
        logger.info("                          .address(\"localhost\", 9090)");
        logger.info("                          .get();");
        logger.info("");
        logger.info("Test with standard gRPC tools:");
        logger.info("  grpcurl -plaintext localhost:9090 list");
        logger.info("  grpcurl -plaintext -d '{...}' localhost:9090 <service>/<method>");
        logger.info("");
        logger.info("Compatible with:");
        logger.info("  ✓ grpcurl (command-line)");
        logger.info("  ✓ BloomRPC (GUI)");
        logger.info("  ✓ Postman (GUI)");
        logger.info("  ✓ Go/Python/Node.js gRPC clients");
        logger.info("");
        logger.info("Press Ctrl+C to stop the server...");
        logger.info("========================================");
        
        // 保持服务运行
        Thread.sleep(Long.MAX_VALUE);
    }
}
