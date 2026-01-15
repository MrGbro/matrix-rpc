package io.homeey.matrix.rpc.observability.micrometer;

import io.homeey.matrix.rpc.observability.api.ReportEvent;

import java.util.Collections;
import java.util.Map;

/**
 * Metrics 指标事件
 * <p>
 * 用于记录 RPC 调用的关键指标：
 * - 调用耗时（用于计算 P99、P95、P50）
 * - 成功/失败状态
 * - 调用方/服务方信息
 * - 方法名
 * - 标签（tags）：用于多维度聚合
 */
public class MetricsEvent extends ReportEvent {

    // 基本信息
    private final String serviceName;       // 服务名
    private final String methodName;        // 方法名
    private final String side;              // 调用端：consumer/provider
    private final long duration;            // 调用耗时（毫秒）
    private final boolean success;          // 是否成功
    private final String errorType;         // 错误类型（成功时为 null）

    // 扩展标签
    private final Map<String, String> tags; // 自定义标签

    private MetricsEvent(Builder builder) {
        super(Type.METRICS);
        this.serviceName = builder.serviceName;
        this.methodName = builder.methodName;
        this.side = builder.side;
        this.duration = builder.duration;
        this.success = builder.success;
        this.errorType = builder.errorType;
        this.tags = builder.tags != null ? Collections.unmodifiableMap(builder.tags) : Collections.emptyMap();
    }

    public String getServiceName() {
        return serviceName;
    }

    public String getMethodName() {
        return methodName;
    }

    public String getSide() {
        return side;
    }

    public long getDuration() {
        return duration;
    }

    public boolean isSuccess() {
        return success;
    }

    public String getErrorType() {
        return errorType;
    }

    public Map<String, String> getTags() {
        return tags;
    }

    @Override
    public int estimateSize() {
        int size = 100; // 基础对象大小
        size += (serviceName != null ? serviceName.length() * 2 : 0);
        size += (methodName != null ? methodName.length() * 2 : 0);
        size += (errorType != null ? errorType.length() * 2 : 0);
        size += tags.size() * 50; // 估算每个 tag 50 字节
        return size;
    }

    @Override
    public String toString() {
        return "MetricsEvent{" +
                "serviceName='" + serviceName + '\'' +
                ", methodName='" + methodName + '\'' +
                ", side='" + side + '\'' +
                ", duration=" + duration +
                ", success=" + success +
                ", errorType='" + errorType + '\'' +
                ", tags=" + tags +
                ", timestamp=" + timestamp +
                '}';
    }

    /**
     * Builder 模式构建 MetricsEvent
     */
    public static class Builder {
        private String serviceName;
        private String methodName;
        private String side;
        private long duration;
        private boolean success = true;
        private String errorType;
        private Map<String, String> tags;

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

        public Builder duration(long duration) {
            this.duration = duration;
            return this;
        }

        public Builder success(boolean success) {
            this.success = success;
            return this;
        }

        public Builder errorType(String errorType) {
            this.errorType = errorType;
            return this;
        }

        public Builder tags(Map<String, String> tags) {
            this.tags = tags;
            return this;
        }

        public MetricsEvent build() {
            if (serviceName == null || methodName == null || side == null) {
                throw new IllegalArgumentException("serviceName, methodName, side are required");
            }
            return new MetricsEvent(this);
        }
    }

    /**
     * 快捷创建 Builder
     */
    public static Builder builder() {
        return new Builder();
    }
}
