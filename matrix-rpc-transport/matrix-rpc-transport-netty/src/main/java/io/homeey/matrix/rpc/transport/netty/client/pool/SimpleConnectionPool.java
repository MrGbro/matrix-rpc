package io.homeey.matrix.rpc.transport.netty.client.pool;

import io.homeey.matrix.rpc.core.URL;
import io.homeey.matrix.rpc.transport.netty.client.reconnect.ExponentialBackoffReconnect;
import io.homeey.matrix.rpc.transport.netty.client.reconnect.ReconnectHandler;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.LockSupport;

/**
 * 简单连接池实现
 * 
 * <p>特性：
 * <ul>
 *   <li>基于 ConcurrentLinkedQueue 实现轻量级连接池</li>
 *   <li>支持动态创建连接（最多到 maxConnections）</li>
 *   <li>支持连接复用和健康检查</li>
 *   <li>连接断开时自动清理</li>
 *   <li>支持断线重连（指数退避策略）</li>
 * </ul>
 * 
 * @author Matrix RPC Team
 * @since 2026-01-14
 */
public class SimpleConnectionPool implements ConnectionPool {
    
    private static final Logger logger = LoggerFactory.getLogger(SimpleConnectionPool.class);
    
    private final URL url;
    private final Bootstrap bootstrap;
    private final int maxConnections;
    private final int minConnections;
    
    /** 空闲连接队列 */
    private final ConcurrentLinkedQueue<Channel> idleChannels = new ConcurrentLinkedQueue<>();
    
    /** 活跃连接集合 */
    private final Set<Channel> activeChannels = ConcurrentHashMap.newKeySet();
    
    /** 总连接数计数器 */
    private final AtomicInteger totalConnections = new AtomicInteger(0);
    
    /** 连接池是否已关闭 */
    private volatile boolean closed = false;
    
    /** 重连处理器 */
    private final ReconnectHandler reconnectHandler;
    
    /** 是否启用自动重连（默认启用） */
    private final boolean enableReconnect;
    
    /**
     * 创建连接池
     * 
     * @param bootstrap Netty Bootstrap
     * @param url 目标服务地址
     * @param maxConnections 最大连接数
     */
    public SimpleConnectionPool(Bootstrap bootstrap, URL url, int maxConnections) {
        this.url = url;
        this.bootstrap = bootstrap;
        this.maxConnections = maxConnections;
        this.minConnections = Math.max(1, maxConnections / 2);
        
        // 创建重连处理器
        this.enableReconnect = "true".equals(url.getParameter("enableReconnect", "true"));
        this.reconnectHandler = new ExponentialBackoffReconnect(bootstrap, url);
        
        // 预创建最小连接数
        logger.info("Initialize connection pool for {} with min={}, max={}, reconnect={}", 
            url.getAddress(), minConnections, maxConnections, enableReconnect);
        
        for (int i = 0; i < minConnections; i++) {
            try {
                Channel channel = createConnection();
                if (channel != null) {
                    idleChannels.offer(channel);
                }
            } catch (Exception e) {
                logger.warn("Failed to create initial connection {}/{}", i + 1, minConnections, e);
            }
        }
        
        logger.info("Connection pool initialized with {} connections", idleChannels.size());
    }
    
    @Override
    public Channel acquire(long timeout) throws TimeoutException, InterruptedException {
        if (closed) {
            throw new IllegalStateException("Connection pool is closed");
        }
        
        long deadline = System.currentTimeMillis() + timeout;
        
        while (true) {
            // 1. 尝试从空闲队列获取
            Channel channel = idleChannels.poll();
            if (channel != null) {
                if (channel.isActive()) {
                    activeChannels.add(channel);
                    logger.debug("Acquired channel from idle queue: {}", channel);
                    return channel;
                } else {
                    // 连接已失效，清理并继续尝试
                    logger.debug("Channel is inactive, discarding: {}", channel);
                    totalConnections.decrementAndGet();
                }
            }
            
            // 2. 尝试创建新连接（未达上限）
            if (totalConnections.get() < maxConnections) {
                try {
                    channel = createConnection();
                    if (channel != null) {
                        activeChannels.add(channel);
                        logger.debug("Created new channel: {}", channel);
                        return channel;
                    }
                } catch (Exception e) {
                    logger.warn("Failed to create connection", e);
                }
            }
            
            // 3. 已达上限，等待连接释放
            if (System.currentTimeMillis() > deadline) {
                throw new TimeoutException("Acquire connection timeout after " + timeout + "ms, " +
                    "active=" + activeChannels.size() + ", total=" + totalConnections.get());
            }
            
            // 短暂休眠后重试
            LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(10));
        }
    }
    
    @Override
    public void release(Channel channel) {
        if (channel == null) {
            return;
        }
        
        activeChannels.remove(channel);
        
        if (!channel.isActive() || closed) {
            // 连接已失效或池已关闭，丢弃连接
            logger.debug("Channel is inactive or pool closed, discarding: {}", channel);
            totalConnections.decrementAndGet();
            if (channel.isOpen()) {
                channel.close();
            }
            return;
        }
        
        // 归还到空闲队列
        idleChannels.offer(channel);
        logger.debug("Released channel to idle queue: {}", channel);
    }
    
    @Override
    public PoolStats getStats() {
        return new PoolStats() {
            @Override
            public int getTotalConnections() {
                return totalConnections.get();
            }
            
            @Override
            public int getActiveConnections() {
                return activeChannels.size();
            }
            
            @Override
            public int getIdleConnections() {
                return idleChannels.size();
            }
        };
    }
    
    @Override
    public void close() {
        if (closed) {
            return;
        }
        
        closed = true;
        logger.info("Closing connection pool for {}", url.getAddress());
        
        // 关闭所有空闲连接
        Channel channel;
        while ((channel = idleChannels.poll()) != null) {
            if (channel.isOpen()) {
                channel.close();
            }
        }
        
        // 关闭所有活跃连接
        for (Channel activeChannel : activeChannels) {
            if (activeChannel.isOpen()) {
                activeChannel.close();
            }
        }
        
        activeChannels.clear();
        totalConnections.set(0);
        
        logger.info("Connection pool closed");
    }
    
    /**
     * 创建一个新连接
     * 
     * @return 创建的 Channel，失败返回 null
     */
    private Channel createConnection() throws InterruptedException {
        try {
            ChannelFuture future = bootstrap.connect(url.getHost(), url.getPort()).sync();
            
            if (future.isSuccess()) {
                Channel channel = future.channel();
                totalConnections.incrementAndGet();
                
                // 监听连接关闭事件
                channel.closeFuture().addListener(closeFuture -> {
                    handleChannelClosed(channel);
                });
                
                logger.debug("Created connection to {}: {}", url.getAddress(), channel);
                return channel;
            } else {
                logger.warn("Failed to connect to {}: {}", url.getAddress(), future.cause().getMessage());
                return null;
            }
        } catch (Exception e) {
            logger.error("Failed to create connection to {}", url.getAddress(), e);
            throw e;
        }
    }
    
    /**
     * 处理连接关闭事件
     * 
     * @param channel 关闭的 Channel
     */
    private void handleChannelClosed(Channel channel) {
        logger.warn("Channel closed: {}", channel);
        
        // 从活跃和空闲集合中移除
        activeChannels.remove(channel);
        idleChannels.remove(channel);
        totalConnections.decrementAndGet();
        
        logger.debug("Pool stats after channel closed: active={}, idle={}, total={}", 
            activeChannels.size(), idleChannels.size(), totalConnections.get());
        
        // 如果启用重连且连接池未关闭，尝试重连
        if (enableReconnect && !closed && totalConnections.get() < minConnections) {
            logger.info("Connection count below minimum, attempting to reconnect...");
            tryReconnect();
        }
    }
    
    /**
     * 尝试重连
     */
    private void tryReconnect() {
        // 在单独的线程中执行重连，避免阻塞主线程
        Thread reconnectThread = new Thread(() -> {
            try {
                Channel newChannel = reconnectHandler.reconnect();
                if (newChannel != null) {
                    // 重连成功，将新连接添加到空闲队列
                    totalConnections.incrementAndGet();
                    
                    // 监听新连接的关闭事件
                    newChannel.closeFuture().addListener(closeFuture -> {
                        handleChannelClosed(newChannel);
                    });
                    
                    idleChannels.offer(newChannel);
                    logger.info("Reconnect successful, added new channel to pool: {}", newChannel);
                } else {
                    logger.warn("Reconnect failed, will retry on next opportunity");
                }
            } catch (Exception e) {
                logger.error("Exception during reconnect", e);
            }
        }, "reconnect-thread-" + url.getAddress());
        
        reconnectThread.setDaemon(true);
        reconnectThread.start();
    }
}
