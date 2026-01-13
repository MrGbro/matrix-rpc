package io.homeey.matrix.rpc.transport.netty.client.reconnect;

import io.homeey.matrix.rpc.core.URL;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

/**
 * 指数退避重连处理器 - 使用指数退避算法进行重连
 * 
 * <p>重连延迟序列：1s → 2s → 4s → 8s → 16s → 30s（最大）
 * 
 * <p>特性：
 * <ul>
 *   <li>避免连接风暴（指数增长延迟）</li>
 *   <li>最大延迟保护（避免无限增长）</li>
 *   <li>重连次数限制（默认无限重试，可配置）</li>
 *   <li>连接成功后自动重置状态</li>
 * </ul>
 * 
 * @author Matrix RPC Team
 * @since 2026-01-14
 */
public class ExponentialBackoffReconnect implements ReconnectHandler {
    
    private static final Logger logger = LoggerFactory.getLogger(ExponentialBackoffReconnect.class);
    
    /**
     * 初始延迟（毫秒）
     */
    private static final long INITIAL_DELAY_MS = 1000;
    
    /**
     * 最大延迟（毫秒）
     */
    private static final long MAX_DELAY_MS = 30000;
    
    /**
     * 最大重连次数（-1 表示无限重试）
     */
    private static final int MAX_RETRY_COUNT = -1;
    
    /**
     * Netty Bootstrap
     */
    private final Bootstrap bootstrap;
    
    /**
     * 目标服务地址
     */
    private final URL url;
    
    /**
     * 当前重连次数
     */
    private int retryCount = 0;
    
    /**
     * 当前延迟（毫秒）
     */
    private long currentDelay = INITIAL_DELAY_MS;
    
    /**
     * 创建指数退避重连处理器
     * 
     * @param bootstrap Netty Bootstrap
     * @param url 目标服务地址
     */
    public ExponentialBackoffReconnect(Bootstrap bootstrap, URL url) {
        this.bootstrap = bootstrap;
        this.url = url;
    }
    
    @Override
    public Channel reconnect() throws InterruptedException {
        if (!shouldRetry()) {
            logger.warn("Reached max retry count, stop reconnecting to {}", url.getAddress());
            return null;
        }
        
        // 等待延迟
        long delay = getNextDelay();
        logger.info("Reconnecting to {} in {}ms (attempt {})", 
                url.getAddress(), delay, retryCount + 1);
        TimeUnit.MILLISECONDS.sleep(delay);
        
        try {
            // 尝试重连
            ChannelFuture future = bootstrap.connect(url.getHost(), url.getPort()).sync();
            
            if (future.isSuccess()) {
                Channel channel = future.channel();
                logger.info("Successfully reconnected to {} after {} attempts", 
                        url.getAddress(), retryCount + 1);
                
                // 重连成功，重置状态
                reset();
                return channel;
            } else {
                // 重连失败，更新状态
                retryCount++;
                currentDelay = Math.min(currentDelay * 2, MAX_DELAY_MS);
                
                logger.warn("Failed to reconnect to {}: {}", 
                        url.getAddress(), future.cause().getMessage());
                return null;
            }
        } catch (Exception e) {
            // 重连失败，更新状态
            retryCount++;
            currentDelay = Math.min(currentDelay * 2, MAX_DELAY_MS);
            
            logger.error("Exception during reconnect to {}", url.getAddress(), e);
            throw e;
        }
    }
    
    @Override
    public void reset() {
        retryCount = 0;
        currentDelay = INITIAL_DELAY_MS;
        logger.debug("Reconnect state reset for {}", url.getAddress());
    }
    
    @Override
    public long getNextDelay() {
        return currentDelay;
    }
    
    @Override
    public boolean shouldRetry() {
        if (MAX_RETRY_COUNT < 0) {
            // 无限重试
            return true;
        }
        return retryCount < MAX_RETRY_COUNT;
    }
    
    /**
     * 获取当前重连次数
     * 
     * @return 重连次数
     */
    public int getRetryCount() {
        return retryCount;
    }
}
