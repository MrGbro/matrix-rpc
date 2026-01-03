package io.homeey.matrix.rpc.core.filter.builtin;


import io.homeey.matrix.rpc.common.Result;
import io.homeey.matrix.rpc.core.Filter;
import io.homeey.matrix.rpc.core.Invocation;
import io.homeey.matrix.rpc.core.Invoker;

public class ExceptionFilter implements Filter {

    @Override
    public Result invoke(Invoker<?> invoker, Invocation invocation) {
        try {
            return invoker.invoke(invocation);
        } catch (Throwable t) {
            System.err.println(
                    "[RPC] exception at " +
                            invocation.methodName() + ": " +
                            t.getMessage()
            );
            throw t;
        }
    }
}
