package io.homeey.matrix.rpc.example.consumer;

import io.homeey.matrix.rpc.common.Result;
import io.homeey.matrix.rpc.common.URL;
import io.homeey.matrix.rpc.core.Invocation;
import io.homeey.matrix.rpc.core.Invoker;
import io.homeey.matrix.rpc.core.SimpleInvocation;
import io.homeey.matrix.rpc.core.invoker.AbstractInvoker;
import io.homeey.matrix.rpc.example.api.EchoService;
import io.homeey.matrix.rpc.transport.api.TransportClient;
import io.homeey.matrix.rpc.transport.netty.client.NettyTransportClient;

public class ConsumerMain {
    public static void main(String[] args) throws Exception {
        System.out.println("========================================");
        System.out.println("Matrix RPC Consumer - Direct Connection");
        System.out.println("========================================");
        
        // 1. 创建直连客户端（无需注册中心）
        URL providerUrl = new URL("matrix", "localhost", 20880, EchoService.class.getName(), null);
        TransportClient client = new NettyTransportClient(providerUrl);
        client.connect();
        System.out.println("Connected to provider: " + providerUrl.getAddress());
        
        // 2. 创建Invoker
        Invoker<EchoService> invoker = new AbstractInvoker<EchoService>(EchoService.class) {
            @Override
            public Result invoke(Invocation invocation) {
                return client.send(invocation, 3000);
            }
        };

        // 3. 创建Invocation并调用
        Invocation invocation = new SimpleInvocation(
                EchoService.class.getName(),
                "echo",
                new Class[]{String.class},
                new Object[]{"Hello Matrix RPC!"}
        );

        System.out.println("\nCalling remote service...");
        Result result = invoker.invoke(invocation);
        
        if (result.hasException()) {
            System.err.println("RPC call failed: " + result.getException().getMessage());
        } else {
            System.out.println("RPC Result: " + result.getValue(String.class));
        }
        
        // 4. 关闭连接
        client.close();
        System.out.println("\n========================================");
        System.out.println("RPC call completed!");
        System.out.println("========================================");
    }
}