package io.homeey.matrix.rpc.config;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * 配置发布策略
 * <p>
 * 支持多种发布策略：
 * <ul>
 *   <li><b>全量发布</b>: 立即推送给所有客户端</li>
 *   <li><b>灰度发布</b>: 按百分比、IP列表、环境标签等进行灰度</li>
 *   <li><b>定时发布</b>: 指定时间点发布</li>
 * </ul>
 * 
 * <h3>🎯 使用场景</h3>
 * <ul>
 *   <li>紧急配置变更：使用全量发布</li>
 *   <li>风险配置变更：使用灰度发布，逐步放量</li>
 *   <li>计划变更：使用定时发布</li>
 * </ul>
 * 
 * <h3>💡 使用示例</h3>
 * <pre>
 * // 灰度发布：先推送给 10% 的实例
 * PublishStrategy strategy = PublishStrategy.grayPublish()
 *     .percentage(10)
 *     .build();
 * 
 * configCenter.publishConfig("timeout", "5000", "v2.0", strategy);
 * </pre>
 *
 * @author Matrix RPC Team
 */
public class PublishStrategy {

    /**
     * 发布类型
     */
    public enum Type {
        /**
         * 全量发布：立即推送给所有客户端
         */
        FULL,
        
        /**
         * 灰度发布：按规则逐步推送
         */
        GRAY,
        
        /**
         * 定时发布：在指定时间点发布
         */
        SCHEDULED
    }

    private final Type type;
    private final int percentage;
    private final Set<String> targetIps;
    private final Set<String> targetEnvs;
    private final long scheduledTime;

    private PublishStrategy(Builder builder) {
        this.type = builder.type;
        this.percentage = builder.percentage;
        this.targetIps = Collections.unmodifiableSet(new HashSet<>(builder.targetIps));
        this.targetEnvs = Collections.unmodifiableSet(new HashSet<>(builder.targetEnvs));
        this.scheduledTime = builder.scheduledTime;
    }

    public Type getType() {
        return type;
    }

    public int getPercentage() {
        return percentage;
    }

    public Set<String> getTargetIps() {
        return targetIps;
    }

    public Set<String> getTargetEnvs() {
        return targetEnvs;
    }

    public long getScheduledTime() {
        return scheduledTime;
    }

    /**
     * 判断是否应该推送给指定客户端
     * 
     * @param clientIp 客户端 IP
     * @param clientEnv 客户端环境
     * @return 如果应该推送返回 true
     */
    public boolean shouldPush(String clientIp, String clientEnv) {
        switch (type) {
            case FULL:
                return true;
                
            case GRAY:
                // IP 白名单优先级最高
                if (!targetIps.isEmpty()) {
                    return targetIps.contains(clientIp);
                }
                
                // 环境标签匹配
                if (!targetEnvs.isEmpty()) {
                    return targetEnvs.contains(clientEnv);
                }
                
                // 按百分比灰度（基于 IP hash）
                if (percentage > 0 && percentage < 100) {
                    int hash = Math.abs(clientIp.hashCode());
                    return (hash % 100) < percentage;
                }
                
                return false;
                
            case SCHEDULED:
                return System.currentTimeMillis() >= scheduledTime;
                
            default:
                return false;
        }
    }

    /**
     * 创建全量发布策略
     * 
     * @return 全量发布策略
     */
    public static PublishStrategy fullPublish() {
        return new Builder(Type.FULL).build();
    }

    /**
     * 创建灰度发布策略 Builder
     * 
     * @return Builder 实例
     */
    public static Builder grayPublish() {
        return new Builder(Type.GRAY);
    }

    /**
     * 创建定时发布策略 Builder
     * 
     * @param scheduledTime 发布时间戳（毫秒）
     * @return Builder 实例
     */
    public static Builder scheduledPublish(long scheduledTime) {
        return new Builder(Type.SCHEDULED).scheduledTime(scheduledTime);
    }

    @Override
    public String toString() {
        return "PublishStrategy{" +
               "type=" + type +
               ", percentage=" + percentage +
               ", targetIps=" + targetIps.size() +
               ", targetEnvs=" + targetEnvs.size() +
               ", scheduledTime=" + scheduledTime +
               '}';
    }

    /**
     * PublishStrategy Builder
     */
    public static class Builder {
        private final Type type;
        private int percentage = 0;
        private Set<String> targetIps = new HashSet<>();
        private Set<String> targetEnvs = new HashSet<>();
        private long scheduledTime = 0;

        private Builder(Type type) {
            this.type = type;
        }

        /**
         * 设置灰度百分比（0-100）
         * 
         * @param percentage 百分比
         * @return Builder 实例
         */
        public Builder percentage(int percentage) {
            if (percentage < 0 || percentage > 100) {
                throw new IllegalArgumentException("Percentage must be between 0 and 100");
            }
            this.percentage = percentage;
            return this;
        }

        /**
         * 添加目标 IP
         * 
         * @param ip 目标 IP
         * @return Builder 实例
         */
        public Builder addTargetIp(String ip) {
            this.targetIps.add(ip);
            return this;
        }

        /**
         * 添加多个目标 IP
         * 
         * @param ips 目标 IP 集合
         * @return Builder 实例
         */
        public Builder targetIps(Set<String> ips) {
            this.targetIps.addAll(ips);
            return this;
        }

        /**
         * 添加目标环境
         * 
         * @param env 目标环境（dev/test/staging/prod）
         * @return Builder 实例
         */
        public Builder addTargetEnv(String env) {
            this.targetEnvs.add(env);
            return this;
        }

        /**
         * 添加多个目标环境
         * 
         * @param envs 目标环境集合
         * @return Builder 实例
         */
        public Builder targetEnvs(Set<String> envs) {
            this.targetEnvs.addAll(envs);
            return this;
        }

        /**
         * 设置定时发布时间
         * 
         * @param scheduledTime 发布时间戳（毫秒）
         * @return Builder 实例
         */
        public Builder scheduledTime(long scheduledTime) {
            this.scheduledTime = scheduledTime;
            return this;
        }

        public PublishStrategy build() {
            return new PublishStrategy(this);
        }
    }
}
