package io.homeey.matrix.rpc.filter.sentinel;

import com.alibaba.csp.sentinel.Entry;
import com.alibaba.csp.sentinel.EntryType;
import com.alibaba.csp.sentinel.SphU;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import io.homeey.matrix.rpc.core.Invocation;
import io.homeey.matrix.rpc.core.Invoker;
import io.homeey.matrix.rpc.core.Result;
import io.homeey.matrix.rpc.core.RpcException;
import io.homeey.matrix.rpc.filter.Filter;
import io.homeey.matrix.rpc.spi.Activate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Sentinel限流器Filter
 * 
 * <p>配置示例:
 * <pre>
 * # 启用限流（默认启用）
 * matrix.filter.sentinel.ratelimiter.enabled=true
 * 
 * # 限流规则（通过Sentinel Dashboard配置或sentinel-rules.properties）
 * # - QPS限流
 * # - 线程数限流
 * # - 热点参数限流
 * </pre>
 * 
 * @author Matrix RPC Team
 * @since 2026-01-11
 */
@Activate(group = {"CONSUMER", "PROVIDER"}, order = 40)
public class SentinelRateLimiterFilter implements Filter {
    
    private static final Logger logger = LoggerFactory.getLogger(SentinelRateLimiterFilter.class);
    private static final String FILTER_NAME = "sentinel.ratelimiter";
    
    @Override
    public Result invoke(Invoker<?> invoker, Invocation invocation) {
        if (!SentinelFilterConfig.isEnabled(FILTER_NAME)) {
            return invoker.invoke(invocation);
        }
        
        String resourceName = buildResourceName(invocation);
        
        Entry entry = null;
        try {
            // Sentinel限流检测
            entry = SphU.entry(resourceName, EntryType.IN);
            return invoker.invoke(invocation);
        } catch (BlockException e) {
            // 限流触发
            logger.warn("Rate limit exceeded for resource: {}", resourceName);
            return new Result(new RpcException(
                "Service rate limit exceeded: " + resourceName, e
            ));
        } finally {
            if (entry != null) {
                entry.exit();
            }
        }
    }
    
    private String buildResourceName(Invocation invocation) {
        return invocation.getServiceName() + "." + invocation.methodName();
    }
}
