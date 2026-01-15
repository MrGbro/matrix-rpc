package io.homeey.matrix.rpc.observability.micrometer;

import io.homeey.matrix.rpc.observability.api.AsyncReporter;
import io.homeey.matrix.rpc.observability.async.DefaultAsyncReporter;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Micrometer 指标收集器
 * <p>
 * 核心职责：
 * 1. 消费 MetricsEvent 事件
 * 2. 将事件转换为 Micrometer 指标（Timer、Counter）
 * 3. 支持多维度标签（service、method、side、status）
 * 4. 自动计算 P99、P95、P50 等百分位数
 * <p>
 * 指标命名：
 * - rpc.request.duration: 请求耗时（Timer，包含百分位数）
 * - rpc.request.total: 请求总数（Counter）
 * - rpc.request.errors: 错误总数（Counter）
 */
public class MicrometerMetricsCollector implements AutoCloseable {
    private static final Logger logger = LoggerFactory.getLogger(MicrometerMetricsCollector.class);

    // 指标名称常量
    private static final String METRIC_REQUEST_DURATION = "rpc.request.duration";
    private static final String METRIC_REQUEST_TOTAL = "rpc.request.total";
    private static final String METRIC_REQUEST_ERRORS = "rpc.request.errors";

    // 标签 Key 常量
    private static final String TAG_SERVICE = "service";
    private static final String TAG_METHOD = "method";
    private static final String TAG_SIDE = "side";
    private static final String TAG_STATUS = "status";
    private static final String TAG_ERROR_TYPE = "error_type";

    // Micrometer Registry
    private final MeterRegistry registry;

    // 异步上报器
    private final AsyncReporter<MetricsEvent> reporter;

    // Timer 缓存（避免重复创建）
    private final ConcurrentHashMap<String, Timer> timerCache = new ConcurrentHashMap<>();

    // Counter 缓存
    private final ConcurrentHashMap<String, Counter> counterCache = new ConcurrentHashMap<>();

    /**
     * 构造函数
     *
     * @param registry Micrometer MeterRegistry
     */
    public MicrometerMetricsCollector(MeterRegistry registry) {
        this(registry, 10000, 2);
    }

    /**
     * 构造函数（可配置队列容量和线程数）
     *
     * @param registry       Micrometer MeterRegistry
     * @param queueCapacity  队列容量
     * @param consumerThreads 消费线程数
     */
    public MicrometerMetricsCollector(MeterRegistry registry, int queueCapacity, int consumerThreads) {
        if (registry == null) {
            throw new IllegalArgumentException("MeterRegistry cannot be null");
        }
        this.registry = registry;

        // 创建异步上报器，事件处理器为 recordMetrics
        this.reporter = new DefaultAsyncReporter<>(
                this::recordMetrics,
                queueCapacity,
                consumerThreads
        );

        logger.info("MicrometerMetricsCollector initialized: queueCapacity={}, consumerThreads={}",
                queueCapacity, consumerThreads);
    }

    /**
     * 上报指标事件（非阻塞）
     *
     * @param event MetricsEvent
     * @return true=成功入队, false=队列满（已丢弃）
     */
    public boolean report(MetricsEvent event) {
        return reporter.report(event);
    }

    /**
     * 记录指标到 Micrometer（由异步线程调用）
     *
     * @param event MetricsEvent
     */
    private void recordMetrics(MetricsEvent event) {
        try {
            // 1. 记录请求耗时（Timer）
            Timer timer = getOrCreateTimer(event);
            timer.record(Duration.ofMillis(event.getDuration()));

            // 2. 记录请求总数（Counter）
            Counter totalCounter = getOrCreateTotalCounter(event);
            totalCounter.increment();

            // 3. 如果失败，记录错误数（Counter）
            if (!event.isSuccess()) {
                Counter errorCounter = getOrCreateErrorCounter(event);
                errorCounter.increment();
            }

        } catch (Exception e) {
            logger.error("Failed to record metrics for event: {}", event, e);
            // 不抛异常，避免影响异步上报器
        }
    }

    /**
     * 获取或创建 Timer（带缓存）
     */
    private Timer getOrCreateTimer(MetricsEvent event) {
        String cacheKey = buildCacheKey(METRIC_REQUEST_DURATION, event.getServiceName(),
                event.getMethodName(), event.getSide(), event.isSuccess() ? "success" : "failure");

        return timerCache.computeIfAbsent(cacheKey, k ->
                Timer.builder(METRIC_REQUEST_DURATION)
                        .description("RPC request duration")
                        .tags(buildTags(event))
                        .publishPercentiles(0.5, 0.95, 0.99) // P50, P95, P99
                        .register(registry)
        );
    }

    /**
     * 获取或创建 Total Counter（带缓存）
     */
    private Counter getOrCreateTotalCounter(MetricsEvent event) {
        String cacheKey = buildCacheKey(METRIC_REQUEST_TOTAL, event.getServiceName(),
                event.getMethodName(), event.getSide(), event.isSuccess() ? "success" : "failure");

        return counterCache.computeIfAbsent(cacheKey, k ->
                Counter.builder(METRIC_REQUEST_TOTAL)
                        .description("RPC request total count")
                        .tags(buildTags(event))
                        .register(registry)
        );
    }

    /**
     * 获取或创建 Error Counter（带缓存）
     */
    private Counter getOrCreateErrorCounter(MetricsEvent event) {
        String cacheKey = buildCacheKey(METRIC_REQUEST_ERRORS, event.getServiceName(),
                event.getMethodName(), event.getSide(),
                event.getErrorType() != null ? event.getErrorType() : "unknown");

        return counterCache.computeIfAbsent(cacheKey, k -> {
            List<Tag> tags = new ArrayList<>(buildTags(event));
            if (event.getErrorType() != null) {
                tags.add(Tag.of(TAG_ERROR_TYPE, event.getErrorType()));
            }
            return Counter.builder(METRIC_REQUEST_ERRORS)
                    .description("RPC request error count")
                    .tags(tags)
                    .register(registry);
        });
    }

    /**
     * 构建标签列表
     */
    private List<Tag> buildTags(MetricsEvent event) {
        List<Tag> tags = new ArrayList<>();
        tags.add(Tag.of(TAG_SERVICE, event.getServiceName()));
        tags.add(Tag.of(TAG_METHOD, event.getMethodName()));
        tags.add(Tag.of(TAG_SIDE, event.getSide()));
        tags.add(Tag.of(TAG_STATUS, event.isSuccess() ? "success" : "failure"));

        // 添加自定义标签
        if (event.getTags() != null) {
            event.getTags().forEach((key, value) ->
                    tags.add(Tag.of(key, value))
            );
        }

        return tags;
    }

    /**
     * 构建缓存 Key
     */
    private String buildCacheKey(String metricName, String service, String method, String side, String status) {
        return metricName + ":" + service + ":" + method + ":" + side + ":" + status;
    }

    /**
     * 获取异步上报器统计信息
     */
    public long getReportedCount() {
        return reporter.getReportedCount();
    }

    public long getDroppedCount() {
        return reporter.getDroppedCount();
    }

    public int getRemainingCapacity() {
        return reporter.remainingCapacity();
    }

    /**
     * 优雅关闭
     */
    @Override
    public void close() {
        logger.info("Shutting down MicrometerMetricsCollector...");
        reporter.close();
        logger.info("MicrometerMetricsCollector shutdown completed. Reported: {}, Dropped: {}",
                getReportedCount(), getDroppedCount());
    }
}
