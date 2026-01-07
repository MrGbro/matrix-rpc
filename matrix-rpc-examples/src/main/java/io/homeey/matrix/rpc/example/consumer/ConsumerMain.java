package io.homeey.matrix.rpc.example.consumer;

import io.homeey.matrix.rpc.common.Result;
import io.homeey.matrix.rpc.common.URL;
import io.homeey.matrix.rpc.core.Invocation;
import io.homeey.matrix.rpc.core.Invoker;
import io.homeey.matrix.rpc.core.invoker.AbstractInvoker;
import io.homeey.matrix.rpc.example.api.EchoService;
import io.homeey.matrix.rpc.proxy.api.ProxyFactory;
import io.homeey.matrix.rpc.spi.ExtensionLoader;
import io.homeey.matrix.rpc.transport.api.TransportClient;
import io.homeey.matrix.rpc.transport.netty.client.NettyTransportClient;

public class ConsumerMain {
    public static void main(String[] args) throws Exception {
        System.out.println("========================================");
        System.out.println("Matrix RPC Consumer - With Dynamic Proxy");
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

        // 3. 通过 SPI 获取 ProxyFactory，创建代理对象
        ProxyFactory proxyFactory = ExtensionLoader.getExtensionLoader(ProxyFactory.class)
                .getExtension("jdk");
        EchoService echoService = proxyFactory.getProxy(invoker);
        
        System.out.println("\n--- Using Dynamic Proxy ---");
        
        // 4. 像调用本地方法一样调用远程服务
        String result = echoService.echo("Hello Matrix RPC with Proxy!");
        System.out.println("RPC Result: " + result);
        
        // 5. 关闭连接
        client.close();
        System.out.println("\n========================================");
        System.out.println("RPC call completed!");
        System.out.println("========================================");
    }
}