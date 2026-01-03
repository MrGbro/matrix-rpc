package io.homeey.matrix.rpc.transport.netty.client;


import io.homeey.matrix.rpc.core.remoting.Response;
import io.homeey.matrix.rpc.core.remoting.ResponseFuture;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 *
 * @author jt4mrg@gmail.com
 * @since 2026-01-02
 **/
public class DefaultResponseFuture implements ResponseFuture {

    /**
     * 存储所有未完成的响应Future，以请求ID为键
     */
    private static final Map<Long, DefaultResponseFuture> FUTURES =
            new ConcurrentHashMap<>();

    /**
     * 当前Future对应的请求ID
     */
    private final long requestId;
    
    /**
     * 用于等待响应到达的计数器
     */
    private final CountDownLatch latch = new CountDownLatch(1);

    /**
     * 响应结果，使用volatile保证可见性
     */
    private volatile Response response;

    public DefaultResponseFuture(long requestId) {
        this.requestId = requestId;
        FUTURES.put(requestId, this);
    }

    public static void received(Response response) {
        DefaultResponseFuture future =
                FUTURES.remove(response.getRequestId());
        if (future != null) {
            future.doReceived(response);
        }
    }

    private void doReceived(Response response) {
        this.response = response;
        latch.countDown();
    }

    @Override
    public Object get() throws Exception {
        latch.await();
        return returnValue();
    }

    @Override
    public Object get(long timeout, TimeUnit unit) throws Exception {
        if (!latch.await(timeout, unit)) {
            throw new TimeoutException("rpc timeout");
        }
        return returnValue();
    }

    private Object returnValue() throws Exception {
        if (response.hasException()) {
            throw new RuntimeException(response.getException());
        }
        return response.getValue();
    }

    @Override
    public boolean isDone() {
        return latch.getCount() == 0;
    }
}
