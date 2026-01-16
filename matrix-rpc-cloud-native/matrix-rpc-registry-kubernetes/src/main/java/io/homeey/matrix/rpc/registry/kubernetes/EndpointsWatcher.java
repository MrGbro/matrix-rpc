package io.homeey.matrix.rpc.registry.kubernetes;

import io.fabric8.kubernetes.api.model.Endpoints;
import io.fabric8.kubernetes.client.informers.ResourceEventHandler;
import io.homeey.matrix.rpc.core.URL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Endpoints 变化监听器
 * <p>
 * 实现 ResourceEventHandler 接口，处理 Informer 的三种事件：
 * - onAdd: 新增 Endpoints 时触发
 * - onUpdate: Endpoints 内容变更时触发
 * - onDelete: Endpoints 被删除时触发
 *
 * @author Matrix RPC Team
 */
public class EndpointsWatcher implements ResourceEventHandler<Endpoints> {

    private static final Logger logger = LoggerFactory.getLogger(EndpointsWatcher.class);

    private final String namespace;
    private final ConcurrentHashMap<String, List<URL>> serviceCache;

    public EndpointsWatcher(String namespace, ConcurrentHashMap<String, List<URL>> serviceCache) {
        this.namespace = namespace;
        this.serviceCache = serviceCache;
    }

    @Override
    public void onAdd(Endpoints endpoints) {
        String serviceName = endpoints.getMetadata().getName();
        logger.info("[Informer] Endpoints ADDED: namespace={}, service={}", namespace, serviceName);

        // 更新本地缓存并通知监听器
        updateCache(endpoints);
    }

    @Override
    public void onUpdate(Endpoints oldObj, Endpoints newObj) {
        String serviceName = newObj.getMetadata().getName();
        logger.info("[Informer] Endpoints UPDATED: namespace={}, service={}", namespace, serviceName);

        // 比较变更内容（用于调试）
        if (logger.isDebugEnabled()) {
            int oldAddresses = oldObj.getSubsets().stream()
                    .mapToInt(s -> s.getAddresses() != null ? s.getAddresses().size() : 0).sum();
            int newAddresses = newObj.getSubsets().stream()
                    .mapToInt(s -> s.getAddresses() != null ? s.getAddresses().size() : 0).sum();
            logger.debug("  Address count: {} -> {}", oldAddresses, newAddresses);
        }

        // 更新本地缓存并通知监听器
        updateCache(newObj);
    }

    @Override
    public void onDelete(Endpoints endpoints, boolean deletedFinalStateUnknown) {
        String serviceName = endpoints.getMetadata().getName();
        logger.info("[Informer] Endpoints DELETED: namespace={}, service={}, deletedFinalStateUnknown={}",
                namespace, serviceName, deletedFinalStateUnknown);

        // 从本地缓存移除并通知监听器
        removeFromCache(endpoints);
    }

    /**
     * 更新本地缓存（由 Informer 事件触发）
     * <p>
     * 工作流程：
     * 1. 从 Endpoints 提取 Labels（version/group）
     * 2. 将 Endpoints 转换为 URL 列表
     * 3. 更新 ConcurrentHashMap 缓存
     */
    private void updateCache(Endpoints endpoints) {
        String serviceName = endpoints.getMetadata().getName();
        Map<String, String> labels = endpoints.getMetadata().getLabels();

        String version = labels != null ? labels.get("version") : null;
        String group = labels != null ? labels.get("group") : null;

        // 转换为 URL 列表（使用 PodMetadataExtractor）
        List<URL> urls = PodMetadataExtractor.extractURLs(endpoints);
        String cacheKey = buildCacheKey(serviceName, group, version);

        // 更新缓存
        if (urls.isEmpty()) {
            serviceCache.remove(cacheKey);
            logger.info("➡️ [Cache] Removed: {} (no ready addresses)", cacheKey);
        } else {
            serviceCache.put(cacheKey, urls);
            logger.info("➡️ [Cache] Updated: {} -> {} providers", cacheKey, urls.size());
        }
    }

    /**
     * 从缓存移除（由 Informer 删除事件触发）
     */
    private void removeFromCache(Endpoints endpoints) {
        String serviceName = endpoints.getMetadata().getName();
        Map<String, String> labels = endpoints.getMetadata().getLabels();

        String version = labels != null ? labels.get("version") : null;
        String group = labels != null ? labels.get("group") : null;

        String cacheKey = buildCacheKey(serviceName, group, version);
        serviceCache.remove(cacheKey);

        logger.info("❌ [Cache] Removed: {} (Endpoints deleted)", cacheKey);
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
}
