package io.homeey.matrix.rpc.mesh;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;

/**
 * Service Mesh 环境检测器
 * <p>
 * 自动检测当前应用是否运行在 Service Mesh 环境中（Istio/Linkerd）
 * <p>
 * 检测逻辑：
 * 1. 检查环境变量（ISTIO_META_MESH_ID / LINKERD_PROXY_API）
 * 2. 检查本地端口（15001/15006 for Envoy, 4140 for Linkerd）
 * 3. 检查文件系统（/etc/istio/proxy）
 *
 * @author Matrix RPC Team
 */
public class MeshDetector {

    private static final Logger logger = LoggerFactory.getLogger(MeshDetector.class);

    private static volatile Boolean inMesh = null;
    private static volatile MeshType meshType = null;

    /**
     * Mesh 类型枚举
     */
    public enum MeshType {
        ISTIO,
        LINKERD,
        UNKNOWN
    }

    /**
     * 检测是否运行在 Mesh 环境中
     */
    public static boolean isInMesh() {
        if (inMesh == null) {
            synchronized (MeshDetector.class) {
                if (inMesh == null) {
                    inMesh = detectMesh();
                }
            }
        }
        return inMesh;
    }

    /**
     * 获取 Mesh 类型
     */
    public static MeshType getMeshType() {
        if (!isInMesh()) {
            return null;
        }
        return meshType;
    }

    /**
     * 执行 Mesh 环境检测
     */
    private static boolean detectMesh() {
        // 1. 检测 Istio
        if (detectIstio()) {
            meshType = MeshType.ISTIO;
            logger.info("Detected Istio Service Mesh environment");
            return true;
        }

        // 2. 检测 Linkerd
        if (detectLinkerd()) {
            meshType = MeshType.LINKERD;
            logger.info("Detected Linkerd Service Mesh environment");
            return true;
        }

        logger.info("No Service Mesh detected, running in direct mode");
        return false;
    }

    /**
     * 检测 Istio 环境
     */
    private static boolean detectIstio() {
        // 1. 检查环境变量
        String meshId = System.getenv("ISTIO_META_MESH_ID");
        if (meshId != null && !meshId.isEmpty()) {
            logger.debug("Detected Istio env var: ISTIO_META_MESH_ID={}", meshId);
            return true;
        }

        String proxyVersion = System.getenv("ISTIO_META_ISTIO_VERSION");
        if (proxyVersion != null && !proxyVersion.isEmpty()) {
            logger.debug("Detected Istio env var: ISTIO_META_ISTIO_VERSION={}", proxyVersion);
            return true;
        }

        // 2. 检查 Envoy 代理端口
        // 15001: Envoy outbound
        // 15006: Envoy inbound
        // 15020: Envoy health check
        if (isPortListening(15001)) {
            logger.debug("Detected Envoy proxy on port 15001");
            return true;
        }

        if (isPortListening(15006)) {
            logger.debug("Detected Envoy proxy on port 15006");
            return true;
        }

        // 3. 检查文件系统
        if (fileExists("/etc/istio/proxy/envoy-rev.json")) {
            logger.debug("Detected Istio config file: /etc/istio/proxy/envoy-rev.json");
            return true;
        }

        return false;
    }

    /**
     * 检测 Linkerd 环境
     */
    private static boolean detectLinkerd() {
        // 1. 检查环境变量
        String proxyApi = System.getenv("LINKERD_PROXY_API");
        if (proxyApi != null && !proxyApi.isEmpty()) {
            logger.debug("Detected Linkerd env var: LINKERD_PROXY_API={}", proxyApi);
            return true;
        }

        String disabled = System.getenv("LINKERD_DISABLED");
        if ("true".equalsIgnoreCase(disabled)) {
            logger.debug("Linkerd is explicitly disabled");
            return false;
        }

        // 2. 检查 Linkerd 代理端口
        // 4140: Linkerd proxy inbound
        // 4191: Linkerd admin port
        if (isPortListening(4140)) {
            logger.debug("Detected Linkerd proxy on port 4140");
            return true;
        }

        if (isPortListening(4191)) {
            logger.debug("Detected Linkerd admin on port 4191");
            return true;
        }

        return false;
    }

    /**
     * 检查本地端口是否在监听
     *
     * @param port 端口号
     * @return true 如果端口正在监听
     */
    private static boolean isPortListening(int port) {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress("127.0.0.1", port), 100);
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    /**
     * 检查文件是否存在
     *
     * @param path 文件路径
     * @return true 如果文件存在
     */
    private static boolean fileExists(String path) {
        try {
            return new java.io.File(path).exists();
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 获取 Sidecar 代理端口
     * <p>
     * 根据不同的 Mesh 类型返回对应的代理端口
     *
     * @return Sidecar 代理端口
     */
    public static int getSidecarPort() {
        if (!isInMesh()) {
            throw new IllegalStateException("Not running in Service Mesh environment");
        }

        switch (meshType) {
            case ISTIO:
                // Envoy outbound port
                return 15001;
            case LINKERD:
                // Linkerd proxy port
                return 4140;
            default:
                throw new IllegalStateException("Unknown mesh type: " + meshType);
        }
    }

    /**
     * 强制重新检测（用于测试）
     */
    public static synchronized void reset() {
        inMesh = null;
        meshType = null;
    }

    /**
     * 手动设置 Mesh 状态（用于测试）
     */
    public static synchronized void setInMesh(boolean enabled, MeshType type) {
        inMesh = enabled;
        meshType = type;
    }
}
