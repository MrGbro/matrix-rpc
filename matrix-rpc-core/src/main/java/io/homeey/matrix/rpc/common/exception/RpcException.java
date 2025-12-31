package io.homeey.matrix.rpc.common.exception;

/**
 * RPC异常
 */
public class RpcException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public static final int UNKNOWN_EXCEPTION = 0;
    public static final int NETWORK_EXCEPTION = 1;
    public static final int TIMEOUT_EXCEPTION = 2;
    public static final int SERIALIZATION_EXCEPTION = 3;
    public static final int NO_INVOKER_EXCEPTION = 4;

    private int code;

    public RpcException() {
        super();
    }

    public RpcException(String message) {
        super(message);
    }

    public RpcException(int code, String message) {
        super(message);
        this.code = code;
    }

    public RpcException(String message, Throwable cause) {
        super(message, cause);
    }

    public RpcException(int code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
    }

    public int getCode() {
        return code;
    }

    public boolean isTimeout() {
        return code == TIMEOUT_EXCEPTION;
    }

    public boolean isNetwork() {
        return code == NETWORK_EXCEPTION;
    }
}
