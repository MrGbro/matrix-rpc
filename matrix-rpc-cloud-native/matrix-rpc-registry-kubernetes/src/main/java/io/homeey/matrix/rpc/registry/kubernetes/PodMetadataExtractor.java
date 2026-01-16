package io.homeey.matrix.rpc.registry.kubernetes;

import io.fabric8.kubernetes.api.model.EndpointAddress;
import io.fabric8.kubernetes.api.model.EndpointPort;
import io.fabric8.kubernetes.api.model.EndpointSubset;
import io.fabric8.kubernetes.api.model.Endpoints;
import io.homeey.matrix.rpc.core.URL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Pod 元数据提取器
 * <p>
 * 负责从 Kubernetes Endpoints 中提取 Pod 元数据并转换为 Matrix RPC URL
 * 
 * <h3>📋 提取的元数据</h3>
 * <ul>
 *   <li><b>Labels</b>: version, group, 自定义标签</li>
 *   <li><b>Annotations</b>: 自定义注解</li>
 *   <li><b>Ready 状态</b>: 只使用 Ready 的 Pod（addresses 字段）</li>
 * </ul>
 *
 * @author Matrix RPC Team
 */
public class PodMetadataExtractor {

    private static final Logger logger = LoggerFactory.getLogger(PodMetadataExtractor.class);

    /**
     * 将 Endpoints 转换为 URL 列表
     * <p>
     * 工作流程：
     * 1. 遍历所有 EndpointSubset
     * 2. 只使用 Ready 的地址（addresses 字段）
     * 3. 提取 Labels 和 Annotations
     * 4. 构建 Matrix RPC URL
     *
     * @param endpoints Kubernetes Endpoints 对象
     * @return URL 列表
     */
    public static List<URL> extractURLs(Endpoints endpoints) {
        if (endpoints == null) {
            return Collections.emptyList();
        }

        List<URL> urls = new ArrayList<>();
        String serviceName = endpoints.getMetadata().getName();
        Map<String, String> labels = endpoints.getMetadata().getLabels();
        Map<String, String> annotations = endpoints.getMetadata().getAnnotations();

        // 遍历所有 Subset
        List<EndpointSubset> subsets = endpoints.getSubsets();
        if (subsets == null || subsets.isEmpty()) {
            return Collections.emptyList();
        }

        for (EndpointSubset subset : subsets) {
            // 只使用 Ready 的地址（addresses 字段）
            // notReadyAddresses 字段包含未 Ready 的 Pod，不应该被使用
            List<EndpointAddress> addresses = subset.getAddresses();
            if (addresses == null || addresses.isEmpty()) {
                continue;
            }

            List<EndpointPort> ports = subset.getPorts();
            if (ports == null || ports.isEmpty()) {
                continue;
            }

            // 为每个地址和端口组合创建 URL
            for (EndpointAddress address : addresses) {
                for (EndpointPort port : ports) {
                    URL url = buildURL(serviceName, address, port, labels, annotations);
                    urls.add(url);
                }
            }
        }

        logger.debug("Extracted {} URLs from Endpoints: {}", urls.size(), serviceName);
        return urls;
    }

    /**
     * 构建 Matrix RPC URL
     *
     * @param serviceName 服务名称
     * @param address Endpoint 地址
     * @param port Endpoint 端口
     * @param labels Labels（来自 Endpoints）
     * @param annotations Annotations（来自 Endpoints）
     * @return Matrix RPC URL
     */
    private static URL buildURL(String serviceName,
                                 EndpointAddress address,
                                 EndpointPort port,
                                 Map<String, String> labels,
                                 Map<String, String> annotations) {
        
        Map<String, String> params = new HashMap<>();
        params.put("interface", serviceName);

        // 添加 Labels 作为 URL 参数
        if (labels != null) {
            labels.forEach((k, v) -> {
                // 跳过 Kubernetes 系统标签
                if (!k.startsWith("kubernetes.io/") && !k.startsWith("k8s.io/")) {
                    params.put(k, v);
                }
            });
        }

        // 添加 Annotations 作为 URL 参数（可选）
        if (annotations != null) {
            annotations.forEach((k, v) -> {
                // 只添加 matrix-rpc 相关的注解
                if (k.startsWith("matrix-rpc.")) {
                    String paramKey = k.substring("matrix-rpc.".length());
                    params.put(paramKey, v);
                }
            });
        }

        // 从 TargetRef 提取 Pod 信息（可选）
        if (address.getTargetRef() != null) {
            String podName = address.getTargetRef().getName();
            if (podName != null) {
                params.put("pod", podName);
            }
        }

        return new URL(
                "matrix",                   // protocol
                address.getIp(),            // host
                port.getPort(),             // port
                serviceName,                // path
                params                      // parameters
        );
    }

    /**
     * 提取指定 Label 的值
     *
     * @param endpoints Endpoints 对象
     * @param labelKey Label 键
     * @return Label 值，不存在返回 null
     */
    public static String extractLabel(Endpoints endpoints, String labelKey) {
        if (endpoints == null || endpoints.getMetadata() == null) {
            return null;
        }
        
        Map<String, String> labels = endpoints.getMetadata().getLabels();
        return labels != null ? labels.get(labelKey) : null;
    }

    /**
     * 提取指定 Annotation 的值
     *
     * @param endpoints Endpoints 对象
     * @param annotationKey Annotation 键
     * @return Annotation 值，不存在返回 null
     */
    public static String extractAnnotation(Endpoints endpoints, String annotationKey) {
        if (endpoints == null || endpoints.getMetadata() == null) {
            return null;
        }
        
        Map<String, String> annotations = endpoints.getMetadata().getAnnotations();
        return annotations != null ? annotations.get(annotationKey) : null;
    }
}
