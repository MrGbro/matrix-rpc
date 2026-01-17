package io.homeey.matrix.rpc.registry.zookeeper;

import io.homeey.matrix.rpc.core.URL;
import io.homeey.matrix.rpc.registry.api.AbstractRegistry;
import io.homeey.matrix.rpc.registry.api.NotifyListener;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.recipes.cache.PathChildrenCache;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheEvent;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.zookeeper.CreateMode;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Zookeeper 注册中心实现
 * 
 * <h3>核心特性：</h3>
 * <ul>
 *   <li>临时节点：服务注册使用临时节点，自动下线</li>
 *   <li>Watch 机制：监听服务变化，实时推送</li>
 *   <li>连接重试：Curator 自动重连</li>
 * </ul>
 * 
 * <h3>URL 配置示例：</h3>
 * <pre>
 * zookeeper://127.0.0.1:2181/io.homeey.example.EchoService
 *   ?sessionTimeout=60000
 *   &connectionTimeout=15000
 *   &baseSleepTime=1000
 *   &maxRetries=3
 * </pre>
 * 
 * <h3>ZooKeeper 节点结构：</h3>
 * <pre>
 * /matrix-rpc                           (根节点)
 *   └─ services                         (服务目录)
 *       └─ io.homeey.example.EchoService (服务接口)
 *           └─ providers                (提供者目录)
 *               ├─ 192.168.1.100:8080   (临时节点，存储 URL 数据)
 *               └─ 192.168.1.101:8080   (临时节点，存储 URL 数据)
 * </pre>
 * 
 * @author Matrix RPC Team
 * @since 1.0.0
 */
public class ZookeeperRegistry extends AbstractRegistry {
    
    private static final String ROOT_PATH = "/matrix-rpc";
    private static final String SERVICES_PATH = ROOT_PATH + "/services";
    
    private final CuratorFramework zkClient;
    private final ConcurrentMap<String, PathChildrenCache> watchers = new ConcurrentHashMap<>();
    
    public ZookeeperRegistry(URL registryUrl) {
        super(registryUrl);
        
        String connectString = registryUrl.getHost() + ":" + 
            (registryUrl.getPort() > 0 ? registryUrl.getPort() : 2181);
        
        int sessionTimeout = registryUrl.getParameter("sessionTimeout", 60000);
        int connectionTimeout = registryUrl.getParameter("connectionTimeout", 15000);
        int baseSleepTime = registryUrl.getParameter("baseSleepTime", 1000);
        int maxRetries = registryUrl.getParameter("maxRetries", 3);
        
        this.zkClient = CuratorFrameworkFactory.builder()
            .connectString(connectString)
            .sessionTimeoutMs(sessionTimeout)
            .connectionTimeoutMs(connectionTimeout)
            .retryPolicy(new ExponentialBackoffRetry(baseSleepTime, maxRetries))
            .build();
        
        this.zkClient.start();
        
        logger.info("Zookeeper registry initialized: {}", connectString);
    }
    
    @Override
    protected void doRegister(URL url) {
        try {
            String servicePath = buildServicePath(url.getPath());
            String providerPath = servicePath + "/providers";
            String nodePath = providerPath + "/" + buildNodeName(url);
            
            // 确保父节点存在（持久节点）
            createPersistentIfNotExists(servicePath);
            createPersistentIfNotExists(providerPath);
            
            // 创建临时节点（存储完整的 URL 信息）
            zkClient.create()
                .creatingParentsIfNeeded()
                .withMode(CreateMode.EPHEMERAL)
                .forPath(nodePath, url.toString().getBytes(StandardCharsets.UTF_8));
            
            logger.info("Service registered to Zookeeper: {}", nodePath);
        } catch (Exception e) {
            throw new RuntimeException("Failed to register service to Zookeeper", e);
        }
    }
    
    @Override
    protected void doUnregister(URL url) {
        try {
            String servicePath = buildServicePath(url.getPath());
            String nodePath = servicePath + "/providers/" + buildNodeName(url);
            
            zkClient.delete().forPath(nodePath);
            
            logger.info("Service unregistered from Zookeeper: {}", nodePath);
        } catch (Exception e) {
            logger.error("Failed to unregister service from Zookeeper", e);
        }
    }
    
    @Override
    protected List<URL> doLookup(String serviceInterface, String group, String version) {
        try {
            String servicePath = buildServicePath(serviceInterface);
            String providerPath = servicePath + "/providers";
            
            // 检查路径是否存在
            if (zkClient.checkExists().forPath(providerPath) == null) {
                return List.of();
            }
            
            List<String> children = zkClient.getChildren().forPath(providerPath);
            List<URL> urls = new ArrayList<>();
            
            for (String child : children) {
                try {
                    byte[] data = zkClient.getData().forPath(providerPath + "/" + child);
                    String urlString = new String(data, StandardCharsets.UTF_8);
                    URL url = URL.valueOf(urlString);
                    
                    // 过滤 group 和 version
                    if (matchGroupVersion(url, group, version)) {
                        urls.add(url);
                    }
                } catch (Exception e) {
                    logger.warn("Failed to parse URL from node: {}", child, e);
                }
            }
            
            return urls;
        } catch (Exception e) {
            logger.error("Failed to lookup services from Zookeeper", e);
            return List.of();
        }
    }
    
    @Override
    protected void doSubscribe(String serviceInterface, NotifyListener listener) {
        try {
            String servicePath = buildServicePath(serviceInterface);
            String providerPath = servicePath + "/providers";
            
            // 确保路径存在
            createPersistentIfNotExists(servicePath);
            createPersistentIfNotExists(providerPath);
            
            // 创建 PathChildrenCache 监听子节点变化
            PathChildrenCache watcher = new PathChildrenCache(zkClient, providerPath, true);
            
            watcher.getListenable().addListener((client, event) -> {
                if (event.getType() == PathChildrenCacheEvent.Type.CHILD_ADDED ||
                    event.getType() == PathChildrenCacheEvent.Type.CHILD_REMOVED ||
                    event.getType() == PathChildrenCacheEvent.Type.CHILD_UPDATED) {
                    
                    // 服务列表变化，重新查询并通知
                    List<URL> urls = doLookup(serviceInterface, null, null);
                    notifyListeners(buildServiceName(serviceInterface, null, null), urls);
                }
            });
            
            watcher.start(PathChildrenCache.StartMode.POST_INITIALIZED_EVENT);
            watchers.put(serviceInterface, watcher);
            
            logger.info("Subscribed to Zookeeper service: {}", serviceInterface);
        } catch (Exception e) {
            throw new RuntimeException("Failed to subscribe to Zookeeper service", e);
        }
    }
    
    @Override
    public void destroy() {
        try {
            // 关闭所有 watcher
            for (PathChildrenCache watcher : watchers.values()) {
                watcher.close();
            }
            watchers.clear();
            
            // 关闭 Zookeeper 客户端
            if (zkClient != null) {
                zkClient.close();
            }
            
            super.destroy();
            
            logger.info("Zookeeper registry destroyed");
        } catch (Exception e) {
            logger.error("Failed to destroy Zookeeper registry", e);
        }
    }
    
    /**
     * 构建服务路径
     */
    private String buildServicePath(String serviceInterface) {
        return SERVICES_PATH + "/" + serviceInterface.replace('.', '/');
    }
    
    /**
     * 构建节点名称
     */
    private String buildNodeName(URL url) {
        return url.getAddress().replace(":", "_");
    }
    
    /**
     * 创建持久节点（如果不存在）
     */
    private void createPersistentIfNotExists(String path) throws Exception {
        if (zkClient.checkExists().forPath(path) == null) {
            zkClient.create()
                .creatingParentsIfNeeded()
                .withMode(CreateMode.PERSISTENT)
                .forPath(path);
        }
    }
    
    /**
     * 匹配 group 和 version
     */
    private boolean matchGroupVersion(URL url, String group, String version) {
        if (group != null && !group.equals(url.getParameter("group", "default"))) {
            return false;
        }
        if (version != null && !version.equals(url.getParameter("version", "1.0.0"))) {
            return false;
        }
        return true;
    }
}
