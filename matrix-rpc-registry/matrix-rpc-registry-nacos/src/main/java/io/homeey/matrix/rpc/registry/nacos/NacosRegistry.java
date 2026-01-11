package io.homeey.matrix.rpc.registry.nacos;

import com.alibaba.nacos.api.exception.NacosException;
import com.alibaba.nacos.api.naming.NamingFactory;
import com.alibaba.nacos.api.naming.NamingService;
import com.alibaba.nacos.api.naming.pojo.Instance;
import io.homeey.matrix.rpc.core.URL;
import io.homeey.matrix.rpc.registry.api.NotifyListener;
import io.homeey.matrix.rpc.registry.api.Registry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Nacos注册中心实现
 *
 * @author jt4mrg@gmail.com
 * @since 2026/01/03
 */
public class NacosRegistry implements Registry {
    private static final Logger logger = LoggerFactory.getLogger(NacosRegistry.class);
    private final NamingService namingService;
    private final URL registryUrl;
    private final Map<String, List<URL>> serviceCache = new ConcurrentHashMap<>();
    private final Map<String, NotifyListener> listeners = new ConcurrentHashMap<>();

    public NacosRegistry(URL url) {
        this.registryUrl = url;
        try {
            this.namingService = NamingFactory.createNamingService(url.getHost() + ":" + url.getPort());
            // 启动监听线程
            new Thread(this::listenServiceChanges).start();
        } catch (NacosException e) {
            throw new RuntimeException("Failed to create Nacos naming service", e);
        }
    }

    @Override
    public void register(URL url) {
        try {
            Instance instance = createNacosInstance(url);
            namingService.registerInstance(
                    getServiceName(url),
                    instance
            );
            logger.info("Service registered: {} -> {}", getServiceName(url), url);
        } catch (NacosException e) {
            logger.error("Failed to register service to Nacos: {}", url, e);
            throw new RuntimeException("Failed to register service to Nacos", e);
        }
    }

    @Override
    public void unregister(URL url) {
        try {
            Instance instance = createNacosInstance(url);
            namingService.deregisterInstance(
                    getServiceName(url),
                    instance
            );
            logger.info("Service unregistered: {}", getServiceName(url));
        } catch (NacosException e) {
            logger.error("Failed to unregister service from Nacos: {}", url, e);
            throw new RuntimeException("Failed to unregister service from Nacos", e);
        }
    }

    @Override
    public List<URL> lookup(String serviceInterface, String group, String version) {
        String serviceName = buildServiceName(serviceInterface, group, version);
        try {
            List<Instance> instances = namingService.getAllInstances(serviceName);
            return convertToUrls(instances, serviceInterface);
        } catch (NacosException e) {
            throw new RuntimeException("Failed to lookup service from Nacos", e);
        }
    }

    @Override
    public void subscribe(String serviceInterface, NotifyListener listener) {
        String serviceName = buildServiceName(serviceInterface, null, null);
        listeners.put(serviceName, listener);

        // 立即获取当前服务列表
        List<URL> urls = lookup(serviceInterface, null, null);
        if (!urls.isEmpty()) {
            listener.notify(urls);
        }
    }

    private void listenServiceChanges() {
        while (true) {
            try {
                for (String serviceName : listeners.keySet()) {
                    List<Instance> instances = namingService.getAllInstances(serviceName);
                    List<URL> urls = convertToUrls(instances, extractInterface(serviceName));
                    listeners.get(serviceName).notify(urls);
                }
                Thread.sleep(5000); // 5秒轮询
            } catch (Exception e) {
                logger.error("Service change listener error", e);
            }
        }
    }

    private Instance createNacosInstance(URL url) {
        Instance instance = new Instance();
        instance.setIp(url.getHost());
        instance.setPort(url.getPort());
        instance.setWeight(1.0);
        instance.setEnabled(true);

        // 设置元数据
        instance.setMetadata(Map.of(
                "interface", url.getPath(),
                "group", url.getParameter("group", ""),
                "version", url.getParameter("version", "1.0.0"),
                "protocol", url.getProtocol()
        ));
        return instance;
    }

    private List<URL> convertToUrls(List<Instance> instances, String serviceInterface) {
        return instances.stream()
                .filter(Instance::isEnabled)
                .map(instance -> URL.valueOf(
                        instance.getMetadata().get("protocol") + "://" +
                                instance.getIp() + ":" + instance.getPort() + "/" +
                                serviceInterface + "?" +
                                "group=" + instance.getMetadata().get("group") + "&" +
                                "version=" + instance.getMetadata().get("version")
                ))
                .collect(Collectors.toList());
    }

    private String getServiceName(URL url) {
        return buildServiceName(
                url.getPath(),
                url.getParameter("group"),
                url.getParameter("version")
        );
    }

    private String buildServiceName(String serviceInterface, String group, String version) {
        StringBuilder sb = new StringBuilder("matrix-rpc-");
        sb.append(serviceInterface.replace('.', '-'));
        if (group != null && !group.isEmpty()) {
            sb.append(":").append(group);
        }
        if (version != null && !version.isEmpty()) {
            sb.append(":").append(version);
        }
        return sb.toString();
    }

    private String extractInterface(String serviceName) {
        String base = serviceName.replace("matrix-rpc-", "");
        return base.split(":")[0].replace('-', '.');
    }
}