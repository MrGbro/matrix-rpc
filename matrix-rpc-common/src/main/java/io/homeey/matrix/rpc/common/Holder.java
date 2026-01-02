package io.homeey.matrix.rpc.common;

/**
 *
 * @author jt4mrg@gmail.com
 * @since 2025-12-31
 **/
public class Holder<T> {
    private volatile T value;

    public T get() {
        return value;
    }

    public void set(T value) {
        this.value = value;
    }
}
