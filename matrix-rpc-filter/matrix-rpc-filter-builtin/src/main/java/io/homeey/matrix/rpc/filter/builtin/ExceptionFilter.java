package io.homeey.matrix.rpc.filter.builtin;

import io.homeey.matrix.rpc.core.Result;
import io.homeey.matrix.rpc.core.RpcException;
import io.homeey.matrix.rpc.core.Invocation;
import io.homeey.matrix.rpc.core.Invoker;
import io.homeey.matrix.rpc.filter.Filter;
import io.homeey.matrix.rpc.spi.Activate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 异常处理过滤器
 * <p>
 * 在 Provider 端捕获并处理服务实现抛出的异常：
 * - 将检查异常包装为 RpcException
 * - 记录异常日志
 * - 保护服务端不会因异常而崩溃
 * <p>
 * 配置：
 * <pre>
 * -Dmatrix.filter.exception.enabled=true  # 开关（默认开启）
 * </pre>
 */
@Activate(group = {"PROVIDER"}, order = 200)
public class ExceptionFilter implements Filter {

    private static final Logger logger = LoggerFactory.getLogger(ExceptionFilter.class);
    private static final String FILTER_NAME = "exception";

    @Override
    public Result invoke(Invoker<?> invoker, Invocation invocation) {
        // 检查 Filter 是否启用
        if (!FilterConfig.isEnabled(FILTER_NAME)) {
            return invoker.invoke(invocation);
        }

        try {
            Result result = invoker.invoke(invocation);

            // 检查结果中是否包含异常
            if (result.hasException()) {
                Throwable exception = result.getException();
                // 记录异常日志
                logException(invocation, exception);
                // 包装异常
                return new Result(wrapException(invocation, exception));
            }

            return result;
        } catch (Throwable t) {
            // 捕获调用过程中的异常
            logException(invocation, t);
            return new Result(wrapException(invocation, t));
        }
    }

    private void logException(Invocation invocation, Throwable t) {
        logger.error("Service: {}, Method: {}, Exception: {} - {}",
                invocation.getServiceName(),
                invocation.methodName(),
                t.getClass().getSimpleName(),
                t.getMessage());
    }

    private Throwable wrapException(Invocation invocation, Throwable exception) {
        // 如果已经是 RpcException，直接返回
        if (exception instanceof RpcException) {
            return exception;
        }

        // 如果是运行时异常，直接返回
        if (exception instanceof RuntimeException) {
            return exception;
        }

        // 其他异常包装为 RpcException
        String message = String.format("Service [%s.%s] threw exception: %s",
                invocation.getServiceName(),
                invocation.methodName(),
                exception.getMessage());
        return new RpcException(message, exception);
    }
}
