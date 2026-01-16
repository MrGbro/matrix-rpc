package io.homeey.matrix.rpc.observability.logging;

import io.homeey.matrix.rpc.core.Invocation;
import io.homeey.matrix.rpc.core.Invoker;
import io.homeey.matrix.rpc.core.Result;
import io.homeey.matrix.rpc.filter.Filter;
import io.homeey.matrix.rpc.spi.Activate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 结构化日志 Filter
 * <p>
 * 集成到 Matrix RPC 的 Filter 链，自动收集 RPC 调用日志：
 * - 请求/响应日志
 * - 调用耗时
 * - 错误信息
 * - 网络地址
 * <p>
 * 激活条件：
 * - order=120：在 MicrometerFilter 和 OpenTelemetryFilter 之后执行
 * - 默认激活（不需要额外配置）
 */
@Activate(order = 120)
public class LoggingFilter implements Filter {
    private static final Logger logger = LoggerFactory.getLogger(LoggingFilter.class);

    // 日志追加器（懒加载）
    private static volatile StructuredLogAppender appender = null;

    // 初始化锁
    private static final Object INIT_LOCK = new Object();

    // 是否启用（默认启用）
    private static volatile boolean enabled = true;

    /**
     * 设置是否启用日志（应用启动时调用）
     *
     * @param enabled 是否启用
     */
    public static void setEnabled(boolean enabled) {
        LoggingFilter.enabled = enabled;
        logger.info("LoggingFilter enabled: {}", enabled);
    }

    /**
     * 设置自定义日志追加器（应用启动时调用）
     *
     * @param customAppender 自定义追加器
     */
    public static void setAppender(StructuredLogAppender customAppender) {
        if (customAppender == null) {
            throw new IllegalArgumentException("Appender cannot be null");
        }
        synchronized (INIT_LOCK) {
            if (appender != null) {
                logger.info("Closing existing StructuredLogAppender...");
                appender.close();
            }
            appender = customAppender;
            logger.info("Custom StructuredLogAppender set");
        }
    }

    /**
     * 获取或创建日志追加器
     */
    private static StructuredLogAppender getOrCreateAppender() {
        if (appender == null) {
            synchronized (INIT_LOCK) {
                if (appender == null) {
                    appender = new StructuredLogAppender();
                    logger.info("StructuredLogAppender created");
                }
            }
        }
        return appender;
    }

    /**
     * 获取日志追加器（用于测试或监控）
     */
    public static StructuredLogAppender getAppender() {
        return getOrCreateAppender();
    }

    @Override
    public Result invoke(Invoker<?> invoker, Invocation invocation) {
        // 如果未启用，直接跳过
        if (!enabled) {
            return invoker.invoke(invocation);
        }

        long startTime = System.currentTimeMillis();
        Result result = null;
        Throwable exception = null;

        try {
            // 执行下一个 Filter 或 Invoker
            result = invoker.invoke(invocation);
            return result;

        } catch (Throwable e) {
            exception = e;
            throw e;

        } finally {
            long endTime = System.currentTimeMillis();
            long duration = endTime - startTime;

            // 提取服务和方法信息
            String serviceName = invocation.getServiceName();
            String methodName = invocation.methodName();
            String side = invocation.getAttachments().getOrDefault("side", "unknown");

            // 提取网络地址信息
            String remoteAddress = invocation.getAttachments().get("remoteAddress");
            String localAddress = invocation.getAttachments().get("localAddress");

            // 提取追踪信息（如果有）
            String traceId = invocation.getAttachments().get("traceId");
            String spanId = invocation.getAttachments().get("spanId");

            // 判断是否成功
            boolean success = (exception == null) &&
                    (result == null || !result.hasException());

            String errorMessage = null;
            String errorType = null;
            if (!success) {
                if (exception != null) {
                    errorMessage = exception.getMessage();
                    errorType = exception.getClass().getSimpleName();
                } else if (result != null && result.hasException()) {
                    Throwable resultException = result.getException();
                    errorMessage = resultException.getMessage();
                    errorType = resultException.getClass().getSimpleName();
                }
            }

            // 构建 LogEvent
            LogEvent event = LogEvent.builder()
                    .traceId(traceId)
                    .spanId(spanId)
                    .serviceName(serviceName)
                    .methodName(methodName)
                    .side(side)
                    .timestamp(startTime)
                    .duration(duration)
                    .success(success)
                    .errorMessage(errorMessage)
                    .errorType(errorType)
                    .remoteAddress(remoteAddress)
                    .localAddress(localAddress)
                    .build();

            // 异步上报（非阻塞）
            StructuredLogAppender logAppender = getOrCreateAppender();
            boolean reported = logAppender.report(event);

            if (!reported) {
                logger.debug("Log event dropped due to queue full: {}", event);
            }
        }
    }
}
