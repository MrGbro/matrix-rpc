package io.homeey.matrix.rpc.core.remoting;

import java.io.Serial;
import java.io.Serializable;

/**
 *
 * @author jt4mrg@gmail.com
 * @since 2026-01-02
 **/
public class Response implements Serializable {

    @Serial
    private static final long serialVersionUID = 7652137767497317828L;
    /**
     * 请求ID
     */
    private long requestId;

    /**
     * 响应值
     */
    private Object value;

    /**
     * 异常信息
     */
    private Throwable exception;

    public boolean hasException() {
        return exception != null;
    }

    public long getRequestId() {
        return requestId;
    }

    public void setRequestId(long requestId) {
        this.requestId = requestId;
    }

    public Object getValue() {
        return value;
    }

    public void setValue(Object value) {
        this.value = value;
    }

    public Throwable getException() {
        return exception;
    }

    public void setException(Throwable exception) {
        this.exception = exception;
    }
}
