package io.homeey.matrix.rpc.observability.logging;

import io.homeey.matrix.rpc.observability.api.ReportEvent;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * 结构化日志事件
 * <p>
 * 记录 RPC 调用的详细日志信息，支持结构化输出（JSON 格式）
 * <p>
 * 关键字段：
 * - traceId: 分布式追踪 ID
 * - spanId: 当前调用 Span ID
 * - serviceName: 服务名
 * - methodName: 方法名
 * - side: 调用端（consumer/provider）
 * - timestamp: 时间戳
 * - duration: 调用耗时（毫秒）
 * - success: 是否成功
 * - errorMessage: 错误信息
 * - remoteAddress: 远程地址
 * - localAddress: 本地地址
 * - contextData: 上下文数据（MDC）
 */
public class LogEvent extends ReportEvent {

    // 追踪标识
    private final String traceId;
    private final String spanId;

    // 基本信息
    private final String serviceName;
    private final String methodName;
    private final String side;             // consumer/provider

    // 时间信息
    private final long timestamp;          // 时间戳（毫秒）
    private final long duration;           // 耗时（毫秒）

    // 状态信息
    private final boolean success;
    private final String errorMessage;
    private final String errorType;        // 异常类型

    // 网络信息
    private final String remoteAddress;    // 远程地址
    private final String localAddress;     // 本地地址

    // 上下文数据（MDC）
    private final Map<String, String> contextData;

    // 扩展字段
    private final Map<String, Object> extraFields;

    private LogEvent(Builder builder) {
        super(Type.LOGGING);
        this.traceId = builder.traceId;
        this.spanId = builder.spanId;
        this.serviceName = builder.serviceName;
        this.methodName = builder.methodName;
        this.side = builder.side;
        this.timestamp = builder.timestamp;
        this.duration = builder.duration;
        this.success = builder.success;
        this.errorMessage = builder.errorMessage;
        this.errorType = builder.errorType;
        this.remoteAddress = builder.remoteAddress;
        this.localAddress = builder.localAddress;
        this.contextData = builder.contextData != null ?
                Collections.unmodifiableMap(builder.contextData) : Collections.emptyMap();
        this.extraFields = builder.extraFields != null ?
                Collections.unmodifiableMap(builder.extraFields) : Collections.emptyMap();
    }

    // Getters
    public String getTraceId() {
        return traceId;
    }

    public String getSpanId() {
        return spanId;
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

    public long getTimestamp() {
        return timestamp;
    }

    public long getDuration() {
        return duration;
    }

    public boolean isSuccess() {
        return success;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public String getErrorType() {
        return errorType;
    }

    public String getRemoteAddress() {
        return remoteAddress;
    }

    public String getLocalAddress() {
        return localAddress;
    }

    public Map<String, String> getContextData() {
        return contextData;
    }

    public Map<String, Object> getExtraFields() {
        return extraFields;
    }

    @Override
    public int estimateSize() {
        // 估算对象大小（字节）
        int size = 200; // 基础对象开销
        
        // 字符串字段
        if (traceId != null) size += traceId.length() * 2;
        if (spanId != null) size += spanId.length() * 2;
        if (serviceName != null) size += serviceName.length() * 2;
        if (methodName != null) size += methodName.length() * 2;
        if (side != null) size += side.length() * 2;
        if (errorMessage != null) size += errorMessage.length() * 2;
        if (errorType != null) size += errorType.length() * 2;
        if (remoteAddress != null) size += remoteAddress.length() * 2;
        if (localAddress != null) size += localAddress.length() * 2;
        
        // Map 开销
        if (contextData != null) {
            size += contextData.size() * 50; // 每个条目约50字节
        }
        if (extraFields != null) {
            size += extraFields.size() * 50;
        }
        
        return size;
    }

    @Override
    public String toString() {
        return "LogEvent{" +
                "traceId='" + traceId + '\'' +
                ", spanId='" + spanId + '\'' +
                ", serviceName='" + serviceName + '\'' +
                ", methodName='" + methodName + '\'' +
                ", side='" + side + '\'' +
                ", timestamp=" + timestamp +
                ", duration=" + duration +
                ", success=" + success +
                ", errorMessage='" + errorMessage + '\'' +
                ", errorType='" + errorType + '\'' +
                ", remoteAddress='" + remoteAddress + '\'' +
                ", localAddress='" + localAddress + '\'' +
                ", contextData=" + contextData +
                ", extraFields=" + extraFields +
                '}';
    }

    /**
     * 创建 Builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder 类
     */
    public static class Builder {
        private String traceId;
        private String spanId;
        private String serviceName;
        private String methodName;
        private String side;
        private long timestamp;
        private long duration;
        private boolean success = true;
        private String errorMessage;
        private String errorType;
        private String remoteAddress;
        private String localAddress;
        private Map<String, String> contextData;
        private Map<String, Object> extraFields;

        public Builder traceId(String traceId) {
            this.traceId = traceId;
            return this;
        }

        public Builder spanId(String spanId) {
            this.spanId = spanId;
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

        public Builder timestamp(long timestamp) {
            this.timestamp = timestamp;
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

        public Builder errorMessage(String errorMessage) {
            this.errorMessage = errorMessage;
            return this;
        }

        public Builder errorType(String errorType) {
            this.errorType = errorType;
            return this;
        }

        public Builder remoteAddress(String remoteAddress) {
            this.remoteAddress = remoteAddress;
            return this;
        }

        public Builder localAddress(String localAddress) {
            this.localAddress = localAddress;
            return this;
        }

        public Builder contextData(Map<String, String> contextData) {
            this.contextData = contextData;
            return this;
        }

        public Builder addContextData(String key, String value) {
            if (this.contextData == null) {
                this.contextData = new HashMap<>();
            }
            this.contextData.put(key, value);
            return this;
        }

        public Builder extraFields(Map<String, Object> extraFields) {
            this.extraFields = extraFields;
            return this;
        }

        public Builder addExtraField(String key, Object value) {
            if (this.extraFields == null) {
                this.extraFields = new HashMap<>();
            }
            this.extraFields.put(key, value);
            return this;
        }

        /**
         * 构建 LogEvent
         */
        public LogEvent build() {
            // 校验必填字段
            if (serviceName == null || methodName == null || side == null) {
                throw new IllegalArgumentException(
                        "serviceName, methodName, side are required");
            }
            return new LogEvent(this);
        }
    }
}
