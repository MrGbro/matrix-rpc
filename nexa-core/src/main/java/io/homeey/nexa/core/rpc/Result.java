package io.homeey.nexa.core.rpc;

import lombok.Data;

import java.io.Serializable;

/**
 *
 * @author jt4mrg@gmail.com
 * @since 2025-12-24 22:39
 **/
@Data
public class Result implements Serializable {
    private static final long serialVersionUID = -4631984266706538755L;
    private Object value;
    private Throwable exception;

    public Result() {
    }

    public Result(Object value) {
        this.value = value;
    }

    public Object recreate() throws Throwable {
        if (exception != null) {
            throw exception;
        }
        return value;
    }
}
