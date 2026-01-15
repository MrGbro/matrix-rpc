package io.homeey.matrix.rpc.observability.async;

import io.homeey.matrix.rpc.observability.api.AsyncReporter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

/**
 * 默认异步上报器实现
 * <p>
 * 核心特性：
 * - 基于有界队列（ArrayBlockingQueue）防止 OOM
 * - 非阻塞写入（< 1μs）：使用 offer() 语义
 * - 固定线程池消费（2个线程）
 * - 优雅关闭机制：等待队列消费完毕
 * - 完整的监控指标：成功计数、丢弃计数、队列容量
 *
 * @param <T> 事件类型
 */
public class DefaultAsyncReporter<T> implements AsyncReporter<T> {
    private static final Logger logger = LoggerFactory.getLogger(DefaultAsyncReporter.class);

    // 默认配置常量
    private static final int DEFAULT_QUEUE_CAPACITY = 10000;
    private static final int DEFAULT_CONSUMER_THREADS = 2;
    private static final long SHUTDOWN_TIMEOUT_MS = 5000;
    private static final long POLL_TIMEOUT_MS = 100;

    // 核心组件
    private final BlockingQueue<T> queue;
    private final ExecutorService consumerExecutor;
    private final Consumer<T> eventHandler;

    // 状态管理
    private final AtomicBoolean shutdown = new AtomicBoolean(false);
    private final AtomicLong reportedCount = new AtomicLong(0);
    private final AtomicLong droppedCount = new AtomicLong(0);

    /**
     * 构造函数（使用默认配置）
     *
     * @param eventHandler 事件处理器（由具体的 Metrics/Tracing 实现提供）
     */
    public DefaultAsyncReporter(Consumer<T> eventHandler) {
        this(eventHandler, DEFAULT_QUEUE_CAPACITY, DEFAULT_CONSUMER_THREADS);
    }

    /**
     * 构造函数（自定义配置）
     *
     * @param eventHandler     事件处理器
     * @param queueCapacity    队列容量
     * @param consumerThreads  消费线程数
     */
    public DefaultAsyncReporter(Consumer<T> eventHandler, int queueCapacity, int consumerThreads) {
        if (eventHandler == null) {
            throw new IllegalArgumentException("eventHandler cannot be null");
        }
        if (queueCapacity <= 0) {
            throw new IllegalArgumentException("queueCapacity must be positive");
        }
        if (consumerThreads <= 0) {
            throw new IllegalArgumentException("consumerThreads must be positive");
        }

        this.eventHandler = eventHandler;
        this.queue = new ArrayBlockingQueue<>(queueCapacity);
        this.consumerExecutor = Executors.newFixedThreadPool(
                consumerThreads,
                new ThreadFactory() {
                    private final AtomicLong counter = new AtomicLong(0);

                    @Override
                    public Thread newThread(Runnable r) {
                        Thread thread = new Thread(r, "async-reporter-" + counter.incrementAndGet());
                        thread.setDaemon(true); // 守护线程，不阻止 JVM 退出
                        return thread;
                    }
                }
        );

        // 启动消费线程
        for (int i = 0; i < consumerThreads; i++) {
            consumerExecutor.submit(this::consume);
        }

        logger.info("DefaultAsyncReporter started: queueCapacity={}, consumerThreads={}",
                queueCapacity, consumerThreads);
    }

    @Override
    public boolean report(T event) {
        if (event == null) {
            return false;
        }

        if (shutdown.get()) {
            droppedCount.incrementAndGet();
            return false;
        }

        // 非阻塞写入：offer() 语义（< 1μs）
        boolean success = queue.offer(event);
        if (success) {
            reportedCount.incrementAndGet();
        } else {
            droppedCount.incrementAndGet();
            // 可选：每 1000 次丢弃打印一次警告
            long dropped = droppedCount.get();
            if (dropped % 1000 == 0) {
                logger.warn("AsyncReporter queue full, dropped {} events so far", dropped);
            }
        }

        return success;
    }

    @Override
    public int remainingCapacity() {
        return queue.remainingCapacity();
    }

    @Override
    public long getDroppedCount() {
        return droppedCount.get();
    }

    @Override
    public long getReportedCount() {
        return reportedCount.get();
    }

    @Override
    public boolean shutdown(long timeoutMs) {
        if (!shutdown.compareAndSet(false, true)) {
            return false; // 已经关闭
        }

        logger.info("Shutting down AsyncReporter, remaining events: {}", queue.size());

        // 关闭消费线程池（不再接受新任务）
        consumerExecutor.shutdown();

        try {
            // 等待队列消费完毕
            boolean terminated = consumerExecutor.awaitTermination(timeoutMs, TimeUnit.MILLISECONDS);
            if (!terminated) {
                logger.warn("AsyncReporter shutdown timeout, forcing shutdown. Remaining events: {}", queue.size());
                consumerExecutor.shutdownNow();
                return false;
            }

            logger.info("AsyncReporter shutdown successfully. Total reported: {}, dropped: {}",
                    reportedCount.get(), droppedCount.get());
            return true;

        } catch (InterruptedException e) {
            logger.error("AsyncReporter shutdown interrupted", e);
            consumerExecutor.shutdownNow();
            Thread.currentThread().interrupt();
            return false;
        }
    }

    @Override
    public boolean isShutdown() {
        return shutdown.get();
    }

    /**
     * 消费线程逻辑
     */
    private void consume() {
        logger.debug("Consumer thread started: {}", Thread.currentThread().getName());

        while (!shutdown.get() || !queue.isEmpty()) {
            try {
                // 使用 poll() 带超时，避免阻塞在 take() 导致无法感知 shutdown
                T event = queue.poll(POLL_TIMEOUT_MS, TimeUnit.MILLISECONDS);
                if (event != null) {
                    try {
                        eventHandler.accept(event);
                    } catch (Exception e) {
                        logger.error("Failed to handle event: {}", event, e);
                        // 继续处理下一个事件，不中断消费线程
                    }
                }
            } catch (InterruptedException e) {
                logger.warn("Consumer thread interrupted: {}", Thread.currentThread().getName());
                Thread.currentThread().interrupt();
                break;
            }
        }

        logger.debug("Consumer thread stopped: {}", Thread.currentThread().getName());
    }
}
