package io.homeey.nexa.common.exception;

/**
 *
 * @author jt4mrg@gmail.com
 * @version 0.0.1
 * @since 2025-12-23 23:53
 **/
public class RpcException extends NexaException {
    public RpcException() {
        super();
    }


    public RpcException(String message) {
        super(message);
    }

    public RpcException(String message, Throwable cause) {
        super(message, cause);
    }

    public RpcException(Throwable cause) {
        super(cause);
    }
}
