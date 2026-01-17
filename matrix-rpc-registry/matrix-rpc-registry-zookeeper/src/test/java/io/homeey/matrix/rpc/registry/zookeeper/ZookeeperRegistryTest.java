package io.homeey.matrix.rpc.registry.zookeeper;

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
 * Zookeeper 注册中心集成测试
 * 
 * <h3>运行要求：</h3>
 * <ul>
 *   <li>本地运行 Zookeeper: docker run -d -p 2181:2181 zookeeper:latest</li>
 *   <li>设置环境变量: ZOOKEEPER_ENABLED=true</li>
 * </ul>
 * 
 * @author Matrix RPC Team
 * @since 1.0.0
 */
class ZookeeperRegistryTest {
    
    private ZookeeperRegistry registry;
    private URL registryUrl;
    
    @BeforeEach
    void setUp() {
        // 创建注册中心 URL
        Map<String, String> params = new HashMap<>();
        params.put("sessionTimeout", "60000");
        params.put("connectionTimeout", "15000");
        params.put("baseSleepTime", "1000");
        params.put("maxRetries", "3");
        
        registryUrl = new URL("zookeeper", "127.0.0.1", 2181, null, params);
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
        assertEquals("zookeeper", registryUrl.getProtocol());
        assertEquals("127.0.0.1", registryUrl.getHost());
        assertEquals(2181, registryUrl.getPort());
        assertEquals("60000", registryUrl.getParameter("sessionTimeout"));
    }
    
    /**
     * 测试注册中心初始化
     */
    @Test
    @EnabledIfEnvironmentVariable(named = "ZOOKEEPER_ENABLED", matches = "true")
    void testRegistryInitialization() {
        registry = new ZookeeperRegistry(registryUrl);
        assertNotNull(registry);
    }
    
    /**
     * 测试服务注册
     */
    @Test
    @EnabledIfEnvironmentVariable(named = "ZOOKEEPER_ENABLED", matches = "true")
    void testServiceRegister() throws InterruptedException {
        registry = new ZookeeperRegistry(registryUrl);
        
        // 创建服务 URL
        Map<String, String> params = new HashMap<>();
        params.put("group", "default");
        params.put("version", "1.0.0");
        params.put("weight", "100");
        
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
    @EnabledIfEnvironmentVariable(named = "ZOOKEEPER_ENABLED", matches = "true")
    void testServiceUnregister() throws InterruptedException {
        registry = new ZookeeperRegistry(registryUrl);
        
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
    @EnabledIfEnvironmentVariable(named = "ZOOKEEPER_ENABLED", matches = "true")
    void testServiceSubscribeAndWatch() throws InterruptedException {
        registry = new ZookeeperRegistry(registryUrl);
        
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
     * 测试临时节点（会话断开自动删除）
     */
    @Test
    @EnabledIfEnvironmentVariable(named = "ZOOKEEPER_ENABLED", matches = "true")
    void testEphemeralNode() throws InterruptedException {
        // 创建第一个注册中心实例并注册服务
        ZookeeperRegistry registry1 = new ZookeeperRegistry(registryUrl);
        
        Map<String, String> params = new HashMap<>();
        params.put("group", "default");
        params.put("version", "1.0.0");
        
        URL serviceUrl = new URL(
            "matrix",
            "192.168.1.103",
            8083,
            "io.homeey.example.EphemeralService",
            params
        );
        
        registry1.register(serviceUrl);
        Thread.sleep(1000);
        
        // 验证服务已注册
        List<URL> urls = registry1.lookup("io.homeey.example.EphemeralService", "default", "1.0.0");
        assertTrue(urls.size() > 0);
        
        // 关闭连接（模拟会话断开）
        registry1.destroy();
        Thread.sleep(3000);
        
        // 使用新实例查询，服务应该已被删除
        ZookeeperRegistry registry2 = new ZookeeperRegistry(registryUrl);
        urls = registry2.lookup("io.homeey.example.EphemeralService", "default", "1.0.0");
        boolean found = urls.stream()
            .anyMatch(url -> "192.168.1.103".equals(url.getHost()) && url.getPort() == 8083);
        assertFalse(found, "Ephemeral node should be deleted after session closed");
        
        registry2.destroy();
    }
    
    /**
     * 测试服务路径构建
     */
    @Test
    void testServicePathBuilding() {
        String path1 = buildServicePath("io.homeey.example.EchoService");
        assertEquals("/matrix-rpc/services/io/homeey/example/EchoService", path1);
        
        String path2 = buildServicePath("TestService");
        assertEquals("/matrix-rpc/services/TestService", path2);
    }
    
    /**
     * 测试多实例注册
     */
    @Test
    @EnabledIfEnvironmentVariable(named = "ZOOKEEPER_ENABLED", matches = "true")
    void testMultipleInstances() throws InterruptedException {
        registry = new ZookeeperRegistry(registryUrl);
        
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
     * 测试连接重试机制
     */
    @Test
    void testRetryConfiguration() {
        Map<String, String> params = new HashMap<>();
        params.put("baseSleepTime", "1000");
        params.put("maxRetries", "5");
        
        URL url = new URL("zookeeper", "127.0.0.1", 2181, null, params);
        
        assertEquals("1000", url.getParameter("baseSleepTime"));
        assertEquals("5", url.getParameter("maxRetries"));
    }
    
    /**
     * 测试 Group 和 Version 过滤
     */
    @Test
    @EnabledIfEnvironmentVariable(named = "ZOOKEEPER_ENABLED", matches = "true")
    void testGroupAndVersionFilter() throws InterruptedException {
        registry = new ZookeeperRegistry(registryUrl);
        
        // 注册不同 group 的服务
        Map<String, String> params1 = new HashMap<>();
        params1.put("group", "group1");
        params1.put("version", "1.0.0");
        
        URL service1 = new URL("matrix", "192.168.1.110", 8110, 
            "io.homeey.example.FilterService", params1);
        registry.register(service1);
        
        Map<String, String> params2 = new HashMap<>();
        params2.put("group", "group2");
        params2.put("version", "1.0.0");
        
        URL service2 = new URL("matrix", "192.168.1.111", 8111, 
            "io.homeey.example.FilterService", params2);
        registry.register(service2);
        
        Thread.sleep(1000);
        
        // 查询 group1 的服务
        List<URL> group1Urls = registry.lookup("io.homeey.example.FilterService", "group1", "1.0.0");
        assertTrue(group1Urls.stream().allMatch(url -> 
            "group1".equals(url.getParameter("group"))));
        
        // 查询 group2 的服务
        List<URL> group2Urls = registry.lookup("io.homeey.example.FilterService", "group2", "1.0.0");
        assertTrue(group2Urls.stream().allMatch(url -> 
            "group2".equals(url.getParameter("group"))));
        
        System.out.println("Group1 instances: " + group1Urls.size());
        System.out.println("Group2 instances: " + group2Urls.size());
    }
    
    /**
     * Helper: 构建服务路径
     */
    private String buildServicePath(String serviceInterface) {
        return "/matrix-rpc/services/" + serviceInterface.replace('.', '/');
    }
}
