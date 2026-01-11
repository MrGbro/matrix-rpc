package io.homeey.matrix.rpc.filter.sentinel;

import com.alibaba.csp.sentinel.slots.block.degrade.DegradeRule;
import com.alibaba.csp.sentinel.slots.block.degrade.DegradeRuleManager;
import com.alibaba.csp.sentinel.slots.block.degrade.circuitbreaker.CircuitBreakerStrategy;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRule;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRuleManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * Sentinel规则初始化器
 * 
 * <p>从 sentinel-rules.properties 加载规则
 * 
 * @author Matrix RPC Team
 * @since 2026-01-11
 */
public class SentinelRuleInitializer {
    
    private static final Logger logger = LoggerFactory.getLogger(SentinelRuleInitializer.class);
    private static final String CONFIG_FILE = "sentinel-rules.properties";
    
    static {
        // 类加载时自动初始化规则
        initRules();
    }
    
    /**
     * 初始化Sentinel规则
     */
    public static void initRules() {
        try (InputStream is = SentinelRuleInitializer.class
                .getClassLoader()
                .getResourceAsStream(CONFIG_FILE)) {
            
            if (is == null) {
                logger.info("No sentinel-rules.properties found, using default rules");
                return;
            }
            
            Properties props = new Properties();
            props.load(is);
            
            // 加载限流规则
            List<FlowRule> flowRules = loadFlowRules(props);
            FlowRuleManager.loadRules(flowRules);
            logger.info("Loaded {} flow rules", flowRules.size());
            
            // 加载熔断规则
            List<DegradeRule> degradeRules = loadDegradeRules(props);
            DegradeRuleManager.loadRules(degradeRules);
            logger.info("Loaded {} degrade rules", degradeRules.size());
            
        } catch (Exception e) {
            logger.error("Failed to load Sentinel rules", e);
        }
    }
    
    /**
     * 加载限流规则
     */
    private static List<FlowRule> loadFlowRules(Properties props) {
        List<FlowRule> rules = new ArrayList<>();
        
        // 格式: sentinel.flow.<ServiceInterface>.<method>.qps=<限流值>
        props.stringPropertyNames().stream()
            .filter(key -> key.startsWith("sentinel.flow.") && key.endsWith(".qps"))
            .forEach(key -> {
                String resource = key.substring(14, key.length() - 4);
                int qps = Integer.parseInt(props.getProperty(key));
                
                FlowRule rule = new FlowRule(resource);
                rule.setCount(qps);
                rule.setGrade(com.alibaba.csp.sentinel.slots.block.RuleConstant.FLOW_GRADE_QPS);
                rules.add(rule);
                
                logger.debug("Flow rule: resource={}, qps={}", resource, qps);
            });
        
        return rules;
    }
    
    /**
     * 加载熔断规则
     */
    private static List<DegradeRule> loadDegradeRules(Properties props) {
        List<DegradeRule> rules = new ArrayList<>();
        
        // 格式: sentinel.degrade.<ServiceInterface>.<method>.errorRatio=<异常比例>
        props.stringPropertyNames().stream()
            .filter(key -> key.startsWith("sentinel.degrade.") && key.endsWith(".errorRatio"))
            .forEach(key -> {
                String resource = key.substring(17, key.length() - 11);
                double errorRatio = Double.parseDouble(props.getProperty(key));
                
                DegradeRule rule = new DegradeRule(resource);
                rule.setGrade(CircuitBreakerStrategy.ERROR_RATIO.getType());
                rule.setCount(errorRatio);
                rule.setTimeWindow(10); // 熔断时长10秒
                rule.setMinRequestAmount(5); // 最小请求数
                rules.add(rule);
                
                logger.debug("Degrade rule: resource={}, errorRatio={}", resource, errorRatio);
            });
        
        return rules;
    }
}
