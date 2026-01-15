package io.homeey.matrix.rpc.observability.api;

/**
 * 上报事件基类
 * 
 * <p>所有可观测性事件（Metrics、Tracing、Logging）的基类。
 * 
 * @author Matrix RPC Team
 * @since 0.0.7
 */
public abstract class ReportEvent {
    
    /**
     * 事件类型
     */
    public enum Type {
        /** Metrics 指标事件 */
        METRICS,
        /** Tracing 追踪事件 */
        TRACING,
        /** Logging 日志事件 */
        LOGGING
    }
    
    /**
     * 事件创建时间戳（毫秒）
     */
    protected final long timestamp;
    
    /**
     * 事件类型
     */
    protected final Type type;
    
    protected ReportEvent(Type type) {
        this.timestamp = System.currentTimeMillis();
        this.type = type;
    }
    
    /**
     * 获取事件创建时间戳
     * 
     * @return 时间戳（毫秒）
     */
    public long getTimestamp() {
        return timestamp;
    }
    
    /**
     * 获取事件类型
     * 
     * @return 事件类型
     */
    public Type getType() {
        return type;
    }
    
    /**
     * 估算事件大小（字节）
     * 
     * <p>用于内存监控和队列容量规划。
     * 
     * @return 估算大小（字节）
     */
    public abstract int estimateSize();
}
