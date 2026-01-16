package io.homeey.matrix.rpc.observability.logging;

import io.homeey.matrix.rpc.observability.api.AsyncReporter;
import io.homeey.matrix.rpc.observability.async.DefaultAsyncReporter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.util.HashMap;
import java.util.Map;

/**
 * 结构化日志追加器
 * <p>
 * 核心职责：
 * 1. 消费 LogEvent 事件
 * 2. 将事件转换为结构化日志（JSON）
 * 3. 通过 SLF4J/Logback 输出日志
 * 4. 支持 MDC（Mapped Diagnostic Context）上下文传递
 * <p>
 * 设计要点：
 * - 使用 MDC 传递上下文数据（traceId、spanId 等）
 * - 支持 Logstash Encoder 输出 JSON 格式
 * - 异步非阻塞日志输出
 */
public class StructuredLogAppender implements AutoCloseable {
    private static final Logger logger = LoggerFactory.getLogger("RPC_ACCESS_LOG");

    // 异步上报器
    private final AsyncReporter<LogEvent> reporter;

    // 是否启用 MDC
    private final boolean mdcEnabled;

    /**
     * 构造函数
     */
    public StructuredLogAppender() {
        this(10000, 2, true);
    }

    /**
     * 构造函数（可配置队列容量和线程数）
     *
     * @param queueCapacity   队列容量
     * @param consumerThreads 消费线程数
     * @param mdcEnabled      是否启用 MDC
     */
    public StructuredLogAppender(int queueCapacity, int consumerThreads, boolean mdcEnabled) {
        this.mdcEnabled = mdcEnabled;

        // 创建异步上报器，事件处理器为 writeLog
        this.reporter = new DefaultAsyncReporter<>(
                this::writeLog,
                queueCapacity,
                consumerThreads
        );

        logger.info("StructuredLogAppender initialized: queueCapacity={}, consumerThreads={}, mdcEnabled={}",
                queueCapacity, consumerThreads, mdcEnabled);
    }

    /**
     * 上报日志事件（非阻塞）
     *
     * @param event LogEvent
     * @return true=成功入队, false=队列满（已丢弃）
     */
    public boolean report(LogEvent event) {
        return reporter.report(event);
    }

    /**
     * 写入结构化日志（由异步线程调用）
     *
     * @param event LogEvent
     */
    private void writeLog(LogEvent event) {
        try {
            // 1. 设置 MDC 上下文（如果启用）
            Map<String, String> previousMdc = null;
            if (mdcEnabled) {
                previousMdc = MDC.getCopyOfContextMap();
                setupMDC(event);
            }

            try {
                // 2. 构建日志消息
                String logMessage = buildLogMessage(event);

                // 3. 根据成功/失败状态输出日志
                if (event.isSuccess()) {
                    logger.info(logMessage);
                } else {
                    logger.error(logMessage);
                }

            } finally {
                // 4. 清理 MDC（恢复之前的上下文）
                if (mdcEnabled) {
                    MDC.clear();
                    if (previousMdc != null) {
                        MDC.setContextMap(previousMdc);
                    }
                }
            }

        } catch (Exception e) {
            logger.error("Failed to write log for event: {}", event, e);
            // 不抛异常，避免影响异步上报器
        }
    }

    /**
     * 设置 MDC 上下文
     */
    private void setupMDC(LogEvent event) {
        // 设置核心字段
        if (event.getTraceId() != null) {
            MDC.put("traceId", event.getTraceId());
        }
        if (event.getSpanId() != null) {
            MDC.put("spanId", event.getSpanId());
        }
        MDC.put("serviceName", event.getServiceName());
        MDC.put("methodName", event.getMethodName());
        MDC.put("side", event.getSide());
        MDC.put("duration", String.valueOf(event.getDuration()));
        MDC.put("success", String.valueOf(event.isSuccess()));

        // 设置网络信息
        if (event.getRemoteAddress() != null) {
            MDC.put("remoteAddress", event.getRemoteAddress());
        }
        if (event.getLocalAddress() != null) {
            MDC.put("localAddress", event.getLocalAddress());
        }

        // 设置错误信息
        if (!event.isSuccess()) {
            if (event.getErrorType() != null) {
                MDC.put("errorType", event.getErrorType());
            }
            if (event.getErrorMessage() != null) {
                MDC.put("errorMessage", event.getErrorMessage());
            }
        }

        // 设置自定义上下文数据
        if (event.getContextData() != null) {
            event.getContextData().forEach(MDC::put);
        }
    }

    /**
     * 构建日志消息（纯文本格式，JSON 由 Logstash Encoder 生成）
     */
    private String buildLogMessage(LogEvent event) {
        StringBuilder sb = new StringBuilder();

        // 基本信息
        sb.append("RPC ");
        sb.append(event.getSide().toUpperCase());
        sb.append(" - ");
        sb.append(event.getServiceName());
        sb.append(".");
        sb.append(event.getMethodName());

        // 耗时
        sb.append(" [");
        sb.append(event.getDuration());
        sb.append("ms]");

        // 状态
        if (event.isSuccess()) {
            sb.append(" SUCCESS");
        } else {
            sb.append(" FAILED");
            if (event.getErrorType() != null) {
                sb.append(" (");
                sb.append(event.getErrorType());
                sb.append(")");
            }
        }

        // 网络信息
        if (event.getRemoteAddress() != null) {
            sb.append(" remote=");
            sb.append(event.getRemoteAddress());
        }

        return sb.toString();
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
        logger.info("Shutting down StructuredLogAppender...");
        reporter.close();
        logger.info("StructuredLogAppender shutdown completed. Reported: {}, Dropped: {}",
                getReportedCount(), getDroppedCount());
    }
}
