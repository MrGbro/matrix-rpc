package io.homeey.matrix.rpc.registry.kubernetes;

import io.homeey.matrix.rpc.core.URL;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Kubernetes Registry 单元测试
 *
 * @author Matrix RPC Team
 */
class KubernetesRegistryTest {

    @Test
    void testBuildCacheKey() {
        // 测试缓存 Key 构建逻辑
        String key1 = buildCacheKey("EchoService", null, null);
        assertEquals("EchoService", key1);

        String key2 = buildCacheKey("EchoService", "default", null);
        assertEquals("EchoService:default", key2);

        String key3 = buildCacheKey("EchoService", "default", "1.0");
        assertEquals("EchoService:default:1.0", key3);
    }

    @Test
    void testURLConstruction() {
        // 测试 URL 构建
        Map<String, String> params = new HashMap<>();
        params.put("namespace", "default");
        params.put("interface", "EchoService");
        params.put("version", "1.0");

        URL url = new URL("kubernetes", "kubernetes.default.svc", 443, "EchoService", params);

        assertNotNull(url);
        assertEquals("kubernetes", url.getProtocol());
        assertEquals("default", url.getParameter("namespace"));
        assertEquals("EchoService", url.getParameter("interface"));
        assertEquals("1.0", url.getParameter("version"));
    }

    @Test
    void testGetCurrentPodName() {
        // 测试获取 Pod 名称（从环境变量）
        String podName = System.getenv("HOSTNAME");
        if (podName == null) {
            podName = System.getenv("POD_NAME");
        }

        // 非 K8s 环境下可能为 null
        System.out.println("Current Pod Name: " + podName);
    }

    /**
     * Helper method for testing cache key building
     */
    private String buildCacheKey(String serviceInterface, String group, String version) {
        StringBuilder sb = new StringBuilder(serviceInterface);
        if (group != null) {
            sb.append(":").append(group);
        }
        if (version != null) {
            sb.append(":").append(version);
        }
        return sb.toString();
    }
}
