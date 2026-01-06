package io.homeey.matrix.rpc.example.provider;

import io.homeey.matrix.rpc.common.Result;
import io.homeey.matrix.rpc.common.URL;
import io.homeey.matrix.rpc.core.Invocation;
import io.homeey.matrix.rpc.core.Invoker;
import io.homeey.matrix.rpc.core.Protocol;
import io.homeey.matrix.rpc.core.invoker.AbstractInvoker;
import io.homeey.matrix.rpc.example.api.EchoService;
import io.homeey.matrix.rpc.spi.ExtensionLoader;

public class ProviderMain {
    public static void main(String[] args) throws InterruptedException {
        // 1. 创建服务实例
        EchoService echoService = new EchoServiceImpl();

        // 2. 创建Invoker - 将服务实例包装为Invoker
        Invoker<EchoService> invoker = new AbstractInvoker<EchoService>(EchoService.class) {
            @Override
            public Result invoke(Invocation invocation) {
                try {
                    String message = (String) invocation.arguments()[0];
                    String result = echoService.echo(message);
                    return new Result(result);
                } catch (Exception e) {
                    return new Result(e);
                }
            }
        };

        // 3. 通过SPI获取Protocol
        Protocol protocol = ExtensionLoader.getExtensionLoader(Protocol.class)
                .getExtension("matrix");

        // 4. 暴露服务
        URL url = new URL("matrix", "localhost", 20880, EchoService.class.getName(), null);
        protocol.export(invoker, url);

        System.out.println("========================================");
        System.out.println("Echo service exported at: " + url);
        System.out.println("Press Ctrl+C to stop the server...");
        System.out.println("========================================");
        
        // 保持服务运行
        Thread.currentThread().join();
    }
}