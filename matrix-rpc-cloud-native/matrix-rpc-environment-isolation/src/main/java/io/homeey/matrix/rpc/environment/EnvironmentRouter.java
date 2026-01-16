package io.homeey.matrix.rpc.environment;

import io.homeey.matrix.rpc.cluster.api.Router;
import io.homeey.matrix.rpc.core.Invocation;
import io.homeey.matrix.rpc.core.URL;
import io.homeey.matrix.rpc.spi.Activate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Environment Router - 环境隔离路由器
 * <p>
 * 基于环境标签（dev/test/staging/prod）进行流量隔离
 * 
 * <h3>🔄 职责</h3>
 * <ul>
 *   <li>从 EnvironmentContext 获取目标环境</li>
 *   <li>委托给 EnvironmentLabelMatcher 进行标签匹配</li>
 *   <li>支持严格模式和宽松模式的降级策略</li>
 * </ul>
 * 
 * <h3>⚙️ 路由策略</h3>
 * <ul>
 *   <li><b>严格模式</b>（strict=true）: 只能调用同环境服务，无可用服务时返回空</li>
 *   <li><b>宽松模式</b>（strict=false）: 优先同环境，无可用服务时降级到 prod</li>
 * </ul>
 * 
 * <h3>🎛️ 系统属性</h3>
 * <ul>
 *   <li><b>matrix.env</b>: 当前应用的环境（dev/test/staging/prod）</li>
 *   <li><b>matrix.env.strict</b>: 是否启用严格模式（默认 false）</li>
 * </ul>
 */
@Activate(order = 100)
public class EnvironmentRouter implements Router {

    private static final Logger logger = LoggerFactory.getLogger(EnvironmentRouter.class);
    
    // 环境标签 key
    private static final String ENV_KEY = "env";
    
    // 当前应用的环境
    private static final String CURRENT_ENV = System.getProperty("matrix.env", "dev");
    
    // 是否启用严格模式（true=只能调用同环境服务，false=允许降级到 prod）
    private static final boolean STRICT_MODE = Boolean.parseBoolean(
            System.getProperty("matrix.env.strict", "false"));

    @Override
    public List<URL> route(List<URL> providers, Invocation invocation) {
        if (providers == null || providers.isEmpty()) {
            return providers;
        }
        
        // 从 EnvironmentContext 获取目标环境
        Environment targetEnv = getTargetEnvironment(invocation);
        
        logger.debug("Environment routing: target={}, strict={}", 
                    targetEnv.getEnv(), STRICT_MODE);
        
        // 1. 使用 EnvironmentLabelMatcher 过滤出匹配的 Provider
        List<URL> matchedProviders = filterByEnvironment(providers, targetEnv);
        
        if (!matchedProviders.isEmpty()) {
            logger.debug("Found {} providers in target environment: {}", 
                        matchedProviders.size(), targetEnv.getEnv());
            return matchedProviders;
        }
        
        // 2. 目标环境无可用服务，执行降级策略
        return fallbackStrategy(providers, targetEnv);
    }

    /**
     * 获取目标环境
     * <p>
     * 优先从 EnvironmentContext 获取，其次从 Invocation Attachments 获取
     * 
     * @param invocation RPC 调用上下文
     * @return 目标环境
     */
    private Environment getTargetEnvironment(Invocation invocation) {
        // 1. 尝试从 EnvironmentContext 获取
        if (EnvironmentContext.hasContext()) {
            return EnvironmentContext.getEnvironment();
        }
        
        // 2. 从 Invocation Attachments 获取环境名称
        Map<String, String> attachments = invocation.getAttachments();
        String envFromInvocation = attachments.get(ENV_KEY);
        
        String targetEnvName = (envFromInvocation != null && !envFromInvocation.isEmpty()) 
                ? envFromInvocation : CURRENT_ENV;
        
        // 3. 构建简单的 Environment 对象
        return Environment.builder()
                .env(targetEnvName)
                .namespace("default")
                .cluster("default")
                .build();
    }

    /**
     * 使用 EnvironmentLabelMatcher 过滤匹配的 Provider
     * 
     * @param providers Provider 列表
     * @param targetEnv 目标环境
     * @return 匹配的 Provider 列表
     */
    private List<URL> filterByEnvironment(List<URL> providers, Environment targetEnv) {
        List<URL> result = new ArrayList<>();
        
        for (URL provider : providers) {
            // 委托给 EnvironmentLabelMatcher 进行匹配
            if (EnvironmentLabelMatcher.matches(provider, targetEnv)) {
                result.add(provider);
            }
        }
        
        return result;
    }

    /**
     * 降级策略
     * 
     * @param providers 所有 Provider
     * @param targetEnv 目标环境
     * @return 降级后的 Provider 列表
     */
    private List<URL> fallbackStrategy(List<URL> providers, Environment targetEnv) {
        if (STRICT_MODE) {
            // 严格模式：不允许跨环境调用
            logger.warn("⚠️ No available provider in target environment: {} (strict mode enabled)", 
                       targetEnv.getEnv());
            return new ArrayList<>();
        }
        
        // 宽松模式：降级到 prod 环境
        Environment prodEnv = Environment.builder()
                .env("prod")
                .namespace(targetEnv.getNamespace())
                .cluster(targetEnv.getCluster())
                .build();
        
        List<URL> prodProviders = filterByEnvironment(providers, prodEnv);
        if (!prodProviders.isEmpty()) {
            logger.warn("🔄 No provider in target environment: {}, fallback to prod (found {} providers)", 
                       targetEnv.getEnv(), prodProviders.size());
            return prodProviders;
        }
        
        // prod 也没有，返回所有通用服务（无环境标签）
        List<URL> universalProviders = new ArrayList<>();
        for (URL provider : providers) {
            if (EnvironmentLabelMatcher.isUniversal(provider)) {
                universalProviders.add(provider);
            }
        }
        
        if (!universalProviders.isEmpty()) {
            logger.warn("🌐 No provider in target/prod environment, using {} universal providers", 
                       universalProviders.size());
            return universalProviders;
        }
        
        logger.error("❌ No available provider after environment routing");
        return new ArrayList<>();
    }

    @Override
    public int getPriority() {
        return 100;
    }
}
