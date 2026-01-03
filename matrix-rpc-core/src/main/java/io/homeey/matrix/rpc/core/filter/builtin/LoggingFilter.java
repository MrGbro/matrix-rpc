package io.homeey.matrix.rpc.core.filter.builtin;


import io.homeey.matrix.rpc.common.Result;
import io.homeey.matrix.rpc.core.Filter;
import io.homeey.matrix.rpc.core.Invocation;
import io.homeey.matrix.rpc.core.Invoker;
import io.homeey.matrix.rpc.spi.Activate;

@Activate()
public class LoggingFilter implements Filter {

    @Override
    public Result invoke(Invoker<?> invoker, Invocation invocation) {
        System.out.println(
                "[RPC] invoke " +
                        invocation.getServiceName() + "#" +
                        invocation.methodName()
        );
        return invoker.invoke(invocation);
    }
}
