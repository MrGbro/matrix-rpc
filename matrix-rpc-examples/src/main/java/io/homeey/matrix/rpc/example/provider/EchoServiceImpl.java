package io.homeey.matrix.rpc.example.provider;

import io.homeey.matrix.rpc.example.api.EchoService;

public class EchoServiceImpl implements EchoService {

    @Override
    public String echo(String msg) {
        return "echo: " + msg;
    }
}
