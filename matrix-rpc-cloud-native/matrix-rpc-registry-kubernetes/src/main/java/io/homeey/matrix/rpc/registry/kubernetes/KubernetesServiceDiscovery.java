package io.homeey.matrix.rpc.registry.kubernetes;

import io.fabric8.kubernetes.api.model.Endpoints;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.informers.SharedIndexInformer;
import io.fabric8.kubernetes.client.informers.SharedInformerFactory;
import io.homeey.matrix.rpc.core.URL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Kubernetes 服务发现核心实现
 * <p>
 * 基于 Informer 机制 + 本地缓存实现高性能服务发现
 * 
 * <h3>🔄 Informer 工作原理</h3>
 * <pre>
 * 1. 全量同步：每30秒从 API Server 拉取一次全量 Endpoints
 *    - 触发条件：resyncPeriod 超时
 *    - 作用：防止 Watch 丢失事件，保证缓存一致性
 * 
 * 2. 增量 Watch：实时监听 Endpoints 的 Add/Update/Delete 事件
 *    - 通过 EndpointsWatcher 处理事件
 *    - 自动更新本地缓存
 * 
 * 3. 本地缓存：ConcurrentHashMap 存储服务列表
 *    - Key: serviceInterface:group:version
 *    - Value: List&lt;URL&gt;
 *    - 减少 API Server 访问压力
 * </pre>
 *
 * @author Matrix RPC Team
 */
public class KubernetesServiceDiscovery {

    private static final Logger logger = LoggerFactory.getLogger(KubernetesServiceDiscovery.class);
    private static final long RESYNC_PERIOD_MS = 30 * 1000L; // 30秒全量同步一次

    private final KubernetesClient kubeClient;
    private final SharedInformerFactory informerFactory;
    private final String namespace;
    
    // 本地缓存：serviceInterface:group:version -> List<URL>
    private final ConcurrentHashMap<String, List<URL>> serviceCache;
    
    // Endpoints Watcher
    private EndpointsWatcher endpointsWatcher;
    
    // Informer 实例
    private SharedIndexInformer<Endpoints> endpointsInformer;
    
    private volatile boolean started = false;

    public KubernetesServiceDiscovery(KubernetesClient kubeClient, String namespace) {
        this.kubeClient = kubeClient;
        this.namespace = namespace;
        this.informerFactory = kubeClient.informers();
        this.serviceCache = new ConcurrentHashMap<>();
        
        logger.info("KubernetesServiceDiscovery initialized: namespace={}", namespace);
    }

    /**
     * 启动 Informer 监听
     */
    public synchronized void start() {
        if (started) {
            logger.warn("KubernetesServiceDiscovery already started, skip");
            return;
        }

        // 创建 Endpoints Watcher
        this.endpointsWatcher = new EndpointsWatcher(namespace, serviceCache);

        // 创建 Endpoints Informer（监听当前 namespace 下的所有 Endpoints）
        this.endpointsInformer = informerFactory.sharedIndexInformerFor(
                Endpoints.class,
                RESYNC_PERIOD_MS  // 30秒全量同步一次（resyncPeriod）
        );

        // 注册事件处理器
        endpointsInformer.addEventHandler(endpointsWatcher);

        // 启动所有已注册的 Informer
        informerFactory.startAllRegisteredInformers();
        started = true;

        logger.info("✅ KubernetesServiceDiscovery Informer started successfully");
        logger.info("   - 监听范围: {}/endpoints/*", namespace);
        logger.info("   - 全量同步: 每{}ms一次", RESYNC_PERIOD_MS);
        logger.info("   - 增量更新: 实时 Watch");
    }

    /**
     * 停止 Informer 监听
     */
    public synchronized void stop() {
        if (!started) {
            return;
        }

        if (informerFactory != null) {
            informerFactory.stopAllRegisteredInformers();
        }
        
        started = false;
        logger.info("KubernetesServiceDiscovery stopped");
    }

    /**
     * 查询服务列表（从本地缓存读取）
     *
     * @param serviceInterface 服务接口
     * @param group 分组
     * @param version 版本
     * @return URL 列表
     */
    public List<URL> lookup(String serviceInterface, String group, String version) {
        // 确保 Informer 已启动
        if (!started) {
            start();
        }

        String cacheKey = buildCacheKey(serviceInterface, group, version);
        List<URL> urls = serviceCache.get(cacheKey);

        if (urls == null || urls.isEmpty()) {
            logger.info("🔍 [Lookup] Cache MISS: {} -> fallback to K8s API", cacheKey);
            // 缓存未命中，直接查询 K8s API（Informer 还未同步到该服务）
            urls = queryFromKubernetes(serviceInterface, group, version);
            if (!urls.isEmpty()) {
                serviceCache.put(cacheKey, urls);
            }
        } else {
            logger.debug("✅ [Lookup] Cache HIT: {} -> {} providers (from Informer cache)", cacheKey, urls.size());
        }

        return new ArrayList<>(urls);
    }

    /**
     * 获取本地缓存（供 EndpointsWatcher 使用）
     */
    ConcurrentHashMap<String, List<URL>> getServiceCache() {
        return serviceCache;
    }

    /**
     * 直接从 Kubernetes API 查询（用于缓存未命中时的降级查询）
     */
    private List<URL> queryFromKubernetes(String serviceInterface, String group, String version) {
        try {
            Endpoints endpoints = kubeClient.endpoints()
                    .inNamespace(namespace)
                    .withName(serviceInterface)
                    .get();

            if (endpoints == null) {
                return Collections.emptyList();
            }

            // 检查 group 和 version 是否匹配
            Map<String, String> labels = endpoints.getMetadata().getLabels();
            if (group != null && !group.equals(labels != null ? labels.get("group") : null)) {
                return Collections.emptyList();
            }
            if (version != null && !version.equals(labels != null ? labels.get("version") : null)) {
                return Collections.emptyList();
            }

            // 转换为 URL 列表
            return PodMetadataExtractor.extractURLs(endpoints);

        } catch (Exception e) {
            logger.error("Failed to query service from K8s: {}", serviceInterface, e);
            return Collections.emptyList();
        }
    }

    /**
     * 构建缓存 Key
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

    /**
     * 判断是否已启动
     */
    public boolean isStarted() {
        return started;
    }
}
