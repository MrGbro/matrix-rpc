package io.homeey.matrix.rpc.core.cluster;

public interface RetryPolicy {

    boolean shouldRetry(
            int retryCount,
            Throwable lastException
    );

    long retryIntervalMs(int retryCount);
}
