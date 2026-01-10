package io.homeey.matrix.rpc.cluster.api;

import java.util.List;
import java.util.Map;

/**
 * 路由规则数据结构
 * 
 * <p>支持多种路由条件：
 * <ul>
 *   <li>标签路由：tag=gray</li>
 *   <li>条件路由：method=getUserInfo & param.userId=123 => host=192.168.1.100</li>
 *   <li>权重路由：weight=200</li>
 * </ul>
 * 
 * <p>规则示例（YAML 格式）：
 * <pre>
 * - name: gray-routing
 *   priority: 10
 *   enabled: true
 *   conditions:
 *     - key: tag
 *       operator: EQUAL
 *       value: gray
 *   action:
 *     type: FILTER
 *     target: tag=gray
 * 
 * - name: method-routing
 *   priority: 20
 *   enabled: true
 *   conditions:
 *     - key: method
 *       operator: EQUAL
 *       value: getUserInfo
 *   action:
 *     type: FILTER
 *     target: host=192.168.1.100
 * </pre>
 * 
 * @author Matrix RPC Team
 * @since 2026-01-11
 */
public class RouteRule {
    
    /**
     * 规则名称（唯一标识）
     */
    private String name;
    
    /**
     * 优先级（数字越小优先级越高）
     */
    private int priority;
    
    /**
     * 是否启用
     */
    private boolean enabled = true;
    
    /**
     * 匹配条件列表（AND 关系）
     */
    private List<Condition> conditions;
    
    /**
     * 路由动作
     */
    private Action action;
    
    /**
     * 规则描述
     */
    private String description;
    
    /**
     * 创建时间
     */
    private long createTime;
    
    /**
     * 更新时间
     */
    private long updateTime;
    
    // Getters and Setters
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public int getPriority() {
        return priority;
    }
    
    public void setPriority(int priority) {
        this.priority = priority;
    }
    
    public boolean isEnabled() {
        return enabled;
    }
    
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
    
    public List<Condition> getConditions() {
        return conditions;
    }
    
    public void setConditions(List<Condition> conditions) {
        this.conditions = conditions;
    }
    
    public Action getAction() {
        return action;
    }
    
    public void setAction(Action action) {
        this.action = action;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public long getCreateTime() {
        return createTime;
    }
    
    public void setCreateTime(long createTime) {
        this.createTime = createTime;
    }
    
    public long getUpdateTime() {
        return updateTime;
    }
    
    public void setUpdateTime(long updateTime) {
        this.updateTime = updateTime;
    }
    
    /**
     * 匹配条件
     */
    public static class Condition {
        /**
         * 条件键（如：tag, method, param.userId, host）
         */
        private String key;
        
        /**
         * 操作符
         */
        private Operator operator;
        
        /**
         * 条件值
         */
        private String value;
        
        public String getKey() {
            return key;
        }
        
        public void setKey(String key) {
            this.key = key;
        }
        
        public Operator getOperator() {
            return operator;
        }
        
        public void setOperator(Operator operator) {
            this.operator = operator;
        }
        
        public String getValue() {
            return value;
        }
        
        public void setValue(String value) {
            this.value = value;
        }
    }
    
    /**
     * 路由动作
     */
    public static class Action {
        /**
         * 动作类型
         */
        private ActionType type;
        
        /**
         * 目标表达式（如：tag=gray, host=192.168.1.100）
         */
        private String target;
        
        /**
         * 是否强制（不降级）
         */
        private boolean force;
        
        public ActionType getType() {
            return type;
        }
        
        public void setType(ActionType type) {
            this.type = type;
        }
        
        public String getTarget() {
            return target;
        }
        
        public void setTarget(String target) {
            this.target = target;
        }
        
        public boolean isForce() {
            return force;
        }
        
        public void setForce(boolean force) {
            this.force = force;
        }
    }
    
    /**
     * 操作符枚举
     */
    public enum Operator {
        /** 等于 */
        EQUAL,
        /** 不等于 */
        NOT_EQUAL,
        /** 包含 */
        CONTAINS,
        /** 正则匹配 */
        REGEX,
        /** 大于 */
        GREATER_THAN,
        /** 小于 */
        LESS_THAN
    }
    
    /**
     * 动作类型
     */
    public enum ActionType {
        /** 过滤（保留匹配的实例） */
        FILTER,
        /** 排除（移除匹配的实例） */
        EXCLUDE,
        /** 权重调整 */
        WEIGHT
    }
}
