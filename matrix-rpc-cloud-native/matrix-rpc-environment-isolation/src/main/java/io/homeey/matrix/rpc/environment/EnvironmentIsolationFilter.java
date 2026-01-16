package io.homeey.matrix.rpc.environment;

import io.homeey.matrix.rpc.core.Invocation;
import io.homeey.matrix.rpc.core.Invoker;
import io.homeey.matrix.rpc.core.Result;
import io.homeey.matrix.rpc.filter.Filter;
import io.homeey.matrix.rpc.spi.Activate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * Environment Isolation Filter - 环境隔离过滤器
 * <p>
 * 自动注入环境标签，防止跨环境调用
 * 
 * <h3>🔄 职责</h3>
 * <ul>
 *   <li>从 EnvironmentContext 读取环境上下文</li>
 *   <li>将环境标签注入到 Invocation Attachments</li>
 *   <li>支持 ThreadLocal 上下文传递</li>
 * </ul>
 * 
 * <h3>🎛️ 系统属性</h3>
 * <ul>
 *   <li><b>matrix.env</b>: 当前应用的默认环境（dev/test/staging/prod）</li>
 * </ul>
 */
@Activate(order = 90, group = {"consumer"})
public class EnvironmentIsolationFilter implements Filter {

    private static final Logger logger = LoggerFactory.getLogger(EnvironmentIsolationFilter.class);
    
    private static final String ENV_KEY = "env";
    private static final String CURRENT_ENV = System.getProperty("matrix.env", "dev");

    @Override
    public Result invoke(Invoker<?> invoker, Invocation invocation) {
        // 1. 从 EnvironmentContext 获取环境上下文
        Environment env = EnvironmentContext.getEnvironment();
        
        // 2. 注入环境标签到 Invocation Attachments
        Map<String, String> attachments = invocation.getAttachments();
        
        // 如果 Attachments 中已经有环境标签，不覆盖（优先级：显式设置 > Context > 默认值）
        if (attachments.get(ENV_KEY) == null) {
            attachments.put(ENV_KEY, env.getEnv());
            logger.debug("✅ Injected environment tag: env={}", env.getEnv());
        }
        
        // 3. 注入其他环境信息（可选）
        if (attachments.get("namespace") == null) {
            attachments.put("namespace", env.getNamespace());
        }
        if (attachments.get("cluster") == null) {
            attachments.put("cluster", env.getCluster());
        }
        
        // 4. 注入自定义标签
        for (Map.Entry<String, String> entry : env.getLabels().entrySet()) {
            String key = entry.getKey();
            if (attachments.get(key) == null) {
                attachments.put(key, entry.getValue());
                logger.debug("🏷️ Injected custom label: {}={}", key, entry.getValue());
            }
        }
        
        return invoker.invoke(invocation);
    }
}
