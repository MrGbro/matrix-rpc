package io.homeey.matrix.rpc.environment;

import io.homeey.matrix.rpc.core.URL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * 环境标签匹配器
 * <p>
 * 负责判断 Provider URL 是否匹配目标环境的标签
 * 
 * <h3>🎯 匹配规则</h3>
 * <ul>
 *   <li><b>精确匹配</b>: 环境名称完全相同</li>
 *   <li><b>标签匹配</b>: 自定义标签全部匹配</li>
 *   <li><b>命名空间匹配</b>: Kubernetes namespace 匹配</li>
 *   <li><b>集群匹配</b>: 集群标识匹配</li>
 * </ul>
 * 
 * <h3>💡 使用示例</h3>
 * <pre>
 * Environment targetEnv = Environment.builder()
 *     .env("test")
 *     .label("version", "v2.0")
 *     .build();
 * 
 * boolean matched = EnvironmentLabelMatcher.matches(providerUrl, targetEnv);
 * </pre>
 *
 * @author Matrix RPC Team
 */
public class EnvironmentLabelMatcher {

    private static final Logger logger = LoggerFactory.getLogger(EnvironmentLabelMatcher.class);

    // 标签 key
    private static final String ENV_KEY = "env";
    private static final String NAMESPACE_KEY = "namespace";
    private static final String CLUSTER_KEY = "cluster";

    /**
     * 判断 Provider 是否匹配目标环境
     * 
     * @param provider Provider URL
     * @param targetEnv 目标环境
     * @return 如果匹配返回 true
     */
    public static boolean matches(URL provider, Environment targetEnv) {
        if (provider == null || targetEnv == null) {
            return false;
        }

        // 1. 检查环境名称
        if (!matchesEnv(provider, targetEnv.getEnv())) {
            return false;
        }

        // 2. 检查命名空间
        if (!matchesNamespace(provider, targetEnv.getNamespace())) {
            return false;
        }

        // 3. 检查集群
        if (!matchesCluster(provider, targetEnv.getCluster())) {
            return false;
        }

        // 4. 检查自定义标签
        if (!matchesLabels(provider, targetEnv.getLabels())) {
            return false;
        }

        logger.debug("Provider matched target environment: provider={}, env={}", 
                    provider.getAddress(), targetEnv.getEnv());
        return true;
    }

    /**
     * 判断 Provider 是否匹配环境名称
     * 
     * @param provider Provider URL
     * @param targetEnv 目标环境名称
     * @return 如果匹配返回 true
     */
    public static boolean matchesEnv(URL provider, String targetEnv) {
        String providerEnv = provider.getParameter(ENV_KEY);
        
        // 如果 Provider 没有环境标签，则认为是通用服务（可被任何环境调用）
        if (providerEnv == null || providerEnv.isEmpty()) {
            logger.trace("Provider has no env label, treated as universal: {}", provider.getAddress());
            return true;
        }

        return targetEnv.equals(providerEnv);
    }

    /**
     * 判断 Provider 是否匹配命名空间
     * 
     * @param provider Provider URL
     * @param targetNamespace 目标命名空间
     * @return 如果匹配返回 true
     */
    public static boolean matchesNamespace(URL provider, String targetNamespace) {
        String providerNamespace = provider.getParameter(NAMESPACE_KEY);
        
        // 如果未指定命名空间，默认为 "default"
        if (providerNamespace == null || providerNamespace.isEmpty()) {
            providerNamespace = "default";
        }
        if (targetNamespace == null || targetNamespace.isEmpty()) {
            targetNamespace = "default";
        }

        return targetNamespace.equals(providerNamespace);
    }

    /**
     * 判断 Provider 是否匹配集群
     * 
     * @param provider Provider URL
     * @param targetCluster 目标集群
     * @return 如果匹配返回 true
     */
    public static boolean matchesCluster(URL provider, String targetCluster) {
        String providerCluster = provider.getParameter(CLUSTER_KEY);
        
        // 如果未指定集群，默认为 "default"
        if (providerCluster == null || providerCluster.isEmpty()) {
            providerCluster = "default";
        }
        if (targetCluster == null || targetCluster.isEmpty()) {
            targetCluster = "default";
        }

        return targetCluster.equals(providerCluster);
    }

    /**
     * 判断 Provider 是否匹配自定义标签
     * 
     * @param provider Provider URL
     * @param targetLabels 目标标签
     * @return 如果所有标签都匹配返回 true
     */
    public static boolean matchesLabels(URL provider, Map<String, String> targetLabels) {
        if (targetLabels == null || targetLabels.isEmpty()) {
            return true;  // 没有额外标签要求，直接匹配
        }

        for (Map.Entry<String, String> entry : targetLabels.entrySet()) {
            String key = entry.getKey();
            String expectedValue = entry.getValue();
            String actualValue = provider.getParameter(key);

            if (!expectedValue.equals(actualValue)) {
                logger.trace("Label mismatch: key={}, expected={}, actual={}", 
                           key, expectedValue, actualValue);
                return false;
            }
        }

        return true;
    }

    /**
     * 判断 Provider 是否没有环境标签（通用服务）
     * 
     * @param provider Provider URL
     * @return 如果没有环境标签返回 true
     */
    public static boolean isUniversal(URL provider) {
        String providerEnv = provider.getParameter(ENV_KEY);
        return providerEnv == null || providerEnv.isEmpty();
    }

    /**
     * 获取 Provider 的环境名称
     * 
     * @param provider Provider URL
     * @return 环境名称，如果未设置返回 null
     */
    public static String getEnvironment(URL provider) {
        return provider.getParameter(ENV_KEY);
    }
}
