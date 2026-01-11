package io.homeey.matrix.rpc.filter.resilience4j;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.time.Duration;
import java.util.Properties;

/**
 * Resilience4j配置加载器
 * 
 * <p>从 resilience4j-config.properties 加载配置
 * 
 * @author Matrix RPC Team
 * @since 2026-01-11
 */
public class Resilience4jConfigLoader {
    
    private static final Logger logger = LoggerFactory.getLogger(Resilience4jConfigLoader.class);
    private static final String CONFIG_FILE = "resilience4j-config.properties";
    
    private static final CircuitBreakerRegistry circuitBreakerRegistry;
    private static final RateLimiterRegistry rateLimiterRegistry;
    
    static {
        // 加载配置
        Properties props = loadConfig();
        
        // 初始化熔断器Registry
        CircuitBreakerConfig circuitBreakerConfig = buildCircuitBreakerConfig(props);
        circuitBreakerRegistry = CircuitBreakerRegistry.of(circuitBreakerConfig);
        logger.info("Resilience4j CircuitBreaker initialized");
        
        // 初始化限流器Registry
        RateLimiterConfig rateLimiterConfig = buildRateLimiterConfig(props);
        rateLimiterRegistry = RateLimiterRegistry.of(rateLimiterConfig);
        logger.info("Resilience4j RateLimiter initialized");
    }
    
    /**
     * 获取熔断器Registry
     */
    public static CircuitBreakerRegistry getCircuitBreakerRegistry() {
        return circuitBreakerRegistry;
    }
    
    /**
     * 获取限流器Registry
     */
    public static RateLimiterRegistry getRateLimiterRegistry() {
        return rateLimiterRegistry;
    }
    
    /**
     * 加载配置文件
     */
    private static Properties loadConfig() {
        Properties props = new Properties();
        try (InputStream is = Resilience4jConfigLoader.class
                .getClassLoader()
                .getResourceAsStream(CONFIG_FILE)) {
            
            if (is != null) {
                props.load(is);
                logger.info("Loaded resilience4j config from {}", CONFIG_FILE);
            } else {
                logger.info("No {} found, using default config", CONFIG_FILE);
            }
        } catch (Exception e) {
            logger.error("Failed to load resilience4j config", e);
        }
        return props;
    }
    
    /**
     * 构建熔断器配置
     */
    private static CircuitBreakerConfig buildCircuitBreakerConfig(Properties props) {
        // 失败率阈值
        float failureRateThreshold = Float.parseFloat(
            props.getProperty("resilience4j.circuitbreaker.failureRateThreshold", "50")
        );
        
        // 慢调用阈值（毫秒）
        long slowCallDurationThreshold = Long.parseLong(
            props.getProperty("resilience4j.circuitbreaker.slowCallDurationThreshold", "1000")
        );
        
        // 慢调用率阈值
        float slowCallRateThreshold = Float.parseFloat(
            props.getProperty("resilience4j.circuitbreaker.slowCallRateThreshold", "50")
        );
        
        // 滑动窗口大小
        int slidingWindowSize = Integer.parseInt(
            props.getProperty("resilience4j.circuitbreaker.slidingWindowSize", "10")
        );
        
        // 最小调用次数
        int minimumNumberOfCalls = Integer.parseInt(
            props.getProperty("resilience4j.circuitbreaker.minimumNumberOfCalls", "5")
        );
        
        // 半开状态允许的调用次数
        int permittedNumberOfCallsInHalfOpenState = Integer.parseInt(
            props.getProperty("resilience4j.circuitbreaker.permittedNumberOfCallsInHalfOpenState", "3")
        );
        
        // 等待时长（秒）
        long waitDurationInOpenState = Long.parseLong(
            props.getProperty("resilience4j.circuitbreaker.waitDurationInOpenState", "10")
        );
        
        logger.debug("CircuitBreaker config: failureRate={}%, slowCallRate={}%, windowSize={}", 
            failureRateThreshold, slowCallRateThreshold, slidingWindowSize);
        
        return CircuitBreakerConfig.custom()
            .failureRateThreshold(failureRateThreshold)
            .slowCallDurationThreshold(Duration.ofMillis(slowCallDurationThreshold))
            .slowCallRateThreshold(slowCallRateThreshold)
            .slidingWindowSize(slidingWindowSize)
            .minimumNumberOfCalls(minimumNumberOfCalls)
            .permittedNumberOfCallsInHalfOpenState(permittedNumberOfCallsInHalfOpenState)
            .waitDurationInOpenState(Duration.ofSeconds(waitDurationInOpenState))
            .build();
    }
    
    /**
     * 构建限流器配置
     */
    private static RateLimiterConfig buildRateLimiterConfig(Properties props) {
        // 限流周期（秒）
        long limitRefreshPeriod = Long.parseLong(
            props.getProperty("resilience4j.ratelimiter.limitRefreshPeriod", "1")
        );
        
        // 每个周期允许的请求数
        int limitForPeriod = Integer.parseInt(
            props.getProperty("resilience4j.ratelimiter.limitForPeriod", "100")
        );
        
        // 超时等待时长（毫秒）
        long timeoutDuration = Long.parseLong(
            props.getProperty("resilience4j.ratelimiter.timeoutDuration", "0")
        );
        
        logger.debug("RateLimiter config: limit={}/{}s, timeout={}ms", 
            limitForPeriod, limitRefreshPeriod, timeoutDuration);
        
        return RateLimiterConfig.custom()
            .limitRefreshPeriod(Duration.ofSeconds(limitRefreshPeriod))
            .limitForPeriod(limitForPeriod)
            .timeoutDuration(Duration.ofMillis(timeoutDuration))
            .build();
    }
}
