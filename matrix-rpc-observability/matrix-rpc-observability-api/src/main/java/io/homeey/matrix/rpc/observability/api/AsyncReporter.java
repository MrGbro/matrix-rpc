package io.homeey.matrix.rpc.observability.api;

import io.homeey.matrix.rpc.spi.SPI;

/**
 * 异步上报器接口
 * 
 * <p>核心设计原则：
 * <ul>
 *   <li>非阻塞：report() 方法必须立即返回（< 1μs）</li>
 *   <li>有界队列：防止内存溢出</li>
 *   <li>丢弃策略：队列满时直接丢弃，不阻塞主路径</li>
 *   <li>优雅关闭：JVM 关闭时等待队列消费完毕</li>
 * </ul>
 * 
 * <p>性能指标：
 * <ul>
 *   <li>写入延迟：< 1μs（非阻塞入队）</li>
 *   <li>丢弃率：< 0.1%（正常流量下）</li>
 *   <li>内存开销：队列大小 * 事件大小</li>
 * </ul>
 * 
 * @param <T> 上报事件类型
 * @author Matrix RPC Team
 * @since 0.0.7
 */
@SPI("default")
public interface AsyncReporter<T> extends AutoCloseable {
    
    /**
     * 异步提交数据（非阻塞）
     * 
     * <p>此方法必须立即返回，不能阻塞调用线程。
     * 如果队列已满，应直接丢弃数据并返回 false。
     * 
     * @param event 上报事件，不能为 null
     * @return true=成功入队, false=队列满（已丢弃）
     * @throws NullPointerException 如果 event 为 null
     */
    boolean report(T event);
    
    /**
     * 获取队列剩余容量
     * 
     * <p>用于监控队列状态，判断是否接近满载。
     * 
     * @return 剩余容量，0 表示队列已满
     */
    int remainingCapacity();
    
    /**
     * 获取丢弃计数（用于监控）
     * 
     * <p>累计从创建以来被丢弃的事件数量。
     * 
     * @return 累计丢弃数量
     */
    long getDroppedCount();
    
    /**
     * 获取成功上报计数（用于监控）
     * 
     * @return 累计成功上报数量
     */
    long getReportedCount();
    
    /**
     * 优雅关闭（等待队列消费完毕）
     * 
     * <p>在指定超时时间内等待队列中的所有事件被消费。
     * 超时后强制关闭，未消费的事件将被丢弃。
     * 
     * @param timeoutMs 超时时间（毫秒），0 表示立即关闭
     * @return true=正常关闭, false=超时强制关闭
     */
    boolean shutdown(long timeoutMs);
    
    /**
     * 默认关闭实现（5秒超时）
     */
    @Override
    default void close() {
        shutdown(5000);
    }
    
    /**
     * 检查是否已关闭
     * 
     * @return true=已关闭, false=运行中
     */
    boolean isShutdown();
}
