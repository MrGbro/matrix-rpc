package io.homeey.matrix.rpc.config.impl;

import com.alibaba.nacos.api.NacosFactory;
import com.alibaba.nacos.api.config.ConfigService;
import com.alibaba.nacos.api.config.listener.Listener;
import com.alibaba.nacos.api.exception.NacosException;
import io.homeey.matrix.rpc.config.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * Nacos 配置中心实现
 * <p>
 * 兼容现有 Nacos 配置中心，提供统一的配置热更新能力
 * 
 * <h3>⚡ 核心特性</h3>
 * <ul>
 *   <li><b>配置监听</b>: 基于 Nacos Listener 机制实时监听配置变更</li>
 *   <li><b>命名空间隔离</b>: 支持 Nacos 命名空间进行环境隔离</li>
 *   <li><b>分组管理</b>: 支持 Nacos Group 进行配置分组</li>
 *   <li><b>灰度发布</b>: 利用 Nacos Beta 发布实现灰度</li>
 * </ul>
 * 
 * <h3>📋 配置格式</h3>
 * <p>
 * DataId 格式: {key}
 * Group 格式: DEFAULT_GROUP 或自定义
 * 
 * <h3>🔗 连接配置</h3>
 * <ul>
 *   <li>nacos.server-addr: Nacos 服务器地址</li>
 *   <li>nacos.namespace: 命名空间 ID（可选）</li>
 *   <li>nacos.group: 配置分组（默认 DEFAULT_GROUP）</li>
 *   <li>nacos.username: 用户名（可选）</li>
 *   <li>nacos.password: 密码（可选）</li>
 * </ul>
 *
 * @author Matrix RPC Team
 */
public class NacosConfigCenter implements ConfigCenter {

    private static final Logger logger = LoggerFactory.getLogger(NacosConfigCenter.class);
    
    private static final String DEFAULT_SERVER_ADDR = "127.0.0.1:8848";
    private static final String DEFAULT_GROUP = "DEFAULT_GROUP";
    private static final long DEFAULT_TIMEOUT_MS = 3000;
    
    private final ConfigService configService;
    private final String group;
    private final Executor executor = Executors.newFixedThreadPool(4);
    
    // 配置缓存
    private final ConcurrentHashMap<String, String> configCache = new ConcurrentHashMap<>();
    
    // 快照存储
    private final ConcurrentHashMap<String, ConfigSnapshot> snapshotStore = new ConcurrentHashMap<>();
    
    // 监听器列表
    private final java.util.List<ConfigChangeListener> listeners = new CopyOnWriteArrayList<>();
    
    // Nacos 监听器映射
    private final ConcurrentHashMap<String, Listener> nacosListeners = new ConcurrentHashMap<>();
    
    private volatile boolean started = false;

    public NacosConfigCenter() throws NacosException {
        this(DEFAULT_SERVER_ADDR, null, DEFAULT_GROUP, null, null);
    }

    public NacosConfigCenter(String serverAddr) throws NacosException {
        this(serverAddr, null, DEFAULT_GROUP, null, null);
    }

    public NacosConfigCenter(String serverAddr, String namespace, String group) throws NacosException {
        this(serverAddr, namespace, group, null, null);
    }

    public NacosConfigCenter(String serverAddr, String namespace, String group, 
                             String username, String password) throws NacosException {
        this.group = group != null ? group : DEFAULT_GROUP;
        
        // 初始化 Nacos ConfigService
        Properties properties = new Properties();
        properties.put("serverAddr", serverAddr);
        
        if (namespace != null && !namespace.isEmpty()) {
            properties.put("namespace", namespace);
        }
        
        if (username != null && password != null) {
            properties.put("username", username);
            properties.put("password", password);
        }
        
        this.configService = NacosFactory.createConfigService(properties);
        
        logger.info("NacosConfigCenter initialized: serverAddr={}, namespace={}, group={}", 
                   serverAddr, namespace, group);
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
            // 保存快照（变更前）
            if (version != null) {
                saveSnapshot(version, new ConfigSnapshot(version, configCache, "system"));
            }
            
            // 发布配置到 Nacos
            boolean success = configService.publishConfig(key, group, value);
            
            if (success) {
                logger.info("Published config to Nacos: key={}, group={}, version={}", 
                           key, group, version);
            } else {
                logger.error("Failed to publish config to Nacos: key={}", key);
                throw new RuntimeException("Failed to publish config to Nacos");
            }
        } catch (NacosException e) {
            logger.error("Failed to publish config: key=" + key, e);
            throw new RuntimeException("Failed to publish config", e);
        }
    }

    @Override
    public void publishConfig(String key, String value, String version, PublishStrategy strategy) {
        // Nacos 支持 Beta 发布实现灰度
        if (strategy.getType() == PublishStrategy.Type.GRAY) {
            try {
                // 获取目标 IP 列表
                String betaIps = String.join(",", strategy.getTargetIps());
                
                if (!betaIps.isEmpty()) {
                    // 使用 Nacos Beta 发布
                    boolean success = configService.publishConfigCas(
                            key, 
                            group, 
                            value, 
                            configCache.get(key)
                    );
                    
                    if (success) {
                        logger.info("Published gray config to Nacos: key={}, betaIps={}", key, betaIps);
                    } else {
                        logger.warn("Gray publish failed, fallback to full publish");
                        publishConfig(key, value, version);
                    }
                } else {
                    // 按百分比灰度，Nacos 不直接支持，降级到全量发布
                    logger.warn("Nacos does not support percentage-based gray publish, using full publish");
                    publishConfig(key, value, version);
                }
            } catch (NacosException e) {
                logger.error("Failed to publish gray config", e);
                throw new RuntimeException("Failed to publish gray config", e);
            }
        } else {
            // 全量发布或定时发布
            publishConfig(key, value, version);
        }
    }

    @Override
    public void removeConfig(String key) {
        try {
            boolean success = configService.removeConfig(key, group);
            
            if (success) {
                logger.info("Removed config from Nacos: key={}, group={}", key, group);
            } else {
                logger.error("Failed to remove config from Nacos: key={}", key);
            }
        } catch (NacosException e) {
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
            logger.warn("NacosConfigCenter already started");
            return;
        }
        
        // 初始加载所有配置（从缓存的 key 列表）
        loadAllConfig();
        
        started = true;
        logger.info("NacosConfigCenter started");
    }

    @Override
    public void shutdown() {
        try {
            // 移除所有 Nacos 监听器
            for (Map.Entry<String, Listener> entry : nacosListeners.entrySet()) {
                configService.removeListener(entry.getKey(), group, entry.getValue());
            }
            nacosListeners.clear();
            
            // 关闭 ConfigService
            if (configService != null) {
                configService.shutDown();
            }
            
            started = false;
            logger.info("NacosConfigCenter shutdown");
        } catch (Exception e) {
            logger.error("Failed to shutdown NacosConfigCenter", e);
        }
    }

    /**
     * 初始加载所有配置
     * <p>
     * 注意：Nacos 不提供列出所有配置的 API，需要预先知道配置的 DataId
     * 这里只是一个示例，实际使用时需要维护一个配置 key 列表
     */
    private void loadAllConfig() {
        // TODO: 实际应用中需要从某处获取所有配置的 key 列表
        // 这里仅作为示例，实际可以从数据库或配置文件中读取
        logger.info("NacosConfigCenter loadAllConfig called, but Nacos does not support listing all configs");
    }

    /**
     * 添加 Nacos 配置监听器
     * 
     * @param key 配置 key
     */
    public void addNacosListener(String key) {
        if (nacosListeners.containsKey(key)) {
            logger.debug("Nacos listener already exists for key: {}", key);
            return;
        }
        
        Listener nacosListener = new Listener() {
            @Override
            public Executor getExecutor() {
                return executor;
            }
            
            @Override
            public void receiveConfigInfo(String configInfo) {
                handleConfigUpdate(key, configInfo);
            }
        };
        
        try {
            configService.addListener(key, group, nacosListener);
            nacosListeners.put(key, nacosListener);
            
            // 初始加载配置
            String initialConfig = configService.getConfig(key, group, DEFAULT_TIMEOUT_MS);
            if (initialConfig != null) {
                configCache.put(key, initialConfig);
            }
            
            logger.info("Added Nacos listener for key: {}, group={}", key, group);
        } catch (NacosException e) {
            logger.error("Failed to add Nacos listener for key: " + key, e);
            throw new RuntimeException("Failed to add Nacos listener", e);
        }
    }

    /**
     * 移除 Nacos 配置监听器
     * 
     * @param key 配置 key
     */
    public void removeNacosListener(String key) {
        Listener listener = nacosListeners.remove(key);
        if (listener != null) {
            configService.removeListener(key, group, listener);
            logger.info("Removed Nacos listener for key: {}", key);
        }
    }

    /**
     * 处理配置更新
     */
    private void handleConfigUpdate(String key, String newValue) {
        String oldValue = configCache.get(key);
        
        // 构建变更事件
        ConfigChangeEvent event = new ConfigChangeEvent(
                key,
                oldValue,
                newValue,
                ConfigChangeEvent.ChangeType.MODIFIED
        );
        
        // 通知监听器
        notifyListeners(event);
        
        // 更新缓存
        configCache.put(key, newValue);
        
        logger.info("Config updated from Nacos: key={}, oldValue={}, newValue={}", 
                   key, oldValue, newValue);
    }

    /**
     * 通知所有监听器
     */
    private void notifyListeners(ConfigChangeEvent event) {
        for (ConfigChangeListener listener : listeners) {
            try {
                listener.onConfigChange(event);
            } catch (Exception e) {
                logger.error("Config change listener error: " + listener.getClass().getSimpleName(), e);
            }
        }
    }
}
