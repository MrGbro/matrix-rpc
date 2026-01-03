package io.homeey.matrix.rpc.core.cluster.impl;


import io.homeey.matrix.rpc.core.cluster.RetryPolicy;

public class DefaultRetryPolicy implements RetryPolicy {

    private final int maxRetries;

    public DefaultRetryPolicy(int maxRetries) {
        this.maxRetries = maxRetries;
    }

    @Override
    public boolean shouldRetry(int retryCount, Throwable lastException) {
        return retryCount < maxRetries;
    }

    @Override
    public long retryIntervalMs(int retryCount) {
        // 指数退避
        return Math.min(1000L * (1L << retryCount), 5000L);
    }
}
