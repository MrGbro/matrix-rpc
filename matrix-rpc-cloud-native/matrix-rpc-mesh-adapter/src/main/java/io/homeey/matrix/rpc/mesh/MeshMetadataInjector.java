package io.homeey.matrix.rpc.mesh;

import io.homeey.matrix.rpc.core.Invocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * Mesh 元数据注入器
 * <p>
 * 负责向 RPC 调用中注入 Mesh 所需的元数据 Headers
 * 
 * <h3>📋 注入的元数据</h3>
 * <ul>
 *   <li><b>x-mesh-method</b>: 方法名</li>
 *   <li><b>x-mesh-type</b>: Mesh 类型（istio/linkerd）</li>
 *   <li><b>x-mesh-service</b>: 服务名</li>
 *   <li><b>x-request-id</b>: 请求 ID（用于链路追踪）</li>
 * </ul>
 *
 * @author Matrix RPC Team
 */
public class MeshMetadataInjector {

    private static final Logger logger = LoggerFactory.getLogger(MeshMetadataInjector.class);

    /**
     * 注入 Mesh 所需的 Headers
     * 
     * @param invocation RPC 调用上下文
     */
    public static void injectHeaders(Invocation invocation) {
        Map<String, String> attachments = invocation.getAttachments();

        // 1. 注入方法名
        String methodName = invocation.methodName();
        if (methodName != null) {
            attachments.put("x-mesh-method", methodName);
            logger.debug("Injected x-mesh-method: {}", methodName);
        }

        // 2. 注入 Mesh 类型标识
        MeshDetector.MeshType meshType = MeshDetector.getMeshType();
        if (meshType != null && meshType != MeshDetector.MeshType.UNKNOWN) {
            attachments.put("x-mesh-type", meshType.name().toLowerCase());
            logger.debug("Injected x-mesh-type: {}", meshType.name().toLowerCase());
        }

        // 3. 注入服务名（从 Invocation 中提取）
        String serviceName = invocation.getServiceName();
        if (serviceName != null) {
            attachments.put("x-mesh-service", serviceName);
            logger.debug("Injected x-mesh-service: {}", serviceName);
        }

        // 4. 注入请求 ID（如果尚未设置）
        if (!attachments.containsKey("x-request-id")) {
            String requestId = generateRequestId();
            attachments.put("x-request-id", requestId);
            logger.debug("Generated x-request-id: {}", requestId);
        }

        // 5. 注入 Sidecar 端口信息（用于调试）
        int sidecarPort = MeshDetector.getSidecarPort();
        if (sidecarPort > 0) {
            attachments.put("x-mesh-sidecar-port", String.valueOf(sidecarPort));
        }
    }

    /**
     * 生成请求 ID
     * <p>
     * 格式: timestamp-randomValue
     * 
     * @return 请求 ID
     */
    private static String generateRequestId() {
        long timestamp = System.currentTimeMillis();
        int random = (int) (Math.random() * 10000);
        return String.format("%d-%04d", timestamp, random);
    }

    /**
     * 检查是否已注入 Mesh Headers
     * 
     * @param invocation RPC 调用上下文
     * @return 如果已注入返回 true
     */
    public static boolean hasInjectedHeaders(Invocation invocation) {
        Map<String, String> attachments = invocation.getAttachments();
        return attachments.containsKey("x-mesh-method") || 
               attachments.containsKey("x-mesh-type");
    }

    /**
     * 清除 Mesh Headers（用于测试）
     * 
     * @param invocation RPC 调用上下文
     */
    public static void clearHeaders(Invocation invocation) {
        Map<String, String> attachments = invocation.getAttachments();
        attachments.remove("x-mesh-method");
        attachments.remove("x-mesh-type");
        attachments.remove("x-mesh-service");
        attachments.remove("x-request-id");
        attachments.remove("x-mesh-sidecar-port");
        logger.debug("Cleared Mesh headers");
    }
}
