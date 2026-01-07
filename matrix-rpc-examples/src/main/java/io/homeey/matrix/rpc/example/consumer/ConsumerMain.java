package io.homeey.matrix.rpc.example.consumer;

import io.homeey.matrix.rpc.example.api.EchoService;
import io.homeey.matrix.rpc.runtime.RpcReference;

public class ConsumerMain {
    public static void main(String[] args) {
        System.out.println("========================================");
        System.out.println("Matrix RPC Consumer - Simplified API");
        System.out.println("========================================");
        
        // 一行代码获取远程服务代理！
        EchoService echoService = RpcReference.refer(EchoService.class, "localhost", 20880);
        
        System.out.println("\n--- RPC Call via Dynamic Proxy ---");
        
        // 像调用本地方法一样调用远程服务
        String result = echoService.echo("Hello Matrix RPC!");
        System.out.println("Result: " + result);
        
        System.out.println("\n========================================");
        System.out.println("RPC call completed!");
        System.out.println("========================================");
    }
}