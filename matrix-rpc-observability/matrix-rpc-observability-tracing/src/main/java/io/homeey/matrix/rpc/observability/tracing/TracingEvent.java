package io.homeey.matrix.rpc.observability.tracing;

import io.homeey.matrix.rpc.observability.api.ReportEvent;

import java.util.Collections;
import java.util.Map;

/**
 * Tracing 追踪事件
 * <p>
 * 用于记录分布式链路追踪信息：
 * - Trace ID：全局唯一的追踪标识
 * - Span ID：当前调用的唯一标识
 * - Parent Span ID：父调用的标识
 * - 服务名、方法名
 * - 开始时间、结束时间、耗时
 * - 调用端（consumer/provider）
 * - 状态码、错误信息
 * - 自定义标签和属性
 */
public class TracingEvent extends ReportEvent {

    // 追踪标识
    private final String traceId;           // Trace ID（全局唯一）
    private final String spanId;            // Span ID（当前调用）
    private final String parentSpanId;      // Parent Span ID（父调用）

    // 基本信息
    private final String serviceName;       // 服务名
    private final String methodName;        // 方法名
    private final String side;              // 调用端：consumer/provider
    
    // 时间信息
    private final long startTime;           // 开始时间（纳秒）
    private final long endTime;             // 结束时间（纳秒）
    private final long duration;            // 耗时（毫秒）

    // 状态信息
    private final int statusCode;           // 状态码（0=OK, 1=ERROR）
    private final String errorMessage;      // 错误信息

    // 扩展信息
    private final Map<String, String> tags;       // 标签
    private final Map<String, String> attributes; // 属性

    private TracingEvent(Builder builder) {
        super(Type.TRACING);
        this.traceId = builder.traceId;
        this.spanId = builder.spanId;
        this.parentSpanId = builder.parentSpanId;
        this.serviceName = builder.serviceName;
        this.methodName = builder.methodName;
        this.side = builder.side;
        this.startTime = builder.startTime;
        this.endTime = builder.endTime;
        this.duration = builder.duration;
        this.statusCode = builder.statusCode;
        this.errorMessage = builder.errorMessage;
        this.tags = builder.tags != null ? Collections.unmodifiableMap(builder.tags) : Collections.emptyMap();
        this.attributes = builder.attributes != null ? Collections.unmodifiableMap(builder.attributes) : Collections.emptyMap();
    }

    // Getters
    public String getTraceId() { return traceId; }
    public String getSpanId() { return spanId; }
    public String getParentSpanId() { return parentSpanId; }
    public String getServiceName() { return serviceName; }
    public String getMethodName() { return methodName; }
    public String getSide() { return side; }
    public long getStartTime() { return startTime; }
    public long getEndTime() { return endTime; }
    public long getDuration() { return duration; }
    public int getStatusCode() { return statusCode; }
    public String getErrorMessage() { return errorMessage; }
    public Map<String, String> getTags() { return tags; }
    public Map<String, String> getAttributes() { return attributes; }

    @Override
    public int estimateSize() {
        int size = 200; // 基础对象大小
        size += (traceId != null ? traceId.length() * 2 : 0);
        size += (spanId != null ? spanId.length() * 2 : 0);
        size += (parentSpanId != null ? parentSpanId.length() * 2 : 0);
        size += (serviceName != null ? serviceName.length() * 2 : 0);
        size += (methodName != null ? methodName.length() * 2 : 0);
        size += (errorMessage != null ? errorMessage.length() * 2 : 0);
        size += tags.size() * 50;
        size += attributes.size() * 50;
        return size;
    }

    @Override
    public String toString() {
        return "TracingEvent{" +
                "traceId='" + traceId + '\'' +
                ", spanId='" + spanId + '\'' +
                ", parentSpanId='" + parentSpanId + '\'' +
                ", serviceName='" + serviceName + '\'' +
                ", methodName='" + methodName + '\'' +
                ", side='" + side + '\'' +
                ", duration=" + duration +
                ", statusCode=" + statusCode +
                ", timestamp=" + timestamp +
                '}';
    }

    /**
     * Builder 模式构建 TracingEvent
     */
    public static class Builder {
        private String traceId;
        private String spanId;
        private String parentSpanId;
        private String serviceName;
        private String methodName;
        private String side;
        private long startTime;
        private long endTime;
        private long duration;
        private int statusCode = 0; // 默认 OK
        private String errorMessage;
        private Map<String, String> tags;
        private Map<String, String> attributes;

        public Builder traceId(String traceId) {
            this.traceId = traceId;
            return this;
        }

        public Builder spanId(String spanId) {
            this.spanId = spanId;
            return this;
        }

        public Builder parentSpanId(String parentSpanId) {
            this.parentSpanId = parentSpanId;
            return this;
        }

        public Builder serviceName(String serviceName) {
            this.serviceName = serviceName;
            return this;
        }

        public Builder methodName(String methodName) {
            this.methodName = methodName;
            return this;
        }

        public Builder side(String side) {
            this.side = side;
            return this;
        }

        public Builder startTime(long startTime) {
            this.startTime = startTime;
            return this;
        }

        public Builder endTime(long endTime) {
            this.endTime = endTime;
            return this;
        }

        public Builder duration(long duration) {
            this.duration = duration;
            return this;
        }

        public Builder statusCode(int statusCode) {
            this.statusCode = statusCode;
            return this;
        }

        public Builder errorMessage(String errorMessage) {
            this.errorMessage = errorMessage;
            return this;
        }

        public Builder tags(Map<String, String> tags) {
            this.tags = tags;
            return this;
        }

        public Builder attributes(Map<String, String> attributes) {
            this.attributes = attributes;
            return this;
        }

        public TracingEvent build() {
            if (traceId == null || spanId == null || serviceName == null || methodName == null || side == null) {
                throw new IllegalArgumentException("traceId, spanId, serviceName, methodName, side are required");
            }
            return new TracingEvent(this);
        }
    }

    /**
     * 快捷创建 Builder
     */
    public static Builder builder() {
        return new Builder();
    }
}
