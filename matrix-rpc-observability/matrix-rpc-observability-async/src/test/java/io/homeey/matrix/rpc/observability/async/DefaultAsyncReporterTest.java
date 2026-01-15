package io.homeey.matrix.rpc.observability.async;

import io.homeey.matrix.rpc.observability.api.AsyncReporter;
import io.homeey.matrix.rpc.observability.api.ReportEvent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;

/**
 * DefaultAsyncReporter 单元测试
 */
class DefaultAsyncReporterTest {

    private AsyncReporter<TestEvent> reporter;
    private AtomicInteger handledCount;

    @BeforeEach
    void setUp() {
        handledCount = new AtomicInteger(0);
        reporter = new DefaultAsyncReporter<>(event -> {
            handledCount.incrementAndGet();
            // 模拟事件处理（10ms）
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, 100, 2);
    }

    @AfterEach
    void tearDown() {
        if (reporter != null && !reporter.isShutdown()) {
            reporter.close();
        }
    }

    /**
     * 测试正常上报流程
     */
    @Test
    void testNormalReport() throws InterruptedException {
        // 上报 10 个事件
        for (int i = 0; i < 10; i++) {
            assertTrue(reporter.report(new TestEvent("event-" + i)));
        }

        // 等待处理完成
        Thread.sleep(200);

        // 验证
        assertEquals(10, reporter.getReportedCount());
        assertEquals(10, handledCount.get());
        assertEquals(0, reporter.getDroppedCount());
    }

    /**
     * 测试队列满时丢弃
     */
    @Test
    void testQueueFullDrop() throws InterruptedException {
        // 快速上报 200 个事件（超过队列容量 100）
        int totalEvents = 200;
        int successCount = 0;

        for (int i = 0; i < totalEvents; i++) {
            if (reporter.report(new TestEvent("event-" + i))) {
                successCount++;
            }
        }

        // 验证：成功入队数 + 丢弃数 = 总数
        long reported = reporter.getReportedCount();
        long dropped = reporter.getDroppedCount();

        assertEquals(totalEvents, reported + dropped);
        assertTrue(dropped > 0, "应该有事件被丢弃");
        System.out.printf("上报: %d, 丢弃: %d%n", reported, dropped);
    }

    /**
     * 测试 null 事件拒绝
     */
    @Test
    void testNullEventRejected() {
        assertFalse(reporter.report(null));
        assertEquals(0, reporter.getReportedCount());
    }

    /**
     * 测试优雅关闭
     */
    @Test
    void testGracefulShutdown() throws InterruptedException {
        // 上报 10 个事件
        for (int i = 0; i < 10; i++) {
            reporter.report(new TestEvent("event-" + i));
        }

        // 优雅关闭（等待 5 秒）
        boolean success = reporter.shutdown(5000);

        // 验证
        assertTrue(success, "应该在超时前完成关闭");
        assertTrue(reporter.isShutdown());
        assertEquals(10, handledCount.get(), "所有事件应该被处理");
    }

    /**
     * 测试关闭超时
     */
    @Test
    void testShutdownTimeout() throws InterruptedException {
        // 上报大量事件（处理时间超过关闭超时）
        for (int i = 0; i < 50; i++) {
            reporter.report(new TestEvent("event-" + i));
        }

        // 短超时关闭（100ms）
        boolean success = reporter.shutdown(100);

        // 验证
        assertFalse(success, "应该超时");
        assertTrue(reporter.isShutdown());
        // 部分事件应该被处理
        assertTrue(handledCount.get() > 0 && handledCount.get() < 50);
    }

    /**
     * 测试关闭后拒绝新事件
     */
    @Test
    void testRejectAfterShutdown() {
        reporter.shutdown(1000);

        // 尝试上报新事件
        assertFalse(reporter.report(new TestEvent("after-shutdown")));
        assertEquals(0, reporter.getReportedCount());
        assertEquals(1, reporter.getDroppedCount());
    }

    /**
     * 测试事件处理器异常不影响消费
     */
    @Test
    void testHandlerExceptionDoesNotStopConsumer() throws InterruptedException {
        AtomicInteger processedCount = new AtomicInteger(0);

        // 创建会抛异常的处理器
        AsyncReporter<TestEvent> faultyReporter = new DefaultAsyncReporter<>(event -> {
            processedCount.incrementAndGet();
            if (processedCount.get() % 2 == 0) {
                throw new RuntimeException("模拟处理异常");
            }
        }, 100, 1);

        try {
            // 上报 10 个事件
            for (int i = 0; i < 10; i++) {
                faultyReporter.report(new TestEvent("event-" + i));
            }

            // 等待处理
            Thread.sleep(200);

            // 验证：即使有异常，所有事件都应该被处理过（processedCount == 10）
            assertEquals(10, processedCount.get());
            assertEquals(10, faultyReporter.getReportedCount());

        } finally {
            faultyReporter.close();
        }
    }

    /**
     * 测试剩余容量
     */
    @Test
    void testRemainingCapacity() throws InterruptedException {
        int initialCapacity = reporter.remainingCapacity();
        assertTrue(initialCapacity > 0);

        // 上报 10 个事件
        for (int i = 0; i < 10; i++) {
            reporter.report(new TestEvent("event-" + i));
        }

        // 容量应该减少
        int currentCapacity = reporter.remainingCapacity();
        assertTrue(currentCapacity < initialCapacity);

        // 等待消费
        Thread.sleep(200);

        // 容量应该恢复
        assertTrue(reporter.remainingCapacity() > currentCapacity);
    }

    /**
     * 压力测试：高并发上报
     */
    @Test
    void testConcurrentReport() throws InterruptedException {
        int threadCount = 10;
        int eventsPerThread = 100;
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);
        AtomicLong totalReported = new AtomicLong(0);

        // 创建大容量 reporter
        AsyncReporter<TestEvent> stressReporter = new DefaultAsyncReporter<>(
                event -> {}, // 空处理器
                10000,
                4
        );

        try {
            // 启动多个线程并发上报
            for (int i = 0; i < threadCount; i++) {
                int threadId = i;
                new Thread(() -> {
                    try {
                        startLatch.await();
                        for (int j = 0; j < eventsPerThread; j++) {
                            if (stressReporter.report(new TestEvent("thread-" + threadId + "-event-" + j))) {
                                totalReported.incrementAndGet();
                            }
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    } finally {
                        doneLatch.countDown();
                    }
                }).start();
            }

            // 统一开始
            long startTime = System.nanoTime();
            startLatch.countDown();
            assertTrue(doneLatch.await(10, TimeUnit.SECONDS));
            long duration = System.nanoTime() - startTime;

            // 等待消费完成
            Thread.sleep(1000);

            // 验证
            long reported = stressReporter.getReportedCount();
            long dropped = stressReporter.getDroppedCount();

            System.out.printf("并发上报测试: 线程=%d, 每线程事件=%d, 总事件=%d%n", 
                    threadCount, eventsPerThread, threadCount * eventsPerThread);
            System.out.printf("结果: 成功=%d, 丢弃=%d, 耗时=%.2fms%n", 
                    reported, dropped, duration / 1_000_000.0);
            System.out.printf("吞吐量: %.2f events/ms%n", 
                    reported / (duration / 1_000_000.0));

            // 验证总数
            assertEquals(threadCount * eventsPerThread, reported + dropped);

        } finally {
            stressReporter.close();
        }
    }

    /**
     * 性能测试：测量写入延迟
     */
    @Test
    void testReportLatency() {
        // 创建空处理器（最小化处理开销）
        AsyncReporter<TestEvent> perfReporter = new DefaultAsyncReporter<>(
                event -> {}, 
                10000, 
                2
        );

        try {
            // 预热
            for (int i = 0; i < 1000; i++) {
                perfReporter.report(new TestEvent("warmup-" + i));
            }

            // 测量 10000 次写入延迟
            int iterations = 10000;
            long totalNanos = 0;

            for (int i = 0; i < iterations; i++) {
                long start = System.nanoTime();
                perfReporter.report(new TestEvent("perf-" + i));
                long end = System.nanoTime();
                totalNanos += (end - start);
            }

            double avgMicros = totalNanos / (double) iterations / 1000.0;
            System.out.printf("平均写入延迟: %.3f μs%n", avgMicros);

            // 验证：写入延迟应该 < 10μs（非常宽松的阈值）
            assertTrue(avgMicros < 10.0, 
                    String.format("写入延迟过高: %.3f μs", avgMicros));

        } finally {
            perfReporter.close();
        }
    }

    /**
     * 测试事件类
     */
    static class TestEvent extends ReportEvent {
        private final String data;

        public TestEvent(String data) {
            super(Type.METRICS);
            this.data = data;
        }

        @Override
        public int estimateSize() {
            return data != null ? data.length() : 0;
        }

        @Override
        public String toString() {
            return "TestEvent{" + data + '}';
        }
    }
}
