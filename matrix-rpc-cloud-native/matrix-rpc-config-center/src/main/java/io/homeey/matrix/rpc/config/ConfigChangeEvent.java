package io.homeey.matrix.rpc.config;

/**
 * Configuration Change Event - 配置变更事件
 */
public class ConfigChangeEvent {
    
    private final String key;
    private final String oldValue;
    private final String newValue;
    private final ChangeType changeType;
    private final long timestamp;

    public enum ChangeType {
        ADDED,      // 新增配置
        MODIFIED,   // 修改配置
        DELETED     // 删除配置
    }

    /**
     * EventType 作为 ChangeType 的别名，保持向后兼容
     */
    public static final class EventType {
        public static final ChangeType ADDED = ChangeType.ADDED;
        public static final ChangeType MODIFIED = ChangeType.MODIFIED;
        public static final ChangeType DELETED = ChangeType.DELETED;
    }

    public ConfigChangeEvent(String key, String oldValue, String newValue, ChangeType changeType) {
        this.key = key;
        this.oldValue = oldValue;
        this.newValue = newValue;
        this.changeType = changeType;
        this.timestamp = System.currentTimeMillis();
    }

    public String getKey() {
        return key;
    }

    public String getOldValue() {
        return oldValue;
    }

    public String getNewValue() {
        return newValue;
    }

    public ChangeType getChangeType() {
        return changeType;
    }

    public long getTimestamp() {
        return timestamp;
    }

    @Override
    public String toString() {
        return "ConfigChangeEvent{" +
                "key='" + key + '\'' +
                ", oldValue='" + oldValue + '\'' +
                ", newValue='" + newValue + '\'' +
                ", changeType=" + changeType +
                ", timestamp=" + timestamp +
                '}';
    }
}
