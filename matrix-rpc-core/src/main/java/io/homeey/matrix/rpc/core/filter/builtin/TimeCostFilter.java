package io.homeey.matrix.rpc.core.filter.builtin;


import io.homeey.matrix.rpc.core.filter.Filter;
import io.homeey.matrix.rpc.core.invocation.Invocation;
import io.homeey.matrix.rpc.core.invocation.Result;
import io.homeey.matrix.rpc.core.invoker.Invoker;
import io.homeey.matrix.rpc.spi.Activate;

@Activate(order = 50, scope = {"CONSUMER"})
public class TimeCostFilter implements Filter {

    @Override
    public Result invoke(Invoker<?> invoker, Invocation invocation) {
        long start = System.currentTimeMillis();
        try {
            return invoker.invoke(invocation);
        } finally {
            long cost = System.currentTimeMillis() - start;
            System.out.println(
                    "[RPC] cost=" + cost + "ms, method=" +
                            invocation.methodName()
            );
        }
    }
}
