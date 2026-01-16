package io.homeey.matrix.rpc.registry.kubernetes;

import io.fabric8.kubernetes.api.model.*;
import io.fabric8.kubernetes.client.*;
import io.homeey.matrix.rpc.core.URL;
import io.homeey.matrix.rpc.registry.api.NotifyListener;
import io.homeey.matrix.rpc.registry.api.Registry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Kubernetes 服务注册与发现实现
 * <p>
 * 基于 Kubernetes Endpoints 实现 Matrix RPC Registry 接口
 * 
 * <h3>✨ 核心特性</h3>
 * <ul>
 *   <li><b>Informer 机制</b>：委托 KubernetesServiceDiscovery 实现</li>
 *   <li><b>Ready 状态感知</b>：自动过滤未 Ready 的 Pod</li>
 *   <li><b>Labels 支持</b>：委托 PodMetadataExtractor 提取元数据</li>
 *   <li><b>高可用</b>：缓存未命中时降级到 K8s API 直接查询</li>
 * </ul>
 * 
 * <h3>📊 架构设计</h3>
 * <pre>
 * KubernetesRegistry (入口)
 *   └─ KubernetesServiceDiscovery (Informer 机制 + 缓存管理)
 *       ├─ EndpointsWatcher (事件处理)
 *       └─ PodMetadataExtractor (元数据提取)
 * </pre>
 *
 * @author Matrix RPC Team
 * @see KubernetesServiceDiscovery
 * @see EndpointsWatcher
 * @see PodMetadataExtractor
 */
public class KubernetesRegistry implements Registry {

    private static final Logger logger = LoggerFactory.getLogger(KubernetesRegistry.class);

    private final KubernetesClient kubeClient;
    private final KubernetesServiceDiscovery serviceDiscovery;
    private final Map<String, List<NotifyListener>> listeners;
    private final String namespace;

    public KubernetesRegistry(URL url) {
        this.namespace = url.getParameter("namespace", "default");
        this.listeners = new HashMap<>();

        // 初始化 Kubernetes Client
        io.fabric8.kubernetes.client.Config config = new io.fabric8.kubernetes.client.ConfigBuilder()
                .withMasterUrl(url.getParameter("master", "https://kubernetes.default.svc"))
                .withNamespace(namespace)
                .build();

        this.kubeClient = new KubernetesClientBuilder().withConfig(config).build();
        
        // 初始化 Service Discovery（委托 Informer 机制）
        this.serviceDiscovery = new KubernetesServiceDiscovery(kubeClient, namespace);

        logger.info("KubernetesRegistry initialized for namespace: {}", namespace);
    }

    @Override
    public void register(URL url) {
        try {
            String serviceName = url.getPath();
            if (serviceName == null || serviceName.isEmpty()) {
                serviceName = url.getParameter("interface");
            }

            // 获取当前 Pod 信息
            String podName = getCurrentPodName();
            String podIp = getCurrentPodIp();

            // 构建 Endpoints
            EndpointsBuilder builder = new EndpointsBuilder()
                    .withNewMetadata()
                    .withNamespace(namespace)
                    .withName(serviceName)
                    .addToLabels("app", serviceName)
                    .addToLabels("matrix-rpc", "provider")
                    .endMetadata()
                    .addNewSubset()
                    .addNewAddress()
                    .withIp(podIp != null ? podIp : url.getHost())
                    .withNewTargetRef()
                    .withKind("Pod")
                    .withName(podName != null ? podName : "matrix-rpc-provider")
                    .withNamespace(namespace)
                    .endTargetRef()
                    .endAddress()
                    .addNewPort()
                    .withPort(url.getPort())
                    .withProtocol("TCP")
                    .withName("matrix-rpc")
                    .endPort()
                    .endSubset();

            // 添加版本和分组标签
            String version = url.getParameter("version");
            String group = url.getParameter("group");
            if (version != null) {
                builder.editMetadata().addToLabels("version", version).endMetadata();
            }
            if (group != null) {
                builder.editMetadata().addToLabels("group", group).endMetadata();
            }

            Endpoints endpoints = builder.build();

            // 创建或更新 Endpoints
            kubeClient.endpoints()
                    .inNamespace(namespace)
                    .resource(endpoints)
                    .createOrReplace();

            logger.info("Service registered to K8s: {}/{}", namespace, serviceName);

        } catch (Exception e) {
            logger.error("Failed to register service to K8s", e);
            throw new RuntimeException("K8s service registration failed", e);
        }
    }

    @Override
    public void unregister(URL url) {
        try {
            String serviceName = url.getPath();
            if (serviceName == null || serviceName.isEmpty()) {
                serviceName = url.getParameter("interface");
            }

            // 删除 Endpoints
            kubeClient.endpoints()
                    .inNamespace(namespace)
                    .withName(serviceName)
                    .delete();

            logger.info("Service unregistered from K8s: {}/{}", namespace, serviceName);

        } catch (Exception e) {
            logger.error("Failed to unregister service from K8s", e);
        }
    }

    @Override
    public List<URL> lookup(String serviceInterface, String group, String version) {
        // 委托给 KubernetesServiceDiscovery（会自动启动 Informer）
        return serviceDiscovery.lookup(serviceInterface, group, version);
    }

    @Override
    public void subscribe(String serviceInterface, NotifyListener listener) {
        // 确保 Service Discovery 已启动
        if (!serviceDiscovery.isStarted()) {
            serviceDiscovery.start();
        }

        String key = serviceInterface;
        listeners.computeIfAbsent(key, k -> new CopyOnWriteArrayList<>()).add(listener);

        // 立即通知当前缓存的服务列表
        List<URL> urls = serviceDiscovery.lookup(serviceInterface, null, null);
        if (!urls.isEmpty()) {
            listener.notify(urls);
        }

        logger.info("Subscribed to service: {}", serviceInterface);
    }

    /**
     * 获取当前 Pod 名称
     */
    private String getCurrentPodName() {
        // 从环境变量读取（Kubernetes 会自动注入）
        String podName = System.getenv("HOSTNAME");
        if (podName == null) {
            podName = System.getenv("POD_NAME");
        }
        return podName;
    }

    /**
     * 获取当前 Pod IP
     */
    private String getCurrentPodIp() {
        String podIp = System.getenv("POD_IP");
        if (podIp == null) {
            try {
                podIp = InetAddress.getLocalHost().getHostAddress();
            } catch (Exception e) {
                logger.warn("Failed to get local IP", e);
            }
        }
        return podIp;
    }

    /**
     * 关闭资源
     */
    public void close() {
        if (serviceDiscovery != null) {
            serviceDiscovery.stop();
        }
        if (kubeClient != null) {
            kubeClient.close();
        }
        logger.info("KubernetesRegistry closed");
    }
}
