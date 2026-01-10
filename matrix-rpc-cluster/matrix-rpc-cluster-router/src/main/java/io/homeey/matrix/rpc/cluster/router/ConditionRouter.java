package io.homeey.matrix.rpc.cluster.router;

import io.homeey.matrix.rpc.cluster.api.RouteRule;
import io.homeey.matrix.rpc.cluster.api.Router;
import io.homeey.matrix.rpc.core.Invocation;
import io.homeey.matrix.rpc.core.URL;
import io.homeey.matrix.rpc.spi.Activate;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 条件路由实现
 * 
 * <p>支持基于规则的动态路由，规则格式：
 * <pre>
 * conditions => actions
 * 
 * 示例：
 * - method=getUserInfo => host=192.168.1.100
 * - method=getOrder & param.userId=123 => tag=vip
 * - host=192.168.1.* => weight=200
 * </pre>
 * 
 * <p>条件支持：
 * <ul>
 *   <li>method - 方法名</li>
 *   <li>param.* - 方法参数</li>
 *   <li>tag - 请求标签</li>
 *   <li>host - 服务端主机</li>
 * </ul>
 * 
 * @author Matrix RPC Team
 * @since 2026-01-11
 */
@Activate(order = 20)
public class ConditionRouter implements Router {
    
    private List<RouteRule> rules = new ArrayList<>();
    
    public ConditionRouter() {
        // 可以从配置中心加载规则
    }
    
    /**
     * 设置路由规则
     */
    public void setRules(List<RouteRule> rules) {
        if (rules != null) {
            this.rules = new ArrayList<>(rules);
            // 按优先级排序
            this.rules.sort((r1, r2) -> Integer.compare(r1.getPriority(), r2.getPriority()));
        }
    }
    
    /**
     * 添加路由规则
     */
    public void addRule(RouteRule rule) {
        if (rule != null && rule.isEnabled()) {
            rules.add(rule);
            // 重新排序
            rules.sort((r1, r2) -> Integer.compare(r1.getPriority(), r2.getPriority()));
        }
    }
    
    /**
     * 删除路由规则
     */
    public void removeRule(String ruleName) {
        rules.removeIf(rule -> rule.getName().equals(ruleName));
    }
    
    @Override
    public int getPriority() {
        return 20;
    }
    
    @Override
    public List<URL> route(List<URL> providers, Invocation invocation) {
        if (providers == null || providers.isEmpty() || rules.isEmpty()) {
            return providers;
        }
        
        // 应用所有启用的规则
        List<URL> result = new ArrayList<>(providers);
        for (RouteRule rule : rules) {
            if (rule.isEnabled() && matchConditions(rule, invocation)) {
                result = applyAction(result, rule.getAction());
                // 如果过滤后没有可用实例，根据 force 决定是否继续
                if (result.isEmpty() && !rule.getAction().isForce()) {
                    return providers; // 返回原始列表
                }
            }
        }
        
        return result.isEmpty() ? providers : result;
    }
    
    /**
     * 匹配所有条件（AND 关系）
     */
    private boolean matchConditions(RouteRule rule, Invocation invocation) {
        if (rule.getConditions() == null || rule.getConditions().isEmpty()) {
            return false;
        }
        
        for (RouteRule.Condition condition : rule.getConditions()) {
            if (!matchCondition(condition, invocation)) {
                return false;
            }
        }
        
        return true;
    }
    
    /**
     * 匹配单个条件
     */
    private boolean matchCondition(RouteRule.Condition condition, Invocation invocation) {
        String key = condition.getKey();
        String expectedValue = condition.getValue();
        RouteRule.Operator operator = condition.getOperator();
        
        String actualValue = extractValue(key, invocation);
        if (actualValue == null) {
            return false;
        }
        
        return applyOperator(actualValue, expectedValue, operator);
    }
    
    /**
     * 从 Invocation 中提取值
     */
    private String extractValue(String key, Invocation invocation) {
        if (key == null) {
            return null;
        }
        
        // 方法名
        if ("method".equals(key)) {
            return invocation.methodName();
        }
        
        // 附件参数（如 tag）
        if (invocation.getAttachments().containsKey(key)) {
            return invocation.getAttachments().get(key);
        }
        
        // 参数提取（如 param.userId）
        if (key.startsWith("param.")) {
            // TODO: 实现参数提取逻辑
            // 这需要在 Invocation 中添加更多信息或使用反射
            return null;
        }
        
        return null;
    }
    
    /**
     * 应用操作符
     */
    private boolean applyOperator(String actual, String expected, RouteRule.Operator operator) {
        if (operator == null) {
            operator = RouteRule.Operator.EQUAL;
        }
        
        switch (operator) {
            case EQUAL:
                return actual.equals(expected);
            case NOT_EQUAL:
                return !actual.equals(expected);
            case CONTAINS:
                return actual.contains(expected);
            case REGEX:
                return Pattern.matches(expected, actual);
            case GREATER_THAN:
                try {
                    return Double.parseDouble(actual) > Double.parseDouble(expected);
                } catch (NumberFormatException e) {
                    return false;
                }
            case LESS_THAN:
                try {
                    return Double.parseDouble(actual) < Double.parseDouble(expected);
                } catch (NumberFormatException e) {
                    return false;
                }
            default:
                return false;
        }
    }
    
    /**
     * 应用路由动作
     */
    private List<URL> applyAction(List<URL> providers, RouteRule.Action action) {
        if (action == null || action.getTarget() == null) {
            return providers;
        }
        
        String target = action.getTarget();
        RouteRule.ActionType type = action.getType();
        
        // 解析目标表达式（如 tag=gray, host=192.168.1.100）
        String[] parts = target.split("=", 2);
        if (parts.length != 2) {
            return providers;
        }
        
        String targetKey = parts[0].trim();
        String targetValue = parts[1].trim();
        
        switch (type) {
            case FILTER:
                // 保留匹配的实例
                return providers.stream()
                        .filter(url -> matchTarget(url, targetKey, targetValue))
                        .collect(Collectors.toList());
            case EXCLUDE:
                // 移除匹配的实例
                return providers.stream()
                        .filter(url -> !matchTarget(url, targetKey, targetValue))
                        .collect(Collectors.toList());
            case WEIGHT:
                // TODO: 权重调整（需要修改 URL 的 weight 参数）
                return providers;
            default:
                return providers;
        }
    }
    
    /**
     * 匹配目标条件
     */
    private boolean matchTarget(URL url, String key, String value) {
        if ("host".equals(key)) {
            // 支持通配符匹配
            String pattern = value.replace("*", ".*");
            return Pattern.matches(pattern, url.getHost());
        }
        
        if ("port".equals(key)) {
            return String.valueOf(url.getPort()).equals(value);
        }
        
        // 匹配 URL 参数（如 tag）
        String paramValue = url.getParameter(key);
        if (paramValue != null) {
            // 支持通配符
            String pattern = value.replace("*", ".*");
            return Pattern.matches(pattern, paramValue);
        }
        
        return false;
    }
    
    @Override
    public boolean isEnabled() {
        return !rules.isEmpty();
    }
    
    /**
     * 获取当前规则数量
     */
    public int getRuleCount() {
        return rules.size();
    }
}
