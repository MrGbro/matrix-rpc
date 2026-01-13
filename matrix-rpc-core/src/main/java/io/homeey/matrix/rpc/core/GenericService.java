package io.homeey.matrix.rpc.core;

/**
 * 泛化调用接口 - 无需服务接口定义即可发起 RPC 调用
 * 
 * <p>适用场景：
 * <ul>
 *   <li>测试平台 - 动态测试各种 RPC 服务</li>
 *   <li>网关服务 - 代理转发 RPC 请求</li>
 *   <li>跨语言调用 - 其他语言客户端无法定义 Java 接口</li>
 *   <li>动态调用 - 运行时确定调用目标</li>
 * </ul>
 * 
 * <p>使用示例：
 * <pre>
 * GenericService service = RpcReference.builder()
 *     .interfaceClass(GenericService.class)
 *     .generic(true)
 *     .serviceName("io.homeey.example.api.EchoService")
 *     .directUrl("matrix://127.0.0.1:9090")
 *     .build()
 *     .get();
 * 
 * Object result = service.$invoke("echo", 
 *     new String[]{"java.lang.String"}, 
 *     new Object[]{"Hello"});
 * </pre>
 * 
 * @author Matrix RPC Team
 * @since 2026-01-14
 */
public interface GenericService {
    
    /**
     * 泛化调用方法
     * 
     * @param methodName 方法名
     * @param parameterTypes 参数类型数组（全限定类名），例如 ["java.lang.String", "int"]
     * @param args 参数值数组
     * @return 调用结果（基本类型会被包装为对应的包装类）
     * @throws RpcException 调用失败时抛出
     */
    Object $invoke(String methodName, String[] parameterTypes, Object[] args) throws RpcException;
}
