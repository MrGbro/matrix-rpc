package io.homeey.matrix.rpc.core;

import io.homeey.matrix.rpc.core.invocation.Result;

import java.io.Serializable;

/**
 *
 * @author jt4mrg@gmail.com
 * @since 2026-01-02
 **/
public record ExecuteResult(Object value, Throwable exception) implements Result, Serializable {

    public static ExecuteResult success(Object value) {
        return new ExecuteResult(value, null);
    }

    public static ExecuteResult fail(Throwable exception) {
        return new ExecuteResult(null, exception);
    }
}
