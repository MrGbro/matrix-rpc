package io.homeey.matrix.rpc.registry.consul;

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
 * Consul 注册中心集成测试
 * 
 * <h3>运行要求：</h3>
 * <ul>
 *   <li>本地运行 Consul: docker run -d -p 8500:8500 consul:latest</li>
 *   <li>设置环境变量: CONSUL_ENABLED=true</li>
 * </ul>
 * 
 * @author Matrix RPC Team
 * @since 1.0.0
 */
class ConsulRegistryTest {
    
    private ConsulRegistry registry;
    private URL registryUrl;
    
    @BeforeEach
    void setUp() {
        // 创建注册中心 URL
        Map<String, String> params = new HashMap<>();
        params.put("healthCheck", "tcp");
        params.put("healthCheckInterval", "5s");
        params.put("healthCheckTimeout", "3s");
        
        registryUrl = new URL("consul", "127.0.0.1", 8500, null, params);
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
        assertEquals("consul", registryUrl.getProtocol());
        assertEquals("127.0.0.1", registryUrl.getHost());
        assertEquals(8500, registryUrl.getPort());
        assertEquals("tcp", registryUrl.getParameter("healthCheck"));
    }
    
    /**
     * 测试注册中心初始化
     */
    @Test
    @EnabledIfEnvironmentVariable(named = "CONSUL_ENABLED", matches = "true")
    void testRegistryInitialization() {
        registry = new ConsulRegistry(registryUrl);
        assertNotNull(registry);
    }
    
    /**
     * 测试服务注册
     */
    @Test
    @EnabledIfEnvironmentVariable(named = "CONSUL_ENABLED", matches = "true")
    void testServiceRegister() {
        registry = new ConsulRegistry(registryUrl);
        
        // 创建服务 URL
        Map<String, String> params = new HashMap<>();
        params.put("group", "default");
        params.put("version", "1.0.0");
        
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
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
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
    @EnabledIfEnvironmentVariable(named = "CONSUL_ENABLED", matches = "true")
    void testServiceUnregister() throws InterruptedException {
        registry = new ConsulRegistry(registryUrl);
        
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
        Thread.sleep(2000);
        
        // 验证服务已注销
        urls = registry.lookup("io.homeey.example.TestService", "default", "1.0.0");
        boolean found = urls.stream()
            .anyMatch(url -> "192.168.1.101".equals(url.getHost()) && url.getPort() == 8081);
        assertFalse(found, "Unregistered service should not be found");
    }
    
    /**
     * 测试服务订阅和通知
     */
    @Test
    @EnabledIfEnvironmentVariable(named = "CONSUL_ENABLED", matches = "true")
    void testServiceSubscribe() throws InterruptedException {
        registry = new ConsulRegistry(registryUrl);
        
        CountDownLatch latch = new CountDownLatch(1);
        AtomicInteger notifyCount = new AtomicInteger(0);
        
        // 订阅服务
        NotifyListener listener = urls -> {
            notifyCount.incrementAndGet();
            System.out.println("Service changed, count: " + notifyCount.get() + ", size: " + urls.size());
            latch.countDown();
        };
        
        registry.subscribe("io.homeey.example.SubscribeService", listener);
        
        // 注册一个新服务
        Map<String, String> params = new HashMap<>();
        params.put("group", "default");
        params.put("version", "1.0.0");
        
        URL serviceUrl = new URL(
            "matrix",
            "192.168.1.102",
            8082,
            "io.homeey.example.SubscribeService",
            params
        );
        
        registry.register(serviceUrl);
        
        // 等待通知（最多10秒）
        boolean notified = latch.await(10, TimeUnit.SECONDS);
        assertTrue(notified, "Should receive notification");
        assertTrue(notifyCount.get() > 0, "Notify count should be greater than 0");
    }
    
    /**
     * 测试健康检查配置
     */
    @Test
    void testHealthCheckConfiguration() {
        // TCP 健康检查
        Map<String, String> tcpParams = new HashMap<>();
        tcpParams.put("healthCheck", "tcp");
        tcpParams.put("healthCheckInterval", "10s");
        
        URL tcpUrl = new URL("consul", "127.0.0.1", 8500, null, tcpParams);
        assertEquals("tcp", tcpUrl.getParameter("healthCheck"));
        assertEquals("10s", tcpUrl.getParameter("healthCheckInterval"));
        
        // HTTP 健康检查
        Map<String, String> httpParams = new HashMap<>();
        httpParams.put("healthCheck", "http");
        httpParams.put("healthCheckInterval", "15s");
        httpParams.put("healthCheckTimeout", "5s");
        
        URL httpUrl = new URL("consul", "127.0.0.1", 8500, null, httpParams);
        assertEquals("http", httpUrl.getParameter("healthCheck"));
        assertEquals("15s", httpUrl.getParameter("healthCheckInterval"));
    }
    
    /**
     * 测试服务名构建
     */
    @Test
    void testServiceNameBuilding() {
        String serviceName1 = buildServiceName("EchoService", null, null);
        assertEquals("EchoService", serviceName1);
        
        String serviceName2 = buildServiceName("EchoService", "default", null);
        assertEquals("EchoService:default", serviceName2);
        
        String serviceName3 = buildServiceName("EchoService", "default", "1.0.0");
        assertEquals("EchoService:default:1.0.0", serviceName3);
    }
    
    /**
     * 测试多实例注册
     */
    @Test
    @EnabledIfEnvironmentVariable(named = "CONSUL_ENABLED", matches = "true")
    void testMultipleInstances() throws InterruptedException {
        registry = new ConsulRegistry(registryUrl);
        
        Map<String, String> params = new HashMap<>();
        params.put("group", "default");
        params.put("version", "1.0.0");
        
        // 注册多个实例
        URL service1 = new URL("matrix", "192.168.1.201", 8201, 
            "io.homeey.example.MultiService", params);
        URL service2 = new URL("matrix", "192.168.1.202", 8202, 
            "io.homeey.example.MultiService", params);
        URL service3 = new URL("matrix", "192.168.1.203", 8203, 
            "io.homeey.example.MultiService", params);
        
        registry.register(service1);
        registry.register(service2);
        registry.register(service3);
        
        Thread.sleep(2000);
        
        // 查询所有实例
        List<URL> urls = registry.lookup("io.homeey.example.MultiService", "default", "1.0.0");
        assertNotNull(urls);
        assertTrue(urls.size() >= 3, "Should have at least 3 instances");
        
        System.out.println("Found " + urls.size() + " instances");
    }
    
    /**
     * Helper: 构建服务名称
     */
    private String buildServiceName(String serviceInterface, String group, String version) {
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
