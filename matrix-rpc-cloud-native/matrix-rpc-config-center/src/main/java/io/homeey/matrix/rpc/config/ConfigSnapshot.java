package io.homeey.matrix.rpc.config;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * Configuration Snapshot - 配置快照（用于版本管理和回滚）
 */
public class ConfigSnapshot implements Serializable {
    
    private static final long serialVersionUID = 1L;

    private final String version;
    private final Map<String, String> config;
    private final long timestamp;
    private final String creator;

    public ConfigSnapshot(String version, Map<String, String> config, String creator) {
        this.version = version;
        this.config = new HashMap<>(config);
        this.timestamp = System.currentTimeMillis();
        this.creator = creator;
    }

    public String getVersion() {
        return version;
    }

    public Map<String, String> getConfig() {
        return new HashMap<>(config);
    }

    public long getTimestamp() {
        return timestamp;
    }

    public String getCreator() {
        return creator;
    }

    @Override
    public String toString() {
        return "ConfigSnapshot{" +
                "version='" + version + '\'' +
                ", configCount=" + config.size() +
                ", timestamp=" + timestamp +
                ", creator='" + creator + '\'' +
                '}';
    }
}
