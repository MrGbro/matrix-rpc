package io.homeey.matrix.rpc.example.consumer;

import io.homeey.matrix.rpc.core.invocation.DefaultInvocation;
import io.homeey.matrix.rpc.core.invocation.Invocation;
import io.homeey.matrix.rpc.core.invocation.Result;
import io.homeey.matrix.rpc.core.invocation.ServiceInvoker;
import io.homeey.matrix.rpc.core.invoker.Invoker;
import io.homeey.matrix.rpc.core.protocol.Protocol;
import io.homeey.matrix.rpc.example.api.EchoService;
import io.homeey.matrix.rpc.example.provider.EchoServiceImpl;
import io.homeey.matrix.rpc.spi.ExtensionLoader;

public class LocalRpcDemo {
    public static void main(String[] args) {
        Protocol protocol =
                ExtensionLoader.getExtensionLoader(Protocol.class)
                        .getDefaultExtension();

        EchoService service = new EchoServiceImpl();

        Invoker<EchoService> serviceInvoker =
                new ServiceInvoker<>(service, EchoService.class);

        protocol.export(serviceInvoker);

        Invoker<EchoService> clientInvoker =
                protocol.refer(EchoService.class, null);

        Invocation invocation =
                new DefaultInvocation(
                        EchoService.class.getName(),
                        "echo",
                        new Class[]{String.class},
                        new Object[]{"hello matrix-rpc"}
                );

        Result result = clientInvoker.invoke(invocation);
        System.out.println(result.value());
    }
}
