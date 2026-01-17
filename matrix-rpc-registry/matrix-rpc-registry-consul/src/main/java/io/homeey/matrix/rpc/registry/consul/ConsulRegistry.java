package io.homeey.matrix.rpc.registry.consul;

import com.ecwid.consul.v1.ConsulClient;
import com.ecwid.consul.v1.QueryParams;
import com.ecwid.consul.v1.Response;
import com.ecwid.consul.v1.agent.model.NewService;
import com.ecwid.consul.v1.health.HealthServicesRequest;
import com.ecwid.consul.v1.health.model.HealthService;
import io.homeey.matrix.rpc.core.URL;
import io.homeey.matrix.rpc.registry.api.AbstractRegistry;
import io.homeey.matrix.rpc.registry.api.NotifyListener;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Consul 注册中心实现
 * 
 * <h3>核心特性：</h3>
 * <ul>
 *   <li>健康检查：TCP/HTTP/TTL</li>
 *   <li>服务推送：Long Polling（Blocking Query）</li>
 *   <li>ACL 支持：Token 认证</li>
 * </ul>
 * 
 * <h3>URL 配置示例：</h3>
 * <pre>
 * consul://127.0.0.1:8500/io.homeey.example.EchoService
 *   ?healthCheck=tcp
 *   &healthCheckInterval=10s
 *   &healthCheckTimeout=3s
 *   &token=your-acl-token
 * </pre>
 * 
 * @author Matrix RPC Team
 * @since 1.0.0
 */
public class ConsulRegistry extends AbstractRegistry {
    
    private final ConsulClient consulClient;
    private final String aclToken;
    
    public ConsulRegistry(URL registryUrl) {
        super(registryUrl);
        
        String host = registryUrl.getHost();
        int port = registryUrl.getPort() > 0 ? registryUrl.getPort() : 8500;
        this.consulClient = new ConsulClient(host, port);
        this.aclToken = registryUrl.getParameter("token");
        
        logger.info("Consul registry initialized: {}:{}", host, port);
    }
    
    @Override
    protected void doRegister(URL url) {
        NewService service = new NewService();
        service.setId(buildServiceId(url));
        service.setName(url.getPath());  // path 存储服务接口名
        service.setAddress(url.getHost());
        service.setPort(url.getPort());
        service.setTags(buildTags(url));
        
        // 配置健康检查
        NewService.Check check = buildHealthCheck(url);
        service.setCheck(check);
        
        // 注册服务
        consulClient.agentServiceRegister(service, aclToken);
        
        logger.info("Service registered to Consul: {}", service.getId());
    }
    
    @Override
    protected void doUnregister(URL url) {
        String serviceId = buildServiceId(url);
        consulClient.agentServiceDeregister(serviceId, aclToken);
        
        logger.info("Service unregistered from Consul: {}", serviceId);
    }
    
    @Override
    protected List<URL> doLookup(String serviceInterface, String group, String version) {
        String serviceName = buildServiceName(serviceInterface, group, version);
        
        // 查询健康的服务实例
        HealthServicesRequest request = HealthServicesRequest.newBuilder()
            .setPassing(true)  // 仅返回健康实例
            .setToken(aclToken)
            .build();
        
        Response<List<HealthService>> response = consulClient.getHealthServices(
            serviceName, 
            request
        );
        
        return response.getValue().stream()
            .map(this::toMatrixUrl)
            .collect(Collectors.toList());
    }
    
    @Override
    protected void doSubscribe(String serviceInterface, NotifyListener listener) {
        String serviceName = buildServiceName(serviceInterface, null, null);
        
        // 启动 Blocking Query 长轮询
        heartbeatExecutor.scheduleWithFixedDelay(() -> {
            try {
                watchServiceChanges(serviceName, listener);
            } catch (Exception e) {
                logger.error("Failed to watch Consul service: {}", serviceName, e);
            }
        }, 0, 10, TimeUnit.SECONDS);
    }
    
    /**
     * 监听服务变化（Blocking Query）
     */
    private void watchServiceChanges(String serviceName, NotifyListener listener) {
        QueryParams queryParams = QueryParams.DEFAULT;
        
        Response<List<HealthService>> response = consulClient.getHealthServices(
            serviceName,
            HealthServicesRequest.newBuilder()
                .setQueryParams(queryParams)
                .setPassing(true)
                .setToken(aclToken)
                .build()
        );
        
        List<URL> urls = response.getValue().stream()
            .map(this::toMatrixUrl)
            .collect(Collectors.toList());
        
        // 通知监听器
        notifyListeners(serviceName, urls);
    }
    
    /**
     * 构建服务 ID
     */
    private String buildServiceId(URL url) {
        return String.format("%s-%s-%d", 
            url.getPath(),  // path 存储服务接口名
            url.getHost(), 
            url.getPort()
        );
    }
    
    /**
     * 构建服务标签
     */
    private List<String> buildTags(URL url) {
        List<String> tags = new ArrayList<>();
        tags.add("protocol=" + url.getProtocol());
        tags.add("group=" + url.getParameter("group", "default"));
        tags.add("version=" + url.getParameter("version", "1.0.0"));
        return tags;
    }
    
    /**
     * 构建健康检查配置
     */
    private NewService.Check buildHealthCheck(URL url) {
        NewService.Check check = new NewService.Check();
        
        String checkType = url.getParameter("healthCheck", "tcp");
        String interval = url.getParameter("healthCheckInterval", "10s");
        String timeout = url.getParameter("healthCheckTimeout", "3s");
        
        if ("tcp".equalsIgnoreCase(checkType)) {
            check.setTcp(url.getHost() + ":" + url.getPort());
        } else if ("http".equalsIgnoreCase(checkType)) {
            check.setHttp("http://" + url.getHost() + ":" + url.getPort() + "/health");
        } else {
            check.setTtl("30s");  // TTL 方式
        }
        
        check.setInterval(interval);
        check.setTimeout(timeout);
        check.setDeregisterCriticalServiceAfter("1m");
        
        return check;
    }
    
    /**
     * Consul HealthService -> Matrix URL
     */
    private URL toMatrixUrl(HealthService healthService) {
        HealthService.Service service = healthService.getService();
        
        return new URL(
            "matrix",
            service.getAddress(),
            service.getPort(),
            service.getService(),  // 服务名作为 path
            null  // 参数暂时为空
        );
    }
}
