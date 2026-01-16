package io.homeey.matrix.rpc.observability.api;

/**
 * 可观测性性能配置
 * <p>
 * 统一管理所有可观测性模块的性能参数，包括：
 * - 队列容量
 * - 消费线程数
 * - 批量处理大小
 * - 超时配置
 * <p>
 * 设计原则：
 * - 默认值适用于大多数场景
 * - 支持动态调整
 * - 性能优先，开销最小化
 */
public class ObservabilityConfig {

    // ========== 队列配置 ==========
    
    /**
     * 默认队列容量（10000 条事件）
     * <p>
     * 计算依据：
     * - 平均每个事件 500 字节
     * - 10000 * 500 = 5MB 内存
     * - 适用于 QPS 10000 的场景（1秒缓冲）
     */
    public static final int DEFAULT_QUEUE_CAPACITY = 10000;

    /**
     * 最小队列容量
     */
    public static final int MIN_QUEUE_CAPACITY = 1000;

    /**
     * 最大队列容量
     */
    public static final int MAX_QUEUE_CAPACITY = 100000;

    // ========== 线程配置 ==========

    /**
     * 默认消费线程数（2 个）
     * <p>
     * 计算依据：
     * - 每个线程处理 5000 QPS
     * - 2 个线程支持 10000 QPS
     * - 避免线程过多导致上下文切换
     */
    public static final int DEFAULT_CONSUMER_THREADS = 2;

    /**
     * 最小消费线程数
     */
    public static final int MIN_CONSUMER_THREADS = 1;

    /**
     * 最大消费线程数
     */
    public static final int MAX_CONSUMER_THREADS = 8;

    // ========== 批量处理配置 ==========

    /**
     * 默认批量处理大小（100 条）
     * <p>
     * 批量处理可以提升吞吐量，但会增加延迟
     * 对于可观测性数据，延迟不敏感，可以使用批量处理
     */
    public static final int DEFAULT_BATCH_SIZE = 100;

    /**
     * 最小批量处理大小
     */
    public static final int MIN_BATCH_SIZE = 10;

    /**
     * 最大批量处理大小
     */
    public static final int MAX_BATCH_SIZE = 1000;

    // ========== 超时配置 ==========

    /**
     * 默认关闭超时（10 秒）
     * <p>
     * 用于优雅关闭时等待队列清空的时间
     */
    public static final int DEFAULT_SHUTDOWN_TIMEOUT_SECONDS = 10;

    /**
     * 最小关闭超时
     */
    public static final int MIN_SHUTDOWN_TIMEOUT_SECONDS = 1;

    /**
     * 最大关闭超时
     */
    public static final int MAX_SHUTDOWN_TIMEOUT_SECONDS = 60;

    // ========== 性能监控配置 ==========

    /**
     * 是否启用性能监控（默认启用）
     * <p>
     * 监控指标包括：
     * - 队列长度
     * - 处理速率
     * - 丢弃数量
     * - 延迟分布
     */
    public static final boolean DEFAULT_METRICS_ENABLED = true;

    /**
     * 监控指标上报间隔（秒）
     */
    public static final int DEFAULT_METRICS_REPORT_INTERVAL_SECONDS = 60;

    // ========== 性能优化配置 ==========

    /**
     * 是否启用对象池（默认禁用）
     * <p>
     * 对象池可以减少 GC 压力，但会增加代码复杂度
     * 只有在高 QPS 场景下才建议启用
     */
    public static final boolean DEFAULT_OBJECT_POOL_ENABLED = false;

    /**
     * 对象池大小
     */
    public static final int DEFAULT_OBJECT_POOL_SIZE = 1000;

    // ========== 工具方法 ==========

    /**
     * 验证队列容量
     */
    public static int validateQueueCapacity(int capacity) {
        if (capacity < MIN_QUEUE_CAPACITY) {
            return MIN_QUEUE_CAPACITY;
        }
        if (capacity > MAX_QUEUE_CAPACITY) {
            return MAX_QUEUE_CAPACITY;
        }
        return capacity;
    }

    /**
     * 验证消费线程数
     */
    public static int validateConsumerThreads(int threads) {
        if (threads < MIN_CONSUMER_THREADS) {
            return MIN_CONSUMER_THREADS;
        }
        if (threads > MAX_CONSUMER_THREADS) {
            return MAX_CONSUMER_THREADS;
        }
        return threads;
    }

    /**
     * 验证批量处理大小
     */
    public static int validateBatchSize(int batchSize) {
        if (batchSize < MIN_BATCH_SIZE) {
            return MIN_BATCH_SIZE;
        }
        if (batchSize > MAX_BATCH_SIZE) {
            return MAX_BATCH_SIZE;
        }
        return batchSize;
    }

    /**
     * 验证关闭超时
     */
    public static int validateShutdownTimeout(int timeout) {
        if (timeout < MIN_SHUTDOWN_TIMEOUT_SECONDS) {
            return MIN_SHUTDOWN_TIMEOUT_SECONDS;
        }
        if (timeout > MAX_SHUTDOWN_TIMEOUT_SECONDS) {
            return MAX_SHUTDOWN_TIMEOUT_SECONDS;
        }
        return timeout;
    }

    /**
     * 根据 QPS 推荐队列容量
     * <p>
     * 计算公式：队列容量 = QPS * 缓冲时间（秒）
     * 
     * @param qps 每秒请求数
     * @param bufferSeconds 缓冲时间（秒），默认 1 秒
     * @return 推荐的队列容量
     */
    public static int recommendQueueCapacity(int qps, int bufferSeconds) {
        int capacity = qps * bufferSeconds;
        return validateQueueCapacity(capacity);
    }

    /**
     * 根据 QPS 推荐消费线程数
     * <p>
     * 计算公式：线程数 = QPS / 每线程处理能力
     * 假设每个线程处理能力为 5000 QPS
     * 
     * @param qps 每秒请求数
     * @return 推荐的消费线程数
     */
    public static int recommendConsumerThreads(int qps) {
        int threads = (qps + 4999) / 5000; // 向上取整
        return validateConsumerThreads(threads);
    }

    // 私有构造函数，防止实例化
    private ObservabilityConfig() {
    }
}
