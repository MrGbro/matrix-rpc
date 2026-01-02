package io.homeey.matrix.rpc.core.filter.builtin;


import io.homeey.matrix.rpc.core.filter.Filter;
import io.homeey.matrix.rpc.core.invocation.Invocation;
import io.homeey.matrix.rpc.core.invocation.Result;
import io.homeey.matrix.rpc.core.invoker.Invoker;
import io.homeey.matrix.rpc.spi.Activate;

@Activate()
public class LoggingFilter implements Filter {

    @Override
    public Result invoke(Invoker<?> invoker, Invocation invocation) {
        System.out.println(
                "[RPC] invoke " +
                        invocation.serviceName() + "#" +
                        invocation.methodName()
        );
        return invoker.invoke(invocation);
    }
}
