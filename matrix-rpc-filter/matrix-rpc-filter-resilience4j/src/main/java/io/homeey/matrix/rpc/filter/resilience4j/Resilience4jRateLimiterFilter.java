package io.homeey.matrix.rpc.filter.resilience4j;

import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import io.github.resilience4j.ratelimiter.RequestNotPermitted;
import io.homeey.matrix.rpc.core.Invocation;
import io.homeey.matrix.rpc.core.Invoker;
import io.homeey.matrix.rpc.core.Result;
import io.homeey.matrix.rpc.core.RpcException;
import io.homeey.matrix.rpc.filter.Filter;
import io.homeey.matrix.rpc.spi.Activate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Supplier;

/**
 * Resilience4j限流器Filter
 * 
 * <p>配置示例:
 * <pre>
 * # 启用限流（默认启用）
 * matrix.filter.resilience4j.ratelimiter.enabled=true
 * 
 * # 限流配置（通过resilience4j-config.properties）
 * resilience4j.ratelimiter.limitForPeriod=100
 * resilience4j.ratelimiter.limitRefreshPeriod=1
 * resilience4j.ratelimiter.timeoutDuration=0
 * </pre>
 * 
 * @author Matrix RPC Team
 * @since 2026-01-11
 */
@Activate(group = {"CONSUMER", "PROVIDER"}, order = 40)
public class Resilience4jRateLimiterFilter implements Filter {
    
    private static final Logger logger = LoggerFactory.getLogger(Resilience4jRateLimiterFilter.class);
    private static final String FILTER_NAME = "resilience4j.ratelimiter";
    private final RateLimiterRegistry registry = Resilience4jConfigLoader.getRateLimiterRegistry();
    
    @Override
    public Result invoke(Invoker<?> invoker, Invocation invocation) {
        if (!Resilience4jFilterConfig.isEnabled(FILTER_NAME)) {
            return invoker.invoke(invocation);
        }
        
        String resourceName = buildResourceName(invocation);
        RateLimiter rateLimiter = registry.rateLimiter(resourceName);
        
        // 使用Resilience4j装饰器模式
        Supplier<Result> decoratedSupplier = RateLimiter.decorateSupplier(
            rateLimiter,
            () -> invoker.invoke(invocation)
        );
        
        try {
            return decoratedSupplier.get();
        } catch (RequestNotPermitted e) {
            // 限流触发
            logger.warn("Rate limit exceeded for resource: {}", resourceName);
            return new Result(new RpcException(
                "Service rate limit exceeded: " + resourceName, e
            ));
        } catch (Exception e) {
            logger.error("RateLimiter error for resource: {}", resourceName, e);
            return new Result(new RpcException(
                "RateLimiter error: " + resourceName, e
            ));
        }
    }
    
    private String buildResourceName(Invocation invocation) {
        return invocation.getServiceName() + "." + invocation.methodName();
    }
}
