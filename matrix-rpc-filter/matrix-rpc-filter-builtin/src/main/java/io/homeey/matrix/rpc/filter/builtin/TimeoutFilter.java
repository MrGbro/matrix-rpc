package io.homeey.matrix.rpc.filter.builtin;

import io.homeey.matrix.rpc.core.Result;
import io.homeey.matrix.rpc.core.RpcException;
import io.homeey.matrix.rpc.core.Invocation;
import io.homeey.matrix.rpc.core.Invoker;
import io.homeey.matrix.rpc.filter.Filter;
import io.homeey.matrix.rpc.spi.Activate;

/**
 * 超时检测过滤器
 * <p>
 * 在 Consumer 端检测 RPC 调用是否超时，并记录警告日志。
 * 注意：实际的超时控制在 Transport 层实现，此 Filter 主要用于：
 * - 记录慢调用
 * - 统计超时比例
 * - 提供可观测性
 * <p>
 * 配置：
 * <pre>
 * -Dmatrix.filter.timeout.enabled=true       # 开关（默认开启）
 * -Dmatrix.filter.timeout.threshold=1000     # 慢调用阈值（毫秒，默认1000）
 * </pre>
 */
@Activate(group = {"CONSUMER"}, order = 100)
public class TimeoutFilter implements Filter {

    private static final String FILTER_NAME = "timeout";
    private static final long DEFAULT_SLOW_THRESHOLD = 1000; // 默认1秒

    @Override
    public Result invoke(Invoker<?> invoker, Invocation invocation) {
        // 检查 Filter 是否启用
        if (!FilterConfig.isEnabled(FILTER_NAME)) {
            return invoker.invoke(invocation);
        }

        long slowThreshold = FilterConfig.getLongConfig(FILTER_NAME, "threshold", DEFAULT_SLOW_THRESHOLD);
        long startTime = System.currentTimeMillis();

        try {
            Result result = invoker.invoke(invocation);
            long costTime = System.currentTimeMillis() - startTime;

            // 检测慢调用
            if (costTime > slowThreshold) {
                logSlowCall(invocation, costTime, slowThreshold);
            }

            return result;
        } catch (Throwable t) {
            long costTime = System.currentTimeMillis() - startTime;

            // 如果是超时异常，记录详细信息
            if (isTimeoutException(t)) {
                logTimeout(invocation, costTime);
            }

            throw t;
        }
    }

    private void logSlowCall(Invocation invocation, long costTime, long threshold) {
        System.out.printf("[TimeoutFilter] SLOW CALL - Service: %s, Method: %s, Cost: %dms (threshold: %dms)%n",
                invocation.getServiceName(),
                invocation.methodName(),
                costTime,
                threshold);
    }

    private void logTimeout(Invocation invocation, long costTime) {
        System.err.printf("[TimeoutFilter] TIMEOUT - Service: %s, Method: %s, Cost: %dms%n",
                invocation.getServiceName(),
                invocation.methodName(),
                costTime);
    }

    private boolean isTimeoutException(Throwable t) {
        // 检查是否是超时相关的异常
        if (t instanceof RpcException rpcEx) {
            String message = rpcEx.getMessage();
            return message != null && (message.contains("timeout") || message.contains("Timeout"));
        }
        return t instanceof java.util.concurrent.TimeoutException;
    }
}
