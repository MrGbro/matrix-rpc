package io.homeey.matrix.rpc.filter.builtin;

import io.homeey.matrix.rpc.core.Result;
import io.homeey.matrix.rpc.core.Invocation;
import io.homeey.matrix.rpc.core.Invoker;
import io.homeey.matrix.rpc.filter.Filter;
import io.homeey.matrix.rpc.spi.Activate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;

/**
 * 访问日志过滤器
 * <p>
 * 记录 RPC 调用的访问日志，包括：
 * - 服务名称
 * - 方法名称
 * - 参数类型
 * - 调用耗时
 * - 调用结果
 * <p>
 * 配置：
 * <pre>
 * -Dmatrix.filter.accesslog.enabled=true  # 开关（默认开启）
 * </pre>
 */
@Activate(group = {"PROVIDER"}, order = 100)
public class AccessLogFilter implements Filter {

    private static final Logger logger = LoggerFactory.getLogger(AccessLogFilter.class);
    private static final String FILTER_NAME = "accesslog";
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

    @Override
    public Result invoke(Invoker<?> invoker, Invocation invocation) {
        // 检查 Filter 是否启用
        if (!FilterConfig.isEnabled(FILTER_NAME)) {
            return invoker.invoke(invocation);
        }

        long startTime = System.currentTimeMillis();
        String serviceName = invocation.getServiceName();
        String methodName = invocation.methodName();
        Class<?>[] paramTypes = invocation.parameterTypes();

        try {
            Result result = invoker.invoke(invocation);
            long costTime = System.currentTimeMillis() - startTime;

            // 记录访问日志
            logAccess(serviceName, methodName, paramTypes, costTime, result.hasException());

            return result;
        } catch (Throwable t) {
            long costTime = System.currentTimeMillis() - startTime;
            logAccess(serviceName, methodName, paramTypes, costTime, true);
            throw t;
        }
    }

    private void logAccess(String serviceName, String methodName, Class<?>[] paramTypes,
                           long costTime, boolean hasError) {
        String timestamp = LocalDateTime.now().format(FORMATTER);
        String params = paramTypes == null ? "()" :
                "(" + String.join(", ", Arrays.stream(paramTypes).map(Class::getSimpleName).toList()) + ")";
        String status = hasError ? "FAILED" : "SUCCESS";

        logger.info("{} | {} | {}.{}{} | {}ms | {}",
                timestamp, status, serviceName, methodName, params, costTime, status);
    }
}
