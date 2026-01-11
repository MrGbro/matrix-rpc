package io.homeey.matrix.rpc.filter.resilience4j;

/**
 * Resilience4j Filter配置管理
 * 
 * @author Matrix RPC Team
 * @since 2026-01-11
 */
public class Resilience4jFilterConfig {
    
    private static final String PREFIX = "matrix.filter.";
    private static final String ENABLED_SUFFIX = ".enabled";
    
    /**
     * 检查Filter是否启用
     * 
     * @param filterName Filter名称
     * @return true-启用，false-禁用
     */
    public static boolean isEnabled(String filterName) {
        String key = PREFIX + filterName + ENABLED_SUFFIX;
        return Boolean.parseBoolean(System.getProperty(key, "true"));
    }
}
