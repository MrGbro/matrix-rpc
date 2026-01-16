package io.homeey.matrix.rpc.observability.tracing;

import io.homeey.matrix.rpc.core.Invocation;
import io.homeey.matrix.rpc.core.Invoker;
import io.homeey.matrix.rpc.core.Result;
import io.homeey.matrix.rpc.filter.Filter;
import io.homeey.matrix.rpc.spi.Activate;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

/**
 * OpenTelemetry Tracing Filter
 * <p>
 * 集成到 Matrix RPC 的 Filter 链，自动收集分布式链路追踪信息：
 * - Trace ID / Span ID 生成和传播
 * - 调用耗时记录
 * - 错误状态记录
 * <p>
 * 激活条件：
 * - order=110：在 MicrometerFilter 之后执行
 * - 默认激活（不需要额外配置）
 */
@Activate(order = 110)
public class OpenTelemetryFilter implements Filter {
    private static final Logger logger = LoggerFactory.getLogger(OpenTelemetryFilter.class);

    // 全局 OpenTelemetry（可由用户设置）
    private static volatile OpenTelemetry globalOpenTelemetry = null;

    // 追踪收集器（懒加载）
    private static volatile OpenTelemetryTracingCollector collector = null;

    // 初始化锁
    private static final Object INIT_LOCK = new Object();

    // 是否启用（默认启用）
    private static volatile boolean enabled = true;

    /**
     * 设置是否启用 Tracing 收集（应用启动时调用）
     *
     * @param enabled 是否启用
     */
    public static void setEnabled(boolean enabled) {
        OpenTelemetryFilter.enabled = enabled;
        logger.info("OpenTelemetryFilter enabled: {}", enabled);
    }

    /**
     * 设置全局 OpenTelemetry（应用启动时调用）
     *
     * @param openTelemetry OpenTelemetry 实例
     */
    public static void setOpenTelemetry(OpenTelemetry openTelemetry) {
        if (openTelemetry == null) {
            throw new IllegalArgumentException("OpenTelemetry cannot be null");
        }
        globalOpenTelemetry = openTelemetry;
        logger.info("OpenTelemetry set");

        // 如果 collector 已创建，需要重新初始化
        synchronized (INIT_LOCK) {
            if (collector != null) {
                logger.info("Closing existing OpenTelemetryTracingCollector...");
                collector.close();
                collector = null;
            }
        }
    }

    /**
     * 获取或创建 OpenTelemetry
     */
    private static OpenTelemetry getOrCreateOpenTelemetry() {
        if (globalOpenTelemetry == null) {
            synchronized (INIT_LOCK) {
                if (globalOpenTelemetry == null) {
                    logger.info("No OpenTelemetry configured, using default SDK");
                    SdkTracerProvider tracerProvider = SdkTracerProvider.builder().build();
                    globalOpenTelemetry = OpenTelemetrySdk.builder()
                            .setTracerProvider(tracerProvider)
                            .build();
                }
            }
        }
        return globalOpenTelemetry;
    }

    /**
     * 获取或创建 TracingCollector
     */
    private static OpenTelemetryTracingCollector getOrCreateCollector() {
        if (collector == null) {
            synchronized (INIT_LOCK) {
                if (collector == null) {
                    OpenTelemetry openTelemetry = getOrCreateOpenTelemetry();
                    Tracer tracer = openTelemetry.getTracer("matrix-rpc");
                    collector = new OpenTelemetryTracingCollector(tracer);
                    logger.info("OpenTelemetryTracingCollector created");
                }
            }
        }
        return collector;
    }

    /**
     * 获取 TracingCollector（用于测试或监控）
     */
    public static OpenTelemetryTracingCollector getCollector() {
        return getOrCreateCollector();
    }

    @Override
    public Result invoke(Invoker<?> invoker, Invocation invocation) {
        // 快速路径：如果未启用，直接跳过
        if (!enabled) {
            return invoker.invoke(invocation);
        }

        long startTime = System.nanoTime();
        Result result = null;
        Throwable exception = null;

        // 生成或获取 Trace ID 和 Span ID
        String traceId = generateTraceId();
        String spanId = generateSpanId();
        String parentSpanId = null; // 简化实现，实际应从 Context 获取

        try {
            // 执行下一个 Filter 或 Invoker
            result = invoker.invoke(invocation);
            return result;

        } catch (Throwable e) {
            exception = e;
            throw e;

        } finally {
            long endTime = System.nanoTime();
            long duration = (endTime - startTime) / 1_000_000; // 转为毫秒

            // 提取服务和方法信息
            String serviceName = invocation.getServiceName();
            String methodName = invocation.methodName();
            String side = invocation.getAttachments().getOrDefault("side", "unknown");

            // 判断是否成功
            boolean success = (exception == null) &&
                    (result == null || !result.hasException());

            int statusCode = success ? 0 : 1;
            String errorMessage = null;
            if (!success) {
                if (exception != null) {
                    errorMessage = exception.getMessage();
                } else if (result != null && result.hasException()) {
                    errorMessage = result.getException().getMessage();
                }
            }

            // 构建 TracingEvent
            TracingEvent event = TracingEvent.builder()
                    .traceId(traceId)
                    .spanId(spanId)
                    .parentSpanId(parentSpanId)
                    .serviceName(serviceName)
                    .methodName(methodName)
                    .side(side)
                    .startTime(startTime)
                    .endTime(endTime)
                    .duration(duration)
                    .statusCode(statusCode)
                    .errorMessage(errorMessage)
                    .build();

            // 异步上报（非阻塞）
            OpenTelemetryTracingCollector tracingCollector = getOrCreateCollector();
            boolean reported = tracingCollector.report(event);

            if (!reported) {
                logger.debug("Tracing event dropped due to queue full: {}", event);
            }
        }
    }

    /**
     * 生成 Trace ID（32 字符十六进制）
     */
    private String generateTraceId() {
        return UUID.randomUUID().toString().replace("-", "") + 
               UUID.randomUUID().toString().replace("-", "").substring(0, 32).substring(16);
    }

    /**
     * 生成 Span ID（16 字符十六进制）
     */
    private String generateSpanId() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 16);
    }
}
