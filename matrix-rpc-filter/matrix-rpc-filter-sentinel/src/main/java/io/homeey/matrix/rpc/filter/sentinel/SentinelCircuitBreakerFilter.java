package io.homeey.matrix.rpc.filter.sentinel;

import com.alibaba.csp.sentinel.Entry;
import com.alibaba.csp.sentinel.EntryType;
import com.alibaba.csp.sentinel.SphU;
import com.alibaba.csp.sentinel.Tracer;
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
 * Sentinel熔断器Filter
 * 
 * <p>配置示例:
 * <pre>
 * # 启用熔断（默认启用）
 * matrix.filter.sentinel.circuitbreaker.enabled=true
 * 
 * # 熔断规则（通过Sentinel Dashboard配置或sentinel-rules.properties）
 * # - 慢调用比例策略
 * # - 异常比例策略
 * # - 异常数策略
 * </pre>
 * 
 * @author Matrix RPC Team
 * @since 2026-01-11
 */
@Activate(group = {"CONSUMER", "PROVIDER"}, order = 50)
public class SentinelCircuitBreakerFilter implements Filter {
    
    private static final Logger logger = LoggerFactory.getLogger(SentinelCircuitBreakerFilter.class);
    private static final String FILTER_NAME = "sentinel.circuitbreaker";
    
    @Override
    public Result invoke(Invoker<?> invoker, Invocation invocation) {
        // 检查开关
        if (!SentinelFilterConfig.isEnabled(FILTER_NAME)) {
            return invoker.invoke(invocation);
        }
        
        // 构建资源名: interface.method
        String resourceName = buildResourceName(invocation);
        
        Entry entry = null;
        try {
            // Sentinel熔断检测
            entry = SphU.entry(resourceName, EntryType.OUT);
            
            // 执行RPC调用
            Result result = invoker.invoke(invocation);
            
            // 如果结果有异常，记录到Sentinel（用于异常比例熔断）
            if (result.hasException()) {
                Tracer.trace(result.getException());
            }
            
            return result;
        } catch (BlockException e) {
            // 熔断触发，返回降级响应
            logger.warn("Circuit breaker triggered for resource: {}", resourceName);
            return new Result(new RpcException(
                "Service circuit breaker triggered: " + resourceName, e
            ));
        } catch (Throwable t) {
            // 其他异常也记录到Sentinel
            Tracer.trace(t);
            throw t;
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
