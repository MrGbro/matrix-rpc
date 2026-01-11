package io.homeey.matrix.rpc.filter.resilience4j;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
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
 * Resilience4j熔断器Filter
 * 
 * <p>配置示例:
 * <pre>
 * # 启用熔断（默认启用）
 * matrix.filter.resilience4j.circuitbreaker.enabled=true
 * 
 * # 熔断配置（通过resilience4j-config.properties）
 * resilience4j.circuitbreaker.failureRateThreshold=50
 * resilience4j.circuitbreaker.slowCallDurationThreshold=1000
 * resilience4j.circuitbreaker.slowCallRateThreshold=50
 * resilience4j.circuitbreaker.slidingWindowSize=10
 * resilience4j.circuitbreaker.minimumNumberOfCalls=5
 * resilience4j.circuitbreaker.waitDurationInOpenState=10
 * </pre>
 * 
 * @author Matrix RPC Team
 * @since 2026-01-11
 */
@Activate(group = {"CONSUMER", "PROVIDER"}, order = 50)
public class Resilience4jCircuitBreakerFilter implements Filter {
    
    private static final Logger logger = LoggerFactory.getLogger(Resilience4jCircuitBreakerFilter.class);
    private static final String FILTER_NAME = "resilience4j.circuitbreaker";
    private final CircuitBreakerRegistry registry = Resilience4jConfigLoader.getCircuitBreakerRegistry();
    
    @Override
    public Result invoke(Invoker<?> invoker, Invocation invocation) {
        if (!Resilience4jFilterConfig.isEnabled(FILTER_NAME)) {
            return invoker.invoke(invocation);
        }
        
        String resourceName = buildResourceName(invocation);
        CircuitBreaker circuitBreaker = registry.circuitBreaker(resourceName);
        
        // 使用Resilience4j装饰器模式
        Supplier<Result> decoratedSupplier = CircuitBreaker.decorateSupplier(
            circuitBreaker,
            () -> {
                Result result = invoker.invoke(invocation);
                // 如果结果有异常，记录失败
                if (result.hasException()) {
                    throw new RuntimeException(result.getException());
                }
                return result;
            }
        );
        
        try {
            return decoratedSupplier.get();
        } catch (CallNotPermittedException e) {
            // 熔断器打开，调用被阻止
            logger.warn("Circuit breaker open for resource: {}", resourceName);
            return new Result(new RpcException(
                "Service circuit breaker open: " + resourceName, e
            ));
        } catch (Exception e) {
            // 业务异常或其他错误
            logger.error("CircuitBreaker error for resource: {}", resourceName, e);
            
            // 如果是业务异常，提取原始异常
            Throwable cause = e.getCause();
            if (cause != null) {
                return new Result(cause);
            }
            
            return new Result(new RpcException(
                "CircuitBreaker error: " + resourceName, e
            ));
        }
    }
    
    private String buildResourceName(Invocation invocation) {
        return invocation.getServiceName() + "." + invocation.methodName();
    }
}
