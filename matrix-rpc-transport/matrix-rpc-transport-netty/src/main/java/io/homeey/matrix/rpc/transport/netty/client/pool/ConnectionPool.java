package io.homeey.matrix.rpc.transport.netty.client.pool;

import io.netty.channel.Channel;

import java.io.Closeable;
import java.util.concurrent.TimeoutException;

/**
 * 连接池接口 - 管理多条连接的生命周期
 * 
 * <p>连接池负责：
 * <ul>
 *   <li>连接的获取与释放</li>
 *   <li>连接的健康检查</li>
 *   <li>连接池状态统计</li>
 * </ul>
 * 
 * @author Matrix RPC Team
 * @since 2026-01-14
 */
public interface ConnectionPool extends Closeable {
    
    /**
     * 从池中获取一个可用连接（阻塞，带超时）
     * 
     * @param timeout 超时时间（毫秒）
     * @return 可用的 Channel
     * @throws TimeoutException 获取超时
     * @throws InterruptedException 线程被中断
     */
    Channel acquire(long timeout) throws TimeoutException, InterruptedException;
    
    /**
     * 归还连接到池中
     * 
     * <p>如果连接不可用（已关闭），将被丢弃
     * 
     * @param channel 要归还的 Channel
     */
    void release(Channel channel);
    
    /**
     * 获取池状态统计信息
     * 
     * @return 池状态统计
     */
    PoolStats getStats();
    
    /**
     * 连接池状态统计信息
     */
    interface PoolStats {
        
        /**
         * 获取连接池中的总连接数
         * 
         * @return 总连接数
         */
        int getTotalConnections();
        
        /**
         * 获取活跃连接数（正在使用中）
         * 
         * @return 活跃连接数
         */
        int getActiveConnections();
        
        /**
         * 获取空闲连接数
         * 
         * @return 空闲连接数
         */
        int getIdleConnections();
    }
}
