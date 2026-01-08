package io.homeey.matrix.rpc.example.provider;

import io.homeey.matrix.rpc.example.api.EchoService;
import io.homeey.matrix.rpc.runtime.RpcService;

public class ProviderMain {
    public static void main(String[] args) {
        // 一行代码暴露服务！
        RpcService.export(EchoService.class, new EchoServiceImpl(), 20880).await();
    }
}