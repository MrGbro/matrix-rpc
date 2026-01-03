package io.homeey.matrix.rpc.transport.api;


import io.homeey.matrix.rpc.core.remoting.Request;
import io.homeey.matrix.rpc.core.remoting.ResponseFuture;

public interface ExchangeClient {

    /**
     * 发送请求并返回响应的 Future 对象
     *
     * @param request 要发送的请求对象
     * @return ResponseFuture 响应的 Future 对象，用于获取异步响应结果
     */
    ResponseFuture send(Request request);
}
