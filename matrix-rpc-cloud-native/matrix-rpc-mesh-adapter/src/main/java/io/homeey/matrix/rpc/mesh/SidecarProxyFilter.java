package io.homeey.matrix.rpc.mesh;

import io.homeey.matrix.rpc.core.Invocation;
import io.homeey.matrix.rpc.core.Invoker;
import io.homeey.matrix.rpc.core.Result;
import io.homeey.matrix.rpc.core.URL;
import io.homeey.matrix.rpc.filter.Filter;
import io.homeey.matrix.rpc.spi.Activate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * Sidecar 代理 Filter
 * <p>
 * 根据配置和环境自动决定流量是走 Sidecar 代理还是直连
 * <p>
 * <h3>🔄 职责</h3>
 * <ul>
 *   <li>判断是否使用 Sidecar 代理（委托给 MeshDetector）</li>
 *   <li>注入 Mesh 元数据（委托给 MeshMetadataInjector）</li>
 *   <li>故障降级处理（委托给 DirectConnectFallback）</li>
 * </ul>
 * 
 * <h3>⚙️ 模式</h3>
 * <ul>
 *   <li><b>auto</b>: 自动检测 Mesh 环境，有则走代理，无则直连（默认）</li>
 *   <li><b>proxy</b>: 强制走 Sidecar 代理</li>
 *   <li><b>direct</b>: 强制直连，跳过 Sidecar</li>
 * </ul>
 * 
 * <h3>🎛️ 系统属性</h3>
 * <ul>
 *   <li><b>matrix.mesh.mode</b>: 设置模式（auto/proxy/direct）</li>
 *   <li><b>matrix.mesh.enabled</b>: 是否启用 Mesh 支持（默认 true）</li>
 *   <li><b>matrix.mesh.strict</b>: 严格模式，禁止降级（默认 false）</li>
 * </ul>
 *
 * @author Matrix RPC Team
 */
@Activate(order = 50, group = {"consumer"})
public class SidecarProxyFilter implements Filter {

    private static final Logger logger = LoggerFactory.getLogger(SidecarProxyFilter.class);

    /**
     * Mesh 模式配置
     * auto: 自动检测（默认）
     * proxy: 强制代理
     * direct: 强制直连
     */
    private static final String MESH_MODE = System.getProperty("matrix.mesh.mode", "auto");

    /**
     * 是否启用 Mesh 支持
     */
    private static volatile boolean enabled = "true".equalsIgnoreCase(
            System.getProperty("matrix.mesh.enabled", "true")
    );

    /**
     * 设置是否启用 Mesh 支持
     */
    public static void setEnabled(boolean enabled) {
        SidecarProxyFilter.enabled = enabled;
        logger.info("SidecarProxyFilter enabled: {}", enabled);
    }

    @Override
    public Result invoke(Invoker<?> invoker, Invocation invocation) {
        // 快速路径：如果未启用，直接跳过
        if (!enabled) {
            return invoker.invoke(invocation);
        }

        // 判断是否需要走 Sidecar
        boolean useSidecar = shouldUseSidecar(invocation);

        if (useSidecar) {
            try {
                return invokeThroughSidecar(invoker, invocation);
            } catch (Exception e) {
                // 检查是否应该快速失败（严格模式）
                if (DirectConnectFallback.shouldFailFast()) {
                    logger.error("❌ Sidecar proxy failed in strict mode, fail fast", e);
                    throw new RuntimeException("Sidecar proxy unavailable in strict mode", e);
                }
                
                // Sidecar 故障，降级到直连
                return DirectConnectFallback.fallbackToDirectConnection(invoker, invocation, e);
            }
        }

        // 直连模式：保持原有行为
        return invoker.invoke(invocation);
    }

    /**
     * 判断是否应该使用 Sidecar 代理
     * 
     * <h3>🔍 决策优先级</h3>
     * <ol>
     *   <li>Attachment 参数（优先级最高）</li>
     *   <li>系统属性配置</li>
     *   <li>自动检测 Mesh 环境</li>
     * </ol>
     * 
     * @param invocation RPC 调用上下文
     * @return 如果应该使用 Sidecar 返回 true
     */
    private boolean shouldUseSidecar(Invocation invocation) {
        // 1. 检查 Attachment 参数（优先级最高）
        String attachmentMode = invocation.getAttachments().get("mesh.mode");
        if ("proxy".equals(attachmentMode)) {
            logger.debug("Attachment mesh.mode=proxy, using sidecar");
            return true;
        }
        if ("direct".equals(attachmentMode)) {
            logger.debug("Attachment mesh.mode=direct, using direct connection");
            return false;
        }

        // 2. 检查系统属性
        if ("proxy".equals(MESH_MODE)) {
            logger.debug("System property mesh.mode=proxy, using sidecar");
            return true;
        }
        if ("direct".equals(MESH_MODE)) {
            logger.debug("System property mesh.mode=direct, using direct connection");
            return false;
        }

        // 3. auto 模式：委托给 MeshDetector 检测 Mesh 环境
        boolean inMesh = MeshDetector.isInMesh();
        if (inMesh) {
            logger.debug("✅ Detected Mesh environment ({}), using sidecar proxy", 
                        MeshDetector.getMeshType());
        } else {
            logger.debug("❌ No Mesh detected, using direct connection");
        }
        return inMesh;
    }

    /**
     * 通过 Sidecar 代理调用
     * 
     * @param invoker RPC 调用者
     * @param invocation RPC 调用上下文
     * @return RPC 调用结果
     */
    private Result invokeThroughSidecar(Invoker<?> invoker, Invocation invocation) {
        // 1. 获取 Sidecar 端口（用于日志）
        int sidecarPort = MeshDetector.getSidecarPort();

        // 2. 委托给 MeshMetadataInjector 注入 Mesh 所需的 Headers
        MeshMetadataInjector.injectHeaders(invocation);

        // 3. 记录日志
        logger.debug("🌐 Invoking through sidecar: type={}, port={}", 
                    MeshDetector.getMeshType(), sidecarPort);

        // 4. 通过原始 Invoker 调用（流量会被 Sidecar 拦截）
        return invoker.invoke(invocation);
    }

}
