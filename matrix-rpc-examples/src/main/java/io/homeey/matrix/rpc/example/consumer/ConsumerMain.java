package io.homeey.matrix.rpc.example.consumer;

import io.homeey.matrix.rpc.example.api.EchoService;
import io.homeey.matrix.rpc.example.api.User;
import io.homeey.matrix.rpc.runtime.RpcReference;

public class ConsumerMain {
    public static void main(String[] args) {
        System.out.println("========================================");
        System.out.println("Matrix RPC Consumer - Complex Object Support");
        System.out.println("========================================");
        
        // 一行代码获取远程服务代理！
        EchoService echoService = RpcReference.refer(EchoService.class, "localhost", 20880);
        
        // 1. 测试字符串传递
        System.out.println("\n--- Test 1: String Parameter ---");
        String result = echoService.echo("Hello Matrix RPC!");
        System.out.println("Result: " + result);
        
        // 2. 测试复杂对象返回值
        System.out.println("\n--- Test 2: Complex Object Return ---");
        User user = echoService.getUser(123L);
        System.out.println("Get User: " + user);
        
        // 3. 测试复杂对象作为参数和返回值
        System.out.println("\n--- Test 3: Complex Object as Parameter and Return ---");
        User newUser = new User(null, "张三", "zhangsan@example.com", 30);
        System.out.println("Input User: " + newUser);
        User savedUser = echoService.saveUser(newUser);
        System.out.println("Saved User: " + savedUser);
        
        System.out.println("\n========================================");
        System.out.println("All RPC calls completed successfully!");
        System.out.println("========================================");
    }
}