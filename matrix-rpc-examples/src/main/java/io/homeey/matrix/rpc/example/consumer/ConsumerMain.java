package io.homeey.matrix.rpc.example.consumer;

import io.homeey.matrix.rpc.example.api.EchoService;
import io.homeey.matrix.rpc.example.api.User;
import io.homeey.matrix.rpc.runtime.RpcReference;

public class ConsumerMain {
    public static void main(String[] args) {
        System.out.println("========================================");
        System.out.println("Matrix RPC Consumer - Direct Connect Mode");
        System.out.println("========================================");

        // 示例 1：直连模式（绕过注册中心，直接连接 Provider）
        System.out.println("\n--- Example 1: Direct Connect Mode ---");
        EchoService echoService1 = RpcReference.create(EchoService.class)
                .address("localhost", 20880) // 启用直连模式
                .get();
        String result1 = echoService1.echo("Hello Matrix RPC!");
        System.out.println("Result: " + result1);

        // 示例 2：直连模式 + 自定义超时
        System.out.println("\n--- Example 2: Direct Connect with Custom Timeout ---");
        EchoService echoService2 = RpcReference.create(EchoService.class)
                .address("localhost", 20880)
                .timeout(5000)  // 5秒超时
                .get();
        String result2 = echoService2.echo("Custom Timeout Test");
        System.out.println("Result: " + result2);

        // 示例 3：复杂对象测试
        System.out.println("\n--- Example 3: Complex Object Test ---");
        User user = echoService1.getUser(123L);
        System.out.println("Get User: " + user);

        User newUser = new User(null, "张三", "zhangsan@example.com", 30);
        User savedUser = echoService1.saveUser(newUser);
        System.out.println("Saved User: " + savedUser);

        System.out.println("\n========================================");
        System.out.println("All RPC calls completed successfully!");
        System.out.println("========================================");
    }
}