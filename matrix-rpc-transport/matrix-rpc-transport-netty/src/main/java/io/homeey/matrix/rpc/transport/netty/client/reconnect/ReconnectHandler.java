package io.homeey.matrix.rpc.transport.netty.client.reconnect;

import io.netty.channel.Channel;

/**
 * 重连处理器接口 - 负责在连接断开时进行重连
 * 
 * <p>实现类应当提供不同的重连策略，例如：
 * <ul>
 *   <li>指数退避重连（推荐）</li>
 *   <li>固定间隔重连</li>
 *   <li>立即重连</li>
 * </ul>
 * 
 * @author Matrix RPC Team
 * @since 2026-01-14
 */
public interface ReconnectHandler {
    
    /**
     * 尝试重连
     * 
     * <p>当连接断开时，由连接池调用此方法尝试重新建立连接
     * 
     * @return 重连后的 Channel，如果重连失败则返回 null
     * @throws InterruptedException 重连过程被中断
     */
    Channel reconnect() throws InterruptedException;
    
    /**
     * 重置重连状态
     * 
     * <p>在连接成功建立后调用，重置重连次数、延迟等状态
     */
    void reset();
    
    /**
     * 获取下一次重连延迟（毫秒）
     * 
     * @return 延迟时间（毫秒）
     */
    long getNextDelay();
    
    /**
     * 判断是否应该继续重连
     * 
     * @return true 表示应继续重连，false 表示停止重连
     */
    boolean shouldRetry();
}
