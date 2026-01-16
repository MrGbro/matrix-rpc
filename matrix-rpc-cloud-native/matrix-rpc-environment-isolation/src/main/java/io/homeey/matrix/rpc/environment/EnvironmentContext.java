package io.homeey.matrix.rpc.environment;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 环境上下文
 * <p>
 * 使用 ThreadLocal 存储当前线程的环境信息，支持跨方法传递
 * 
 * <h3>🔄 使用场景</h3>
 * <ul>
 *   <li>跨服务调用时自动传递环境标签</li>
 *   <li>在调用链路中保持环境上下文</li>
 *   <li>灵活切换目标环境（如：dev 调用 test 环境服务）</li>
 * </ul>
 * 
 * <h3>💡 使用示例</h3>
 * <pre>
 * // 设置目标环境
 * EnvironmentContext.setTargetEnvironment("test");
 * try {
 *     // 此次调用会路由到 test 环境
 *     echoService.echo("hello");
 * } finally {
 *     EnvironmentContext.clear();
 * }
 * </pre>
 *
 * @author Matrix RPC Team
 */
public class EnvironmentContext {

    private static final Logger logger = LoggerFactory.getLogger(EnvironmentContext.class);

    /**
     * 当前线程的环境上下文
     */
    private static final ThreadLocal<Environment> CONTEXT = new ThreadLocal<>();

    /**
     * 当前应用的默认环境
     */
    private static final String DEFAULT_ENV = System.getProperty("matrix.env", "dev");

    /**
     * 获取当前线程的环境上下文
     * 
     * @return 环境上下文，如果未设置则返回默认环境
     */
    public static Environment getEnvironment() {
        Environment env = CONTEXT.get();
        if (env == null) {
            // 返回默认环境
            return Environment.builder()
                    .namespace("default")
                    .cluster("default")
                    .env(DEFAULT_ENV)
                    .build();
        }
        return env;
    }

    /**
     * 设置当前线程的环境上下文
     * 
     * @param environment 环境上下文
     */
    public static void setEnvironment(Environment environment) {
        CONTEXT.set(environment);
        logger.debug("Set environment context: {}", environment);
    }

    /**
     * 设置目标环境（简化方法）
     * 
     * @param targetEnv 目标环境名称（dev/test/staging/prod）
     */
    public static void setTargetEnvironment(String targetEnv) {
        Environment env = Environment.builder()
                .namespace("default")
                .cluster("default")
                .env(targetEnv)
                .build();
        setEnvironment(env);
    }

    /**
     * 获取目标环境名称
     * 
     * @return 目标环境名称
     */
    public static String getTargetEnvironment() {
        return getEnvironment().getEnv();
    }

    /**
     * 清除当前线程的环境上下文
     * <p>
     * 建议在 finally 块中调用，避免内存泄漏
     */
    public static void clear() {
        CONTEXT.remove();
        logger.trace("Cleared environment context");
    }

    /**
     * 获取当前应用的默认环境
     * 
     * @return 默认环境名称
     */
    public static String getDefaultEnvironment() {
        return DEFAULT_ENV;
    }

    /**
     * 检查是否设置了环境上下文
     * 
     * @return 如果已设置返回 true
     */
    public static boolean hasContext() {
        return CONTEXT.get() != null;
    }

    /**
     * 复制当前线程的环境上下文到子线程（用于异步调用）
     * 
     * @return 当前环境上下文的副本
     */
    public static Environment snapshot() {
        Environment current = CONTEXT.get();
        if (current == null) {
            return null;
        }
        // 返回副本
        return Environment.builder()
                .namespace(current.getNamespace())
                .cluster(current.getCluster())
                .env(current.getEnv())
                .labels(current.getLabels())
                .build();
    }

    /**
     * 在子线程中恢复环境上下文
     * 
     * @param snapshot 环境上下文快照
     */
    public static void restore(Environment snapshot) {
        if (snapshot != null) {
            CONTEXT.set(snapshot);
            logger.debug("Restored environment context: {}", snapshot);
        }
    }
}
