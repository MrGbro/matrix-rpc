package io.homeey.matrix.rpc.registry.etcd;

import io.homeey.matrix.rpc.core.URL;
import io.homeey.matrix.rpc.registry.api.NotifyListener;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Etcd 注册中心集成测试
 * 
 * <h3>运行要求：</h3>
 * <ul>
 *   <li>本地运行 Etcd: docker run -d -p 2379:2379 -p 2380:2380 \
 *       quay.io/coreos/etcd:latest \
 *       /usr/local/bin/etcd \
 *       --listen-client-urls http://0.0.0.0:2379 \
 *       --advertise-client-urls http://0.0.0.0:2379</li>
 *   <li>设置环境变量: ETCD_ENABLED=true</li>
 * </ul>
 * 
 * @author Matrix RPC Team
 * @since 1.0.0
 */
class EtcdRegistryTest {
    
    private EtcdRegistry registry;
    private URL registryUrl;
    
    @BeforeEach
    void setUp() {
        // 创建注册中心 URL
        Map<String, String> params = new HashMap<>();
        params.put("ttl", "30");
        params.put("timeout", "5000");
        
        registryUrl = new URL("etcd", "127.0.0.1", 2379, null, params);
    }
    
    @AfterEach
    void tearDown() {
        if (registry != null) {
            registry.destroy();
        }
    }
    
    /**
     * 测试 URL 构建
     */
    @Test
    void testUrlConstruction() {
        assertNotNull(registryUrl);
        assertEquals("etcd", registryUrl.getProtocol());
        assertEquals("127.0.0.1", registryUrl.getHost());
        assertEquals(2379, registryUrl.getPort());
        assertEquals("30", registryUrl.getParameter("ttl"));
    }
    
    /**
     * 测试注册中心初始化
     */
    @Test
    @EnabledIfEnvironmentVariable(named = "ETCD_ENABLED", matches = "true")
    void testRegistryInitialization() {
        registry = new EtcdRegistry(registryUrl);
        assertNotNull(registry);
    }
    
    /**
     * 测试服务注册
     */
    @Test
    @EnabledIfEnvironmentVariable(named = "ETCD_ENABLED", matches = "true")
    void testServiceRegister() throws InterruptedException {
        registry = new EtcdRegistry(registryUrl);
        
        // 创建服务 URL
        Map<String, String> params = new HashMap<>();
        params.put("group", "default");
        params.put("version", "1.0.0");
        params.put("serialization", "protobuf");
        
        URL serviceUrl = new URL(
            "matrix",
            "192.168.1.100",
            8080,
            "io.homeey.example.EchoService",
            params
        );
        
        // 注册服务
        assertDoesNotThrow(() -> registry.register(serviceUrl));
        
        // 等待注册生效
        Thread.sleep(1000);
        
        // 查询服务
        List<URL> urls = registry.lookup("io.homeey.example.EchoService", "default", "1.0.0");
        assertNotNull(urls);
        assertTrue(urls.size() > 0);
        
        // 验证查询结果
        boolean found = urls.stream()
            .anyMatch(url -> "192.168.1.100".equals(url.getHost()) && url.getPort() == 8080);
        assertTrue(found, "Registered service should be found");
    }
    
    /**
     * 测试服务注销
     */
    @Test
    @EnabledIfEnvironmentVariable(named = "ETCD_ENABLED", matches = "true")
    void testServiceUnregister() throws InterruptedException {
        registry = new EtcdRegistry(registryUrl);
        
        Map<String, String> params = new HashMap<>();
        params.put("group", "default");
        params.put("version", "1.0.0");
        
        URL serviceUrl = new URL(
            "matrix",
            "192.168.1.101",
            8081,
            "io.homeey.example.TestService",
            params
        );
        
        // 注册服务
        registry.register(serviceUrl);
        Thread.sleep(1000);
        
        // 验证服务已注册
        List<URL> urls = registry.lookup("io.homeey.example.TestService", "default", "1.0.0");
        assertTrue(urls.size() > 0);
        
        // 注销服务
        registry.unregister(serviceUrl);
        Thread.sleep(1000);
        
        // 验证服务已注销
        urls = registry.lookup("io.homeey.example.TestService", "default", "1.0.0");
        boolean found = urls.stream()
            .anyMatch(url -> "192.168.1.101".equals(url.getHost()) && url.getPort() == 8081);
        assertFalse(found, "Unregistered service should not be found");
    }
    
    /**
     * 测试服务订阅和 Watch 机制
     */
    @Test
    @EnabledIfEnvironmentVariable(named = "ETCD_ENABLED", matches = "true")
    void testServiceSubscribeAndWatch() throws InterruptedException {
        registry = new EtcdRegistry(registryUrl);
        
        CountDownLatch latch = new CountDownLatch(1);
        AtomicInteger notifyCount = new AtomicInteger(0);
        
        // 订阅服务
        NotifyListener listener = urls -> {
            int count = notifyCount.incrementAndGet();
            System.out.println("Service changed notification #" + count + ", size: " + urls.size());
            latch.countDown();
        };
        
        registry.subscribe("io.homeey.example.WatchService", listener);
        
        // 等待订阅生效
        Thread.sleep(1000);
        
        // 注册一个新服务（触发 Watch）
        Map<String, String> params = new HashMap<>();
        params.put("group", "default");
        params.put("version", "1.0.0");
        
        URL serviceUrl = new URL(
            "matrix",
            "192.168.1.102",
            8082,
            "io.homeey.example.WatchService",
            params
        );
        
        registry.register(serviceUrl);
        
        // 等待 Watch 通知（最多10秒）
        boolean notified = latch.await(10, TimeUnit.SECONDS);
        assertTrue(notified, "Should receive watch notification");
        assertTrue(notifyCount.get() > 0, "Notify count should be greater than 0");
    }
    
    /**
     * 测试 Lease 自动续约
     */
    @Test
    @EnabledIfEnvironmentVariable(named = "ETCD_ENABLED", matches = "true")
    void testLeaseKeepAlive() throws InterruptedException {
        // 创建短 TTL 的注册中心
        Map<String, String> params = new HashMap<>();
        params.put("ttl", "5");  // 5秒 TTL
        
        URL shortTtlUrl = new URL("etcd", "127.0.0.1", 2379, null, params);
        registry = new EtcdRegistry(shortTtlUrl);
        
        Map<String, String> serviceParams = new HashMap<>();
        serviceParams.put("group", "default");
        serviceParams.put("version", "1.0.0");
        
        URL serviceUrl = new URL(
            "matrix",
            "192.168.1.103",
            8083,
            "io.homeey.example.LeaseService",
            serviceParams
        );
        
        // 注册服务
        registry.register(serviceUrl);
        Thread.sleep(1000);
        
        // 验证服务已注册
        List<URL> urls = registry.lookup("io.homeey.example.LeaseService", "default", "1.0.0");
        assertTrue(urls.size() > 0);
        
        // 等待超过 TTL 时间（但 Keep Alive 应该保持服务存活）
        Thread.sleep(8000);
        
        // 验证服务仍然存在
        urls = registry.lookup("io.homeey.example.LeaseService", "default", "1.0.0");
        boolean found = urls.stream()
            .anyMatch(url -> "192.168.1.103".equals(url.getHost()) && url.getPort() == 8083);
        assertTrue(found, "Service should still exist due to keep alive");
    }
    
    /**
     * 测试 Lease 过期（停止 Keep Alive）
     */
    @Test
    @EnabledIfEnvironmentVariable(named = "ETCD_ENABLED", matches = "true")
    void testLeaseExpiration() throws InterruptedException {
        // 创建短 TTL 的注册中心
        Map<String, String> params = new HashMap<>();
        params.put("ttl", "3");  // 3秒 TTL
        
        URL shortTtlUrl = new URL("etcd", "127.0.0.1", 2379, null, params);
        EtcdRegistry shortRegistry = new EtcdRegistry(shortTtlUrl);
        
        Map<String, String> serviceParams = new HashMap<>();
        serviceParams.put("group", "default");
        serviceParams.put("version", "1.0.0");
        
        URL serviceUrl = new URL(
            "matrix",
            "192.168.1.104",
            8084,
            "io.homeey.example.ExpirationService",
            serviceParams
        );
        
        // 注册服务
        shortRegistry.register(serviceUrl);
        Thread.sleep(1000);
        
        // 验证服务已注册
        List<URL> urls = shortRegistry.lookup("io.homeey.example.ExpirationService", "default", "1.0.0");
        assertTrue(urls.size() > 0);
        
        // 关闭注册中心（停止 Keep Alive）
        shortRegistry.destroy();
        
        // 等待 Lease 过期
        Thread.sleep(5000);
        
        // 使用新实例查询，服务应该已过期
        registry = new EtcdRegistry(registryUrl);
        urls = registry.lookup("io.homeey.example.ExpirationService", "default", "1.0.0");
        boolean found = urls.stream()
            .anyMatch(url -> "192.168.1.104".equals(url.getHost()) && url.getPort() == 8084);
        assertFalse(found, "Service should be expired after lease TTL");
    }
    
    /**
     * 测试服务 Key 构建
     */
    @Test
    void testServiceKeyBuilding() {
        String key1 = buildServiceKey("io.homeey.example.EchoService", "192.168.1.100", 8080);
        assertEquals("/matrix-rpc/services/io/homeey/example/EchoService/providers/192.168.1.100_8080", key1);
        
        String key2 = buildServiceKey("TestService", "10.0.0.1", 9090);
        assertEquals("/matrix-rpc/services/TestService/providers/10.0.0.1_9090", key2);
    }
    
    /**
     * 测试多实例注册
     */
    @Test
    @EnabledIfEnvironmentVariable(named = "ETCD_ENABLED", matches = "true")
    void testMultipleInstances() throws InterruptedException {
        registry = new EtcdRegistry(registryUrl);
        
        Map<String, String> params = new HashMap<>();
        params.put("group", "default");
        params.put("version", "1.0.0");
        
        // 注册多个实例
        for (int i = 0; i < 5; i++) {
            URL serviceUrl = new URL(
                "matrix",
                "192.168.1." + (200 + i),
                8200 + i,
                "io.homeey.example.ClusterService",
                params
            );
            registry.register(serviceUrl);
        }
        
        Thread.sleep(2000);
        
        // 查询所有实例
        List<URL> urls = registry.lookup("io.homeey.example.ClusterService", "default", "1.0.0");
        assertNotNull(urls);
        assertTrue(urls.size() >= 5, "Should have at least 5 instances");
        
        System.out.println("Registered instances: " + urls.size());
        urls.forEach(url -> System.out.println("  - " + url.getAddress()));
    }
    
    /**
     * 测试不同 Group 和 Version 的隔离
     */
    @Test
    @EnabledIfEnvironmentVariable(named = "ETCD_ENABLED", matches = "true")
    void testGroupAndVersionIsolation() throws InterruptedException {
        registry = new EtcdRegistry(registryUrl);
        
        // 注册不同版本的服务
        Map<String, String> params1 = new HashMap<>();
        params1.put("group", "default");
        params1.put("version", "1.0.0");
        
        URL service1 = new URL("matrix", "192.168.1.110", 8110, 
            "io.homeey.example.VersionService", params1);
        registry.register(service1);
        
        Map<String, String> params2 = new HashMap<>();
        params2.put("group", "default");
        params2.put("version", "2.0.0");
        
        URL service2 = new URL("matrix", "192.168.1.111", 8111, 
            "io.homeey.example.VersionService", params2);
        registry.register(service2);
        
        Thread.sleep(1000);
        
        // 查询 v1.0.0 的服务
        List<URL> v1Urls = registry.lookup("io.homeey.example.VersionService", "default", "1.0.0");
        assertTrue(v1Urls.stream().allMatch(url -> 
            "1.0.0".equals(url.getParameter("version"))));
        
        // 查询 v2.0.0 的服务
        List<URL> v2Urls = registry.lookup("io.homeey.example.VersionService", "default", "2.0.0");
        assertTrue(v2Urls.stream().allMatch(url -> 
            "2.0.0".equals(url.getParameter("version"))));
        
        System.out.println("V1.0.0 instances: " + v1Urls.size());
        System.out.println("V2.0.0 instances: " + v2Urls.size());
    }
    
    /**
     * 测试 TTL 配置
     */
    @Test
    void testTtlConfiguration() {
        Map<String, String> params = new HashMap<>();
        params.put("ttl", "60");
        
        URL url = new URL("etcd", "127.0.0.1", 2379, null, params);
        assertEquals("60", url.getParameter("ttl"));
        
        // 测试默认值
        URL defaultUrl = new URL("etcd", "127.0.0.1", 2379, null, new HashMap<>());
        assertEquals("30", defaultUrl.getParameter("ttl", "30"));
    }
    
    /**
     * Helper: 构建服务 Key
     */
    private String buildServiceKey(String serviceInterface, String host, int port) {
        String prefix = "/matrix-rpc/services/" + serviceInterface.replace('.', '/') + "/providers";
        String address = host + "_" + port;
        return prefix + "/" + address;
    }
}
