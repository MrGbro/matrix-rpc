package io.homeey.matrix.rpc.observability.micrometer;

import io.homeey.matrix.rpc.core.Invocation;
import io.homeey.matrix.rpc.core.Invoker;
import io.homeey.matrix.rpc.core.Result;
import io.homeey.matrix.rpc.filter.Filter;
import io.homeey.matrix.rpc.spi.Activate;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Micrometer Metrics Filter
 * <p>
 * 集成到 Matrix RPC 的 Filter 链，自动收集 RPC 调用指标：
 * - 请求耗时（Timer）
 * - 请求总数（Counter）
 * - 错误总数（Counter）
 * <p>
 * 激活条件：
 * - order=100：在 AccessLog 之后执行
 * - 默认激活（不需要额外配置）
 * <p>
 * 使用方式：
 * 1. 默认使用 SimpleMeterRegistry（内存）
 * 2. 可通过静态方法设置自定义 MeterRegistry（如 Prometheus）
 */
@Activate(order = 100)
public class MicrometerFilter implements Filter {
    private static final Logger logger = LoggerFactory.getLogger(MicrometerFilter.class);

    // 全局 MeterRegistry（可由用户设置）
    private static volatile MeterRegistry globalRegistry = null;

    // 指标收集器（懒加载）
    private static volatile MicrometerMetricsCollector collector = null;

    // 初始化锁
    private static final Object INIT_LOCK = new Object();

    /**
     * 设置全局 MeterRegistry（应用启动时调用）
     * <p>
     * 示例：
     * <pre>
     * PrometheusMeterRegistry registry = new PrometheusMeterRegistry(PrometheusConfig.DEFAULT);
     * MicrometerFilter.setMeterRegistry(registry);
     * </pre>
     *
     * @param registry MeterRegistry
     */
    public static void setMeterRegistry(MeterRegistry registry) {
        if (registry == null) {
            throw new IllegalArgumentException("MeterRegistry cannot be null");
        }
        globalRegistry = registry;
        logger.info("MeterRegistry set to: {}", registry.getClass().getSimpleName());

        // 如果 collector 已创建，需要重新初始化
        synchronized (INIT_LOCK) {
            if (collector != null) {
                logger.info("Closing existing MicrometerMetricsCollector...");
                collector.close();
                collector = null;
            }
        }
    }

    /**
     * 获取或创建 MeterRegistry
     */
    private static MeterRegistry getOrCreateRegistry() {
        if (globalRegistry == null) {
            synchronized (INIT_LOCK) {
                if (globalRegistry == null) {
                    logger.info("No MeterRegistry configured, using SimpleMeterRegistry");
                    globalRegistry = new SimpleMeterRegistry();
                }
            }
        }
        return globalRegistry;
    }

    /**
     * 获取或创建 MetricsCollector
     */
    private static MicrometerMetricsCollector getOrCreateCollector() {
        if (collector == null) {
            synchronized (INIT_LOCK) {
                if (collector == null) {
                    MeterRegistry registry = getOrCreateRegistry();
                    collector = new MicrometerMetricsCollector(registry);
                    logger.info("MicrometerMetricsCollector created");
                }
            }
        }
        return collector;
    }

    /**
     * 获取 MetricsCollector（用于测试或监控）
     */
    public static MicrometerMetricsCollector getCollector() {
        return getOrCreateCollector();
    }

    @Override
    public Result invoke(Invoker<?> invoker, Invocation invocation) {
        long startTime = System.currentTimeMillis();
        Result result = null;
        Throwable exception = null;

        try {
            // 执行下一个 Filter 或 Invoker
            result = invoker.invoke(invocation);
            return result;

        } catch (Throwable e) {
            exception = e;
            throw e;

        } finally {
            // 计算耗时
            long duration = System.currentTimeMillis() - startTime;

            // 提取服务和方法信息
            String serviceName = invocation.getServiceName();
            String methodName = invocation.methodName();
            String side = invocation.getAttachments().getOrDefault("side", "unknown"); // consumer/provider

            // 判断是否成功
            boolean success = (exception == null) &&
                    (result == null || !result.hasException());

            // 提取错误类型
            String errorType = null;
            if (!success) {
                if (exception != null) {
                    errorType = exception.getClass().getSimpleName();
                } else if (result != null && result.hasException()) {
                    errorType = result.getException().getClass().getSimpleName();
                }
            }

            // 构建 MetricsEvent
            MetricsEvent event = MetricsEvent.builder()
                    .serviceName(serviceName)
                    .methodName(methodName)
                    .side(side)
                    .duration(duration)
                    .success(success)
                    .errorType(errorType)
                    .build();

            // 异步上报（非阻塞）
            MicrometerMetricsCollector metricsCollector = getOrCreateCollector();
            boolean reported = metricsCollector.report(event);

            if (!reported) {
                // 队列满，丢弃（已在 AsyncReporter 中统计）
                logger.debug("Metrics event dropped due to queue full: {}", event);
            }
        }
    }
}
