package io.homeey.matrix.rpc.observability.tracing;

import io.homeey.matrix.rpc.observability.api.AsyncReporter;
import io.homeey.matrix.rpc.observability.async.DefaultAsyncReporter;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

/**
 * OpenTelemetry 追踪收集器
 * <p>
 * 核心职责：
 * 1. 消费 TracingEvent 事件
 * 2. 将事件转换为 OpenTelemetry Span
 * 3. 支持分布式链路追踪（Trace ID、Span ID 传播）
 * 4. 自动设置 Span 属性和状态
 */
public class OpenTelemetryTracingCollector implements AutoCloseable {
    private static final Logger logger = LoggerFactory.getLogger(OpenTelemetryTracingCollector.class);

    // OpenTelemetry Tracer
    private final Tracer tracer;

    // 异步上报器
    private final AsyncReporter<TracingEvent> reporter;

    /**
     * 构造函数
     *
     * @param tracer OpenTelemetry Tracer
     */
    public OpenTelemetryTracingCollector(Tracer tracer) {
        this(tracer, 10000, 2);
    }

    /**
     * 构造函数（可配置队列容量和线程数）
     *
     * @param tracer          OpenTelemetry Tracer
     * @param queueCapacity   队列容量
     * @param consumerThreads 消费线程数
     */
    public OpenTelemetryTracingCollector(Tracer tracer, int queueCapacity, int consumerThreads) {
        if (tracer == null) {
            throw new IllegalArgumentException("Tracer cannot be null");
        }
        this.tracer = tracer;

        // 创建异步上报器，事件处理器为 recordSpan
        this.reporter = new DefaultAsyncReporter<>(
                this::recordSpan,
                queueCapacity,
                consumerThreads
        );

        logger.info("OpenTelemetryTracingCollector initialized: queueCapacity={}, consumerThreads={}",
                queueCapacity, consumerThreads);
    }

    /**
     * 上报追踪事件（非阻塞）
     *
     * @param event TracingEvent
     * @return true=成功入队, false=队列满（已丢弃）
     */
    public boolean report(TracingEvent event) {
        return reporter.report(event);
    }

    /**
     * 记录 Span 到 OpenTelemetry（由异步线程调用）
     *
     * @param event TracingEvent
     */
    private void recordSpan(TracingEvent event) {
        try {
            // 1. 创建 Span Builder
            String spanName = event.getServiceName() + "." + event.getMethodName();
            SpanKind spanKind = "consumer".equals(event.getSide()) ? SpanKind.CLIENT : SpanKind.SERVER;

            Span span = tracer.spanBuilder(spanName)
                    .setSpanKind(spanKind)
                    .setStartTimestamp(event.getStartTime(), TimeUnit.NANOSECONDS)
                    .startSpan();

            try {
                // 2. 设置基本属性
                span.setAttribute("rpc.service", event.getServiceName());
                span.setAttribute("rpc.method", event.getMethodName());
                span.setAttribute("rpc.side", event.getSide());

                // 3. 设置自定义标签
                if (event.getTags() != null) {
                    event.getTags().forEach(span::setAttribute);
                }

                // 4. 设置自定义属性
                if (event.getAttributes() != null) {
                    event.getAttributes().forEach(span::setAttribute);
                }

                // 5. 设置状态
                if (event.getStatusCode() == 0) {
                    span.setStatus(StatusCode.OK);
                } else {
                    span.setStatus(StatusCode.ERROR, event.getErrorMessage() != null ? 
                            event.getErrorMessage() : "RPC call failed");
                    if (event.getErrorMessage() != null) {
                        span.recordException(new RuntimeException(event.getErrorMessage()));
                    }
                }

            } finally {
                // 6. 结束 Span
                span.end(event.getEndTime(), TimeUnit.NANOSECONDS);
            }

        } catch (Exception e) {
            logger.error("Failed to record span for event: {}", event, e);
            // 不抛异常，避免影响异步上报器
        }
    }

    /**
     * 获取异步上报器统计信息
     */
    public long getReportedCount() {
        return reporter.getReportedCount();
    }

    public long getDroppedCount() {
        return reporter.getDroppedCount();
    }

    public int getRemainingCapacity() {
        return reporter.remainingCapacity();
    }

    /**
     * 优雅关闭
     */
    @Override
    public void close() {
        logger.info("Shutting down OpenTelemetryTracingCollector...");
        reporter.close();
        logger.info("OpenTelemetryTracingCollector shutdown completed. Reported: {}, Dropped: {}",
                getReportedCount(), getDroppedCount());
    }
}
