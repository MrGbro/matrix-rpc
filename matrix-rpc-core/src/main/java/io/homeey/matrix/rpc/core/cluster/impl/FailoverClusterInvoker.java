package io.homeey.matrix.rpc.core.cluster.impl;


import io.homeey.matrix.rpc.common.Result;
import io.homeey.matrix.rpc.core.Invocation;
import io.homeey.matrix.rpc.core.Invoker;
import io.homeey.matrix.rpc.core.cluster.ClusterInvoker;
import io.homeey.matrix.rpc.core.cluster.Directory;
import io.homeey.matrix.rpc.core.cluster.InvokerSelector;
import io.homeey.matrix.rpc.core.cluster.RetryPolicy;

public class FailoverClusterInvoker<T> implements ClusterInvoker<T> {

    private final Directory<T> directory;
    private final InvokerSelector selector;
    private final RetryPolicy retryPolicy;

    public FailoverClusterInvoker(
            Directory<T> directory,
            InvokerSelector selector,
            RetryPolicy retryPolicy
    ) {
        this.directory = directory;
        this.selector = selector;
        this.retryPolicy = retryPolicy;
    }

    @Override
    public Class<T> getInterface() {
        return directory.getInterface();
    }

    @Override
    public Result invoke(Invocation invocation) {
        int retryCount = 0;
        Throwable lastException = null;

        while (true) {
            try {
                Invoker<T> invoker = selector.select(directory.list(invocation), invocation);
                return invoker.invoke(invocation);
            } catch (Throwable e) {
                lastException = e;
                retryCount++;

                if (!retryPolicy.shouldRetry(retryCount, e)) {
                    throw new RuntimeException(
                            "Failover invoke failed after "
                                    + retryCount + " retries", e);
                }

                try {
                    Thread.sleep(retryPolicy.retryIntervalMs(retryCount));
                } catch (InterruptedException ignored) {
                }
            }
        }
    }
}
