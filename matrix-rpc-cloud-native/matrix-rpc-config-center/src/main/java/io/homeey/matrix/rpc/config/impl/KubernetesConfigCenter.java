package io.homeey.matrix.rpc.config.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientBuilder;
import io.fabric8.kubernetes.client.informers.ResourceEventHandler;
import io.fabric8.kubernetes.client.informers.SharedIndexInformer;
import io.fabric8.kubernetes.client.informers.SharedInformerFactory;
import io.homeey.matrix.rpc.config.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Kubernetes ConfigMap-based Configuration Center
 * 基于 ConfigMap 的配置中心实现
 */
public class KubernetesConfigCenter implements ConfigCenter {

    private static final Logger logger = LoggerFactory.getLogger(KubernetesConfigCenter.class);
    private static final String DEFAULT_NAMESPACE = "default";
    private static final String DEFAULT_CONFIGMAP_NAME = "matrix-rpc-config";

    private final KubernetesClient kubeClient;
    private final SharedInformerFactory informerFactory;
    private final String namespace;
    private final String configMapName;

    // 配置缓存
    private final ConcurrentHashMap<String, String> configCache = new ConcurrentHashMap<>();
    
    // 快照存储
    private final ConcurrentHashMap<String, ConfigSnapshot> snapshotStore = new ConcurrentHashMap<>();
    
    // 监听器列表
    private final List<ConfigChangeListener> listeners = new CopyOnWriteArrayList<>();
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    private volatile boolean started = false;

    public KubernetesConfigCenter() {
        this(DEFAULT_NAMESPACE, DEFAULT_CONFIGMAP_NAME);
    }

    public KubernetesConfigCenter(String namespace, String configMapName) {
        this.namespace = namespace;
        this.configMapName = configMapName;
        
        // 初始化 Kubernetes Client
        io.fabric8.kubernetes.client.Config config = new io.fabric8.kubernetes.client.ConfigBuilder()
                .withNamespace(namespace)
                .build();
        this.kubeClient = new KubernetesClientBuilder()
                .withConfig(config)
                .build();
        
        // 创建 Informer Factory
        this.informerFactory = kubeClient.informers();
        
        logger.info("KubernetesConfigCenter initialized: namespace={}, configMap={}", 
                   namespace, configMapName);
    }

    @Override
    public String getConfig(String key) {
        return configCache.get(key);
    }

    @Override
    public Map<String, String> getAllConfig() {
        return new HashMap<>(configCache);
    }

    @Override
    public void publishConfig(String key, String value, String version) {
        try {
            ConfigMap configMap = kubeClient.configMaps()
                    .inNamespace(namespace)
                    .withName(configMapName)
                    .get();
            
            if (configMap == null) {
                // 创建新的 ConfigMap
                configMap = new ConfigMap();
                configMap.getMetadata().setName(configMapName);
                configMap.getMetadata().setNamespace(namespace);
                configMap.setData(new HashMap<>());
            }
            
            // 保存快照（变更前）
            if (version != null) {
                saveSnapshot(version, new ConfigSnapshot(version, configCache, "system"));
            }
            
            // 更新配置
            configMap.getData().put(key, value);
            
            kubeClient.configMaps()
                    .inNamespace(namespace)
                    .resource(configMap)
                    .createOrReplace();
            
            logger.info("Published config: key={}, version={}", key, version);
        } catch (Exception e) {
            logger.error("Failed to publish config: key=" + key, e);
            throw new RuntimeException("Failed to publish config", e);
        }
    }

    @Override
    public void removeConfig(String key) {
        try {
            ConfigMap configMap = kubeClient.configMaps()
                    .inNamespace(namespace)
                    .withName(configMapName)
                    .get();
            
            if (configMap != null && configMap.getData() != null) {
                configMap.getData().remove(key);
                
                kubeClient.configMaps()
                        .inNamespace(namespace)
                        .resource(configMap)
                        .createOrReplace();
                
                logger.info("Removed config: key={}", key);
            }
        } catch (Exception e) {
            logger.error("Failed to remove config: key=" + key, e);
            throw new RuntimeException("Failed to remove config", e);
        }
    }

    @Override
    public void addListener(ConfigChangeListener listener) {
        listeners.add(listener);
        logger.debug("Added config change listener: {}", listener.getClass().getSimpleName());
    }

    @Override
    public void removeListener(ConfigChangeListener listener) {
        listeners.remove(listener);
        logger.debug("Removed config change listener: {}", listener.getClass().getSimpleName());
    }

    @Override
    public ConfigSnapshot getSnapshot(String version) {
        return snapshotStore.get(version);
    }

    @Override
    public void saveSnapshot(String version, ConfigSnapshot snapshot) {
        snapshotStore.put(version, snapshot);
        logger.info("Saved config snapshot: version={}", version);
    }

    @Override
    public boolean rollback(String version) {
        ConfigSnapshot snapshot = snapshotStore.get(version);
        if (snapshot == null) {
            logger.warn("Snapshot not found: version={}", version);
            return false;
        }
        
        try {
            // 回滚所有配置
            for (Map.Entry<String, String> entry : snapshot.getConfig().entrySet()) {
                publishConfig(entry.getKey(), entry.getValue(), null);
            }
            
            logger.info("Successfully rolled back to version: {}", version);
            return true;
        } catch (Exception e) {
            logger.error("Failed to rollback to version: " + version, e);
            return false;
        }
    }

    @Override
    public synchronized void start() {
        if (started) {
            logger.warn("KubernetesConfigCenter already started");
            return;
        }
        
        // 创建 ConfigMap Informer
        SharedIndexInformer<ConfigMap> configMapInformer = informerFactory.sharedIndexInformerFor(
                ConfigMap.class,
                30 * 1000L  // 30秒全量同步一次
        );
        
        // 注册事件处理器
        configMapInformer.addEventHandler(new ResourceEventHandler<ConfigMap>() {
            @Override
            public void onAdd(ConfigMap configMap) {
                if (isTargetConfigMap(configMap)) {
                    updateCache(configMap);
                }
            }
            
            @Override
            public void onUpdate(ConfigMap oldConfigMap, ConfigMap newConfigMap) {
                if (isTargetConfigMap(newConfigMap)) {
                    updateCache(newConfigMap);
                }
            }
            
            @Override
            public void onDelete(ConfigMap configMap, boolean deletedFinalStateUnknown) {
                if (isTargetConfigMap(configMap)) {
                    clearCache();
                }
            }
        });
        
        // 启动 Informer
        informerFactory.startAllRegisteredInformers();
        started = true;
        
        logger.info("KubernetesConfigCenter started successfully");
    }

    @Override
    public void shutdown() {
        if (!started) {
            return;
        }
        
        informerFactory.stopAllRegisteredInformers();
        kubeClient.close();
        started = false;
        
        logger.info("KubernetesConfigCenter shutdown successfully");
    }

    private boolean isTargetConfigMap(ConfigMap configMap) {
        return configMapName.equals(configMap.getMetadata().getName()) &&
               namespace.equals(configMap.getMetadata().getNamespace());
    }

    private void updateCache(ConfigMap configMap) {
        Map<String, String> data = configMap.getData();
        if (data == null) {
            return;
        }
        
        // 检测变更并触发事件
        for (Map.Entry<String, String> entry : data.entrySet()) {
            String key = entry.getKey();
            String newValue = entry.getValue();
            String oldValue = configCache.get(key);
            
            if (oldValue == null) {
                // 新增配置
                configCache.put(key, newValue);
                notifyListeners(new ConfigChangeEvent(key, null, newValue, 
                               ConfigChangeEvent.ChangeType.ADDED));
            } else if (!oldValue.equals(newValue)) {
                // 修改配置
                configCache.put(key, newValue);
                notifyListeners(new ConfigChangeEvent(key, oldValue, newValue, 
                               ConfigChangeEvent.ChangeType.MODIFIED));
            }
        }
        
        // 检测删除的配置
        Set<String> deletedKeys = new HashSet<>(configCache.keySet());
        deletedKeys.removeAll(data.keySet());
        for (String key : deletedKeys) {
            String oldValue = configCache.remove(key);
            notifyListeners(new ConfigChangeEvent(key, oldValue, null, 
                           ConfigChangeEvent.ChangeType.DELETED));
        }
        
        logger.debug("Updated config cache: {} entries", configCache.size());
    }

    private void clearCache() {
        for (String key : configCache.keySet()) {
            String oldValue = configCache.remove(key);
            notifyListeners(new ConfigChangeEvent(key, oldValue, null, 
                           ConfigChangeEvent.ChangeType.DELETED));
        }
        logger.info("Cleared config cache");
    }

    private void notifyListeners(ConfigChangeEvent event) {
        for (ConfigChangeListener listener : listeners) {
            try {
                listener.onConfigChange(event);
            } catch (Exception e) {
                logger.error("Error notifying listener: " + listener.getClass().getSimpleName(), e);
            }
        }
    }
}
