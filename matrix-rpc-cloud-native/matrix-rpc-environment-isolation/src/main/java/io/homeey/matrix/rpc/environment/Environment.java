package io.homeey.matrix.rpc.environment;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * 环境定义
 * <p>
 * 定义一个完整的环境上下文，包括：
 * <ul>
 *   <li><b>namespace</b>: Kubernetes 命名空间</li>
 *   <li><b>cluster</b>: 集群标识</li>
 *   <li><b>env</b>: 环境名称（dev/test/staging/prod）</li>
 *   <li><b>labels</b>: 自定义标签（如：region、zone、version）</li>
 * </ul>
 * 
 * <h3>🏷️ 使用场景</h3>
 * <ul>
 *   <li>多环境隔离（dev/test/staging/prod）</li>
 *   <li>多集群路由（按 cluster 路由）</li>
 *   <li>多租户隔离（按 namespace 路由）</li>
 *   <li>灰度发布（按 labels 路由）</li>
 * </ul>
 *
 * @author Matrix RPC Team
 */
public class Environment {

    /**
     * Kubernetes 命名空间
     */
    private final String namespace;

    /**
     * 集群标识
     */
    private final String cluster;

    /**
     * 环境名称（dev/test/staging/prod）
     */
    private final String env;

    /**
     * 自定义标签（只读）
     */
    private final Map<String, String> labels;

    private Environment(Builder builder) {
        this.namespace = builder.namespace;
        this.cluster = builder.cluster;
        this.env = builder.env;
        this.labels = Collections.unmodifiableMap(new HashMap<>(builder.labels));
    }

    public String getNamespace() {
        return namespace;
    }

    public String getCluster() {
        return cluster;
    }

    public String getEnv() {
        return env;
    }

    public Map<String, String> getLabels() {
        return labels;
    }

    /**
     * 获取指定标签的值
     * 
     * @param key 标签 key
     * @return 标签值，如果不存在返回 null
     */
    public String getLabel(String key) {
        return labels.get(key);
    }

    /**
     * 检查是否包含指定标签
     * 
     * @param key 标签 key
     * @return 如果存在返回 true
     */
    public boolean hasLabel(String key) {
        return labels.containsKey(key);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Environment that = (Environment) o;
        return Objects.equals(namespace, that.namespace) &&
               Objects.equals(cluster, that.cluster) &&
               Objects.equals(env, that.env) &&
               Objects.equals(labels, that.labels);
    }

    @Override
    public int hashCode() {
        return Objects.hash(namespace, cluster, env, labels);
    }

    @Override
    public String toString() {
        return "Environment{" +
               "namespace='" + namespace + '\'' +
               ", cluster='" + cluster + '\'' +
               ", env='" + env + '\'' +
               ", labels=" + labels +
               '}';
    }

    /**
     * 创建 Builder
     * 
     * @return Builder 实例
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Environment Builder
     */
    public static class Builder {
        private String namespace = "default";
        private String cluster = "default";
        private String env = "dev";
        private Map<String, String> labels = new HashMap<>();

        public Builder namespace(String namespace) {
            this.namespace = namespace;
            return this;
        }

        public Builder cluster(String cluster) {
            this.cluster = cluster;
            return this;
        }

        public Builder env(String env) {
            this.env = env;
            return this;
        }

        public Builder label(String key, String value) {
            this.labels.put(key, value);
            return this;
        }

        public Builder labels(Map<String, String> labels) {
            if (labels != null) {
                this.labels.putAll(labels);
            }
            return this;
        }

        public Environment build() {
            return new Environment(this);
        }
    }
}
