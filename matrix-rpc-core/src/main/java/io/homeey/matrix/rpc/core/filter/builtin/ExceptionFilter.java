package io.homeey.matrix.rpc.core.filter.builtin;


import io.homeey.matrix.rpc.core.filter.Filter;
import io.homeey.matrix.rpc.core.invocation.Invocation;
import io.homeey.matrix.rpc.core.invocation.Result;
import io.homeey.matrix.rpc.core.invoker.Invoker;

public class ExceptionFilter implements Filter {

    @Override
    public Result invoke(Invoker<?> invoker, Invocation invocation) {
        try {
            return invoker.invoke(invocation);
        } catch (Throwable t) {
            System.err.println(
                    "[RPC] exception at " +
                            invocation.serviceName() + ": " +
                            t.getMessage()
            );
            throw t;
        }
    }
}
