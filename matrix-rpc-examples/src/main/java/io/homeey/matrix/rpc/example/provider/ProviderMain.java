package io.homeey.matrix.rpc.example.provider;

import io.homeey.matrix.rpc.common.Result;
import io.homeey.matrix.rpc.common.URL;
import io.homeey.matrix.rpc.core.Invocation;
import io.homeey.matrix.rpc.core.Invoker;
import io.homeey.matrix.rpc.core.Protocol;
import io.homeey.matrix.rpc.core.impl.AbstractInvoker;
import io.homeey.matrix.rpc.example.api.EchoService;
import io.homeey.matrix.rpc.spi.ExtensionLoader;

public class ProviderMain {
    public static void main(String[] args) {
        // 1. 创建服务实例
        EchoService echoService = new EchoServiceImpl();

        // 2. 创建Invoker
        Invoker<EchoService> invoker = new AbstractInvoker<EchoService>(EchoService.class) {
            @Override
            public Result invoke(Invocation invocation) {
                String message = (String) invocation.arguments()[0];
                return new Result(echoService.echo(message));
            }
        };

        // 3. 通过SPI获取Protocol
        Protocol protocol = ExtensionLoader.getExtensionLoader(Protocol.class)
                .getExtension("matrix");

        // 4. 暴露服务
        URL url = new URL("matrix", "localhost", 20880, EchoService.class.getName(), null);
        protocol.export(invoker, url);

        System.out.println("Echo service exported at: " + url);
    }
}