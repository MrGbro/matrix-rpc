package io.homeey.matrix.rpc.common;

/**
 *
 * @author jt4mrg@gmail.com
 * @since 2026-01-03
 **/
public final class Result {

    /**
     * 成功时返回的值
     */
    private final Object value;
    /**
     * 异常时存储的异常对象
     */
    private final Throwable exception;

    public Result(Object value) {
        this.value = value;
        this.exception = null;
    }

    public Result(Throwable exception) {
        this.value = null;
        this.exception = exception;
    }

    public boolean hasException() {
        return exception != null;
    }

    public <T> T getValue(Class<T> type) {
        return type.cast(value);
    }

    public Object getValue() {
        return value;
    }

    public Throwable getException() {
        return exception;
    }


    public static Result success(Object value) {
        return new Result(value);
    }

    public static Result fail(Throwable exception) {
        return new Result(exception);
    }
}