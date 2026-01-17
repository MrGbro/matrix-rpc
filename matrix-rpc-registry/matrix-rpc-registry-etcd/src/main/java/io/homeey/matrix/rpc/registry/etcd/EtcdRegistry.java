package io.homeey.matrix.rpc.registry.etcd;

import io.etcd.jetcd.*;
import io.etcd.jetcd.kv.GetResponse;
import io.etcd.jetcd.lease.LeaseKeepAliveResponse;
import io.etcd.jetcd.options.GetOption;
import io.etcd.jetcd.options.PutOption;
import io.etcd.jetcd.options.WatchOption;
import io.etcd.jetcd.watch.WatchEvent;
import io.grpc.stub.StreamObserver;
import io.homeey.matrix.rpc.core.URL;
import io.homeey.matrix.rpc.registry.api.AbstractRegistry;
import io.homeey.matrix.rpc.registry.api.NotifyListener;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

/**
 * Etcd 注册中心实现
 * 
 * <h3>核心特性：</h3>
 * <ul>
 *   <li>Lease 租约：自动续约和心跳保活</li>
 *   <li>Watch 机制：监听 key 变化，实时推送</li>
 *   <li>MVCC：多版本并发控制</li>
 * </ul>
 * 
 * <h3>URL 配置示例：</h3>
 * <pre>
 * etcd://127.0.0.1:2379/io.homeey.example.EchoService
 *   ?ttl=30
 *   &timeout=5000
 * </pre>
 * 
 * <h3>Etcd Key 结构：</h3>
 * <pre>
 * /matrix-rpc                           (根节点)
 *   └─ services                         (服务目录)
 *       └─ io.homeey.example.EchoService (服务接口)
 *           └─ providers                (提供者目录)
 *               ├─ 192.168.1.100:8080   (带 Lease，存储 URL 数据)
 *               └─ 192.168.1.101:8080   (带 Lease，存储 URL 数据)
 * </pre>
 * 
 * @author Matrix RPC Team
 * @since 1.0.0
 */
public class EtcdRegistry extends AbstractRegistry {
    
    private static final String ROOT_PATH = "/matrix-rpc";
    private static final String SERVICES_PATH = ROOT_PATH + "/services";
    
    private final Client etcdClient;
    private final KV kvClient;
    private final Lease leaseClient;
    private final Watch watchClient;
    
    private final long leaseTtl;  // 租约 TTL（秒）
    private final ConcurrentMap<String, Long> leaseIds = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, Watch.Watcher> watchers = new ConcurrentHashMap<>();
    
    public EtcdRegistry(URL registryUrl) {
        super(registryUrl);
        
        String endpoints = registryUrl.getHost() + ":" + 
            (registryUrl.getPort() > 0 ? registryUrl.getPort() : 2379);
        
        this.leaseTtl = registryUrl.getParameter("ttl", 30);
        
        this.etcdClient = Client.builder()
            .endpoints(endpoints)
            .build();
        
        this.kvClient = etcdClient.getKVClient();
        this.leaseClient = etcdClient.getLeaseClient();
        this.watchClient = etcdClient.getWatchClient();
        
        logger.info("Etcd registry initialized: {}, TTL: {}s", endpoints, leaseTtl);
    }
    
    @Override
    protected void doRegister(URL url) {
        try {
            String key = buildServiceKey(url);
            String value = url.toString();
            
            // 创建 Lease
            long leaseId = leaseClient.grant(leaseTtl).get().getID();
            leaseIds.put(key, leaseId);
            
            // 自动续约（Keep Alive）
            leaseClient.keepAlive(leaseId, new StreamObserver<LeaseKeepAliveResponse>() {
                @Override
                public void onNext(LeaseKeepAliveResponse response) {
                    logger.debug("Lease keep alive: {}, TTL: {}", leaseId, response.getTTL());
                }
                
                @Override
                public void onError(Throwable t) {
                    logger.error("Lease keep alive error: {}", leaseId, t);
                }
                
                @Override
                public void onCompleted() {
                    logger.info("Lease keep alive completed: {}", leaseId);
                }
            });
            
            // 注册服务（关联 Lease）
            PutOption putOption = PutOption.newBuilder().withLeaseId(leaseId).build();
            kvClient.put(
                ByteSequence.from(key, StandardCharsets.UTF_8),
                ByteSequence.from(value, StandardCharsets.UTF_8),
                putOption
            ).get();
            
            logger.info("Service registered to Etcd: {}, Lease: {}", key, leaseId);
        } catch (Exception e) {
            throw new RuntimeException("Failed to register service to Etcd", e);
        }
    }
    
    @Override
    protected void doUnregister(URL url) {
        try {
            String key = buildServiceKey(url);
            
            // 删除 key
            kvClient.delete(ByteSequence.from(key, StandardCharsets.UTF_8)).get();
            
            // 撤销 Lease
            Long leaseId = leaseIds.remove(key);
            if (leaseId != null) {
                leaseClient.revoke(leaseId).get();
            }
            
            logger.info("Service unregistered from Etcd: {}", key);
        } catch (Exception e) {
            logger.error("Failed to unregister service from Etcd", e);
        }
    }
    
    @Override
    protected List<URL> doLookup(String serviceInterface, String group, String version) {
        try {
            String keyPrefix = buildServicePrefix(serviceInterface);
            
            GetOption getOption = GetOption.newBuilder()
                .withPrefix(ByteSequence.from(keyPrefix, StandardCharsets.UTF_8))
                .build();
            
            GetResponse response = kvClient.get(
                ByteSequence.from(keyPrefix, StandardCharsets.UTF_8),
                getOption
            ).get();
            
            List<URL> urls = new ArrayList<>();
            for (KeyValue kv : response.getKvs()) {
                try {
                    String value = kv.getValue().toString(StandardCharsets.UTF_8);
                    URL url = URL.valueOf(value);
                    
                    // 过滤 group 和 version
                    if (matchGroupVersion(url, group, version)) {
                        urls.add(url);
                    }
                } catch (Exception e) {
                    logger.warn("Failed to parse URL: {}", kv.getKey(), e);
                }
            }
            
            return urls;
        } catch (Exception e) {
            logger.error("Failed to lookup services from Etcd", e);
            return List.of();
        }
    }
    
    @Override
    protected void doSubscribe(String serviceInterface, NotifyListener listener) {
        try {
            String keyPrefix = buildServicePrefix(serviceInterface);
            
            WatchOption watchOption = WatchOption.newBuilder()
                .withPrefix(ByteSequence.from(keyPrefix, StandardCharsets.UTF_8))
                .build();
            
            Watch.Watcher watcher = watchClient.watch(
                ByteSequence.from(keyPrefix, StandardCharsets.UTF_8),
                watchOption,
                response -> {
                    for (WatchEvent event : response.getEvents()) {
                        logger.debug("Etcd watch event: {}, key: {}", 
                            event.getEventType(), 
                            event.getKeyValue().getKey().toString(StandardCharsets.UTF_8)
                        );
                    }
                    
                    // 服务列表变化，重新查询并通知
                    List<URL> urls = doLookup(serviceInterface, null, null);
                    notifyListeners(buildServiceName(serviceInterface, null, null), urls);
                }
            );
            
            watchers.put(serviceInterface, watcher);
            
            logger.info("Subscribed to Etcd service: {}", serviceInterface);
        } catch (Exception e) {
            throw new RuntimeException("Failed to subscribe to Etcd service", e);
        }
    }
    
    @Override
    public void destroy() {
        try {
            // 关闭所有 watcher
            for (Watch.Watcher watcher : watchers.values()) {
                watcher.close();
            }
            watchers.clear();
            
            // 撤销所有 Lease
            for (Long leaseId : leaseIds.values()) {
                try {
                    leaseClient.revoke(leaseId).get(5, TimeUnit.SECONDS);
                } catch (Exception e) {
                    logger.warn("Failed to revoke lease: {}", leaseId, e);
                }
            }
            leaseIds.clear();
            
            // 关闭客户端
            if (kvClient != null) kvClient.close();
            if (leaseClient != null) leaseClient.close();
            if (watchClient != null) watchClient.close();
            if (etcdClient != null) etcdClient.close();
            
            super.destroy();
            
            logger.info("Etcd registry destroyed");
        } catch (Exception e) {
            logger.error("Failed to destroy Etcd registry", e);
        }
    }
    
    /**
     * 构建服务 key
     */
    private String buildServiceKey(URL url) {
        return buildServicePrefix(url.getPath()) + "/" + url.getAddress().replace(":", "_");
    }
    
    /**
     * 构建服务前缀
     */
    private String buildServicePrefix(String serviceInterface) {
        return SERVICES_PATH + "/" + serviceInterface.replace('.', '/') + "/providers";
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
