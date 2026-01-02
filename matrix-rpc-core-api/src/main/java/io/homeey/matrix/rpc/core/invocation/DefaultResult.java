package io.homeey.matrix.rpc.core.invocation;

public class DefaultResult implements Result {

    private final Object value;
    private final Throwable exception;

    public DefaultResult(Object value) {
        this.value = value;
        this.exception = null;
    }

    public DefaultResult(Throwable exception) {
        this.value = null;
        this.exception = exception;
    }

    @Override
    public Object value() {
        return value;
    }

    @Override
    public Throwable exception() {
        return exception;
    }
}
