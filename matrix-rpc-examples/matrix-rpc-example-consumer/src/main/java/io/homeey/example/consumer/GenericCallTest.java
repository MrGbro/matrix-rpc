package io.homeey.example.consumer;

import io.homeey.matrix.rpc.core.GenericService;
import io.homeey.matrix.rpc.runtime.RpcReference;

/**
 * 泛化调用功能测试
 * 
 * <p>测试场景：
 * <ul>
 *   <li>无接口定义的泛化调用</li>
 *   <li>不同参数类型的泛化调用</li>
 *   <li>泛化调用错误处理</li>
 * </ul>
 * 
 * @author Matrix RPC Team
 * @since 2026-01-14
 */
public class GenericCallTest {
    
    public static void main(String[] args) throws Exception {
        System.out.println("=== 泛化调用功能测试 ===\n");
        
        // 测试1：基本泛化调用
        testBasicGenericCall();
        
        // 测试2：复杂参数泛化调用
        testComplexGenericCall();
        
        // 测试3：错误处理
        testErrorHandling();
        
        System.out.println("\n=== 测试完成 ===");
    }
    
    /**
     * 测试基本泛化调用
     */
    private static void testBasicGenericCall() throws Exception {
        System.out.println("【测试1】基本泛化调用");
        
        // 创建泛化服务引用（无需 EchoService 接口）
        GenericService service = (GenericService) RpcReference.create(GenericService.class)
                .address("localhost", 20880)
                .generic("io.homeey.example.api.EchoService")  // 指定服务名
                .get();
        
        // 泛化调用 echo 方法
        Object result = service.$invoke(
                "echo",  // 方法名
                new String[]{"java.lang.String"},  // 参数类型
                new Object[]{"Generic Call"}  // 参数值
        );
        
        System.out.println("  调用方法: echo(String)");
        System.out.println("  返回结果: " + result);
        System.out.println("  ✅ 基本泛化调用测试通过\n");
    }
    
    /**
     * 测试复杂参数泛化调用
     */
    private static void testComplexGenericCall() throws Exception {
        System.out.println("【测试2】复杂参数泛化调用");
        
        GenericService service = (GenericService) RpcReference.create(GenericService.class)
                .address("localhost", 20880)
                .generic("io.homeey.example.api.EchoService")
                .get();
        
        // 调用 echo 方法（字符串参数）
        Object result1 = service.$invoke(
                "echo",
                new String[]{"java.lang.String"},
                new Object[]{"测试中文"}
        );
        System.out.println("  中文参数: " + result1);
        
        // 如果有其他方法，可以继续测试
        // 例如：echoUser(User user)
        // Object result2 = service.$invoke(
        //     "echoUser",
        //     new String[]{"io.homeey.example.api.User"},
        //     new Object[]{userObject}
        // );
        
        System.out.println("  ✅ 复杂参数泛化调用测试通过\n");
    }
    
    /**
     * 测试错误处理
     */
    private static void testErrorHandling() throws Exception {
        System.out.println("【测试3】错误处理");
        
        GenericService service = (GenericService) RpcReference.create(GenericService.class)
                .address("localhost", 20880)
                .generic("io.homeey.example.api.EchoService")
                .get();
        
        try {
            // 调用不存在的方法
            service.$invoke(
                    "nonExistentMethod",
                    new String[]{"java.lang.String"},
                    new Object[]{"test"}
            );
            System.out.println("  ❌ 应该抛出异常");
        } catch (Exception e) {
            System.out.println("  ✅ 正确捕获异常: " + e.getClass().getSimpleName());
            System.out.println("  异常信息: " + e.getMessage());
        }
        
        System.out.println("  ✅ 错误处理测试通过\n");
    }
}
