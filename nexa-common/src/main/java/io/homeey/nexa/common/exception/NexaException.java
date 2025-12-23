package io.homeey.nexa.common.exception;

/**
 *
 * @author jt4mrg@gmail.com
 * @version 0.0.1
 * @since 2025-12-23 23:51
 **/
public class NexaException extends RuntimeException {

    public NexaException() {
        super();
    }

    public NexaException(String message) {
        super(message);
    }

    public NexaException(String message, Throwable cause) {
        super(message, cause);
    }

    public NexaException(Throwable cause) {
        super(cause);
    }
}
