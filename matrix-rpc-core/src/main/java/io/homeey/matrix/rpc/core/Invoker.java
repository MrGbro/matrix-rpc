package io.homeey.matrix.rpc.core;

import io.homeey.matrix.rpc.common.Result;

public interface Invoker<T> {
    Class<T> getInterface();
    Result invoke(Invocation invocation);
}