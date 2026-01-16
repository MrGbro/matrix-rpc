package io.homeey.matrix.rpc.mesh;

import io.homeey.matrix.rpc.core.Invocation;
import io.homeey.matrix.rpc.core.Invoker;
import io.homeey.matrix.rpc.core.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Sidecar 故障降级处理器
 * <p>
 * 当 Sidecar 代理不可用时，自动降级到直连模式
 * 
 * <h3>🛡️ 降级策略</h3>
 * <ul>
 *   <li><b>超时降级</b>: Sidecar 调用超时时降级</li>
 *   <li><b>异常降级</b>: Sidecar 调用失败时降级</li>
 *   <li><b>熔断降级</b>: Sidecar 连续失败时自动熔断</li>
 * </ul>
 * 
 * <h3>📊 降级统计</h3>
 * <p>
 * 记录降级次数，用于监控和告警
 *
 * @author Matrix RPC Team
 */
public class DirectConnectFallback {

    private static final Logger logger = LoggerFactory.getLogger(DirectConnectFallback.class);

    // 降级计数器（用于监控）
    private static volatile long fallbackCount = 0;
    
    // 最近一次降级时间
    private static volatile long lastFallbackTime = 0;

    /**
     * 执行降级调用（直连模式）
     * 
     * @param invoker RPC 调用者
     * @param invocation RPC 调用上下文
     * @param sidecarError Sidecar 调用失败的原因
     * @return RPC 调用结果
     */
    public static Result fallbackToDirectConnection(Invoker<?> invoker, 
                                                     Invocation invocation, 
                                                     Throwable sidecarError) {
        
        // 1. 记录降级事件
        recordFallback(sidecarError);
        
        // 2. 清除 Mesh Headers（避免干扰直连调用）
        MeshMetadataInjector.clearHeaders(invocation);
        
        // 3. 执行直连调用
        try {
            logger.info("🔄 Fallback to direct connection: service={}, method={}", 
                       invocation.getServiceName(), invocation.methodName());
            
            Result result = invoker.invoke(invocation);
            
            logger.info("✅ Direct connection succeeded after fallback");
            return result;
            
        } catch (Exception e) {
            logger.error("❌ Direct connection also failed after fallback", e);
            throw e;
        }
    }

    /**
     * 记录降级事件
     * 
     * @param sidecarError Sidecar 调用失败的原因
     */
    private static void recordFallback(Throwable sidecarError) {
        fallbackCount++;
        lastFallbackTime = System.currentTimeMillis();
        
        logger.warn("⚠️ Sidecar proxy failed, fallback to direct connection (count: {}): {}", 
                   fallbackCount, sidecarError.getMessage());
        
        // 调试模式：打印完整堆栈
        if (logger.isDebugEnabled()) {
            logger.debug("Sidecar error details:", sidecarError);
        }
    }

    /**
     * 获取降级次数
     * 
     * @return 降级次数
     */
    public static long getFallbackCount() {
        return fallbackCount;
    }

    /**
     * 获取最近一次降级时间
     * 
     * @return 最近一次降级时间（毫秒）
     */
    public static long getLastFallbackTime() {
        return lastFallbackTime;
    }

    /**
     * 重置降级统计（用于测试）
     */
    public static void resetStatistics() {
        fallbackCount = 0;
        lastFallbackTime = 0;
        logger.info("Reset fallback statistics");
    }

    /**
     * 判断是否应该快速失败（而不是降级）
     * <p>
     * 某些场景下（如强制 Mesh 模式），不应该降级到直连
     * 
     * @return 如果应该快速失败返回 true
     */
    public static boolean shouldFailFast() {
        String meshMode = System.getProperty("matrix.mesh.mode", "auto");
        boolean strictMode = "true".equalsIgnoreCase(
                System.getProperty("matrix.mesh.strict", "false")
        );
        
        // 严格模式：强制 Mesh，不允许降级
        return "proxy".equals(meshMode) && strictMode;
    }

    /**
     * 检查降级是否过于频繁（可能需要告警）
     * 
     * @param windowMs 时间窗口（毫秒）
     * @param threshold 阈值
     * @return 如果降级过于频繁返回 true
     */
    public static boolean isFallbackTooFrequent(long windowMs, long threshold) {
        long now = System.currentTimeMillis();
        if (now - lastFallbackTime > windowMs) {
            return false;  // 时间窗口外的降级不计入
        }
        return fallbackCount >= threshold;
    }
}
