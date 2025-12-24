package io.homeey.nexa.core.rpc;

import lombok.Data;

import java.io.Serializable;
import java.util.concurrent.atomic.AtomicLong;

/**
 *
 * @author jt4mrg@gmail.com
 * @since 2025-12-24 23:03
 **/
@Data
public class Request implements Serializable {

    private static final long serialVersionUID = 1099720155479411281L;

    private static final AtomicLong REQUEST_SEQ = new AtomicLong(0L);

    /**
     * 请求ID
     */
    private Long requestId;
    /**
     * 是否双向通信
     */
    private boolean twoWay;
    /**
     * 是否心跳请求
     */
    private boolean heartbeat;
    /**
     * 调用信息
     */
    private Invocation invocation;

    public Request() {
        this.requestId = REQUEST_SEQ.getAndIncrement();
    }
}
