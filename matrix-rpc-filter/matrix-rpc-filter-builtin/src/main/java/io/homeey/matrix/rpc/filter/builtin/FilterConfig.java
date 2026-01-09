package io.homeey.matrix.rpc.filter.builtin;

/**
 * Filter 配置管理器
 * <p>
 * 支持通过系统属性或配置文件控制 Filter 开关
 * <p>
 * 配置方式：
 * <pre>
 * // 系统属性方式
 * -Dmatrix.filter.accesslog.enabled=true
 * -Dmatrix.filter.exception.enabled=true
 * -Dmatrix.filter.timeout.enabled=true
 * 
 * // 或者禁用所有 filter
 * -Dmatrix.filter.enabled=false
 * </pre>
 */
public class FilterConfig {

    private static final String FILTER_PREFIX = "matrix.filter.";
    private static final String GLOBAL_ENABLED_KEY = FILTER_PREFIX + "enabled";

    /**
     * 判断指定 Filter 是否启用
     *
     * @param filterName Filter 名称（如 accesslog, exception, timeout）
     * @return true 表示启用
     */
    public static boolean isEnabled(String filterName) {
        // 1. 检查全局开关
        String globalEnabled = System.getProperty(GLOBAL_ENABLED_KEY);
        if ("false".equalsIgnoreCase(globalEnabled)) {
            return false;
        }

        // 2. 检查单个 Filter 开关（默认启用）
        String filterKey = FILTER_PREFIX + filterName.toLowerCase() + ".enabled";
        String filterEnabled = System.getProperty(filterKey, "true");
        return !"false".equalsIgnoreCase(filterEnabled);
    }

    /**
     * 获取 Filter 配置值
     *
     * @param filterName Filter 名称
     * @param key        配置键
     * @param defaultValue 默认值
     * @return 配置值
     */
    public static String getConfig(String filterName, String key, String defaultValue) {
        String configKey = FILTER_PREFIX + filterName.toLowerCase() + "." + key;
        return System.getProperty(configKey, defaultValue);
    }

    /**
     * 获取 Filter 配置值（整数）
     */
    public static int getIntConfig(String filterName, String key, int defaultValue) {
        String value = getConfig(filterName, key, null);
        if (value == null) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    /**
     * 获取 Filter 配置值（长整数）
     */
    public static long getLongConfig(String filterName, String key, long defaultValue) {
        String value = getConfig(filterName, key, null);
        if (value == null) {
            return defaultValue;
        }
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
}
