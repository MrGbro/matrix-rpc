package io.homeey.matrix.rpc.example.consumer;

import io.homeey.matrix.rpc.common.Result;
import io.homeey.matrix.rpc.common.URL;
import io.homeey.matrix.rpc.core.Invocation;
import io.homeey.matrix.rpc.core.Invoker;
import io.homeey.matrix.rpc.core.Protocol;
import io.homeey.matrix.rpc.example.api.EchoService;
import io.homeey.matrix.rpc.spi.ExtensionLoader;

import java.util.Map;

public class ConsumerMain {
    public static void main(String[] args) {
        // 1. 通过SPI获取Protocol
        Protocol protocol = ExtensionLoader.getExtensionLoader(Protocol.class)
                .getExtension("matrix");

        // 2. 引用远程服务
        URL url = new URL("matrix", "localhost", 20880, EchoService.class.getName(), null);
        Invoker<EchoService> invoker = protocol.refer(EchoService.class, url);

        // 3. 创建Invocation (简化)
        Invocation invocation = new SimpleInvocation(
                "echo",
                new Class[]{String.class},
                new Object[]{"Hello Matrix RPC!"}
        );

        // 4. 调用 (Phase 1 需手动构造请求)
        Result result = invoker.invoke(invocation);
        System.out.println("Result: " + result.getValue(String.class));
    }

    // 简化版Invocation
        private record SimpleInvocation(String methodName, Class<?>[] parameterTypes,
                                        Object[] arguments) implements Invocation {

        @Override
            public String getServiceName() {
                return EchoService.class.getName();
            }

            @Override
            public Map<String, String> getAttachments() {
                return Map.of();
            }
        }
}