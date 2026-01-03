package io.homeey.matrix.rpc.common;

import java.io.Serializable;

/**
 *
 * @author jt4mrg@gmail.com
 * @since 2026-01-01
 **/
public class RpcException extends RuntimeException implements Serializable {
    private static final long serialVersionUID = -2919193633372849261L;

    public RpcException() {
    }

    public RpcException(String message) {
        super(message);
    }

    public RpcException(String message, Throwable cause) {
        super(message, cause);
    }
}
