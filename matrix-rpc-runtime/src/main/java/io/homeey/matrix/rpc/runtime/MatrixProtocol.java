package io.homeey.matrix.rpc.runtime;

import io.homeey.matrix.rpc.core.*;
import io.homeey.matrix.rpc.cluster.api.Cluster;
import io.homeey.matrix.rpc.cluster.api.Directory;
import io.homeey.matrix.rpc.cluster.api.LoadBalance;
import io.homeey.matrix.rpc.cluster.api.RegistryDirectory;
import io.homeey.matrix.rpc.registry.api.Registry;
import io.homeey.matrix.rpc.registry.api.RegistryFactory;
import io.homeey.matrix.rpc.runtime.support.FilterChainBuilder;
import io.homeey.matrix.rpc.spi.Activate;
import io.homeey.matrix.rpc.spi.ExtensionLoader;
import io.homeey.matrix.rpc.spi.SPI;
import io.homeey.matrix.rpc.transport.api.TransportClient;
import io.homeey.matrix.rpc.transport.api.TransportServer;
import io.homeey.matrix.rpc.transport.netty.client.NettyTransportClient;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;

@SPI("matrix")
@Activate(order = 100)
public class MatrixProtocol implements Protocol {
    private final ConcurrentHashMap<String, Exporter<?>> exporters = new ConcurrentHashMap<>();
    private final TransportServer transportServer;
    private final AtomicBoolean initialized = new AtomicBoolean(false);
    private final Registry registry;
    private final ConcurrentMap<String, TransportClient> clients = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, List<URL>> serviceUrls = new ConcurrentHashMap<>();


    public MatrixProtocol() {
        // 1. 通过SPI加载Transport
        this.transportServer = ExtensionLoader.getExtensionLoader(TransportServer.class)
                .getExtension("netty");

        // 2. 默认使用memory注册中心，可通过系统属性覆盖
        String registryAddress = System.getProperty("matrix.registry.address", "memory://localhost");
        URL registryUrl = URL.valueOf(registryAddress);
        RegistryFactory registryFactory = ExtensionLoader.getExtensionLoader(RegistryFactory.class)
                .getExtension(registryUrl.getProtocol());
        this.registry = registryFactory.getRegistry(registryUrl);

        System.out.println("[Matrix RPC] Using registry: " + registryAddress);
    }

    @Override
    public synchronized <T> Exporter<T> export(Invoker<T> invoker, URL url) {
        // 1. 初始化服务器（仅第一次调用时启动）
        if (initialized.compareAndSet(false, true)) {
            // 2. 启动传输层，设置请求处理器
            transportServer.start(url, this::handleRequest);
        }

        // 3. 为 Invoker 包装 Provider 端 Filter 链
        Invoker<T> filteredInvoker = FilterChainBuilder.buildInvokerChain(invoker, "PROVIDER");

        // 4. 注册服务
        String key = serviceKey(url, invoker.getInterface());
        exporters.put(key, new AbstractExporter<>(filteredInvoker));

        System.out.println("[Matrix RPC] Service exported: " + key);
        // 5. 注册到注册中心
        registry.register(url);
        System.out.println("[Matrix RPC] Service registered to registry: " + url);
        return new AbstractExporter<T>(filteredInvoker) {
            @Override
            public void unexport() {
                exporters.remove(key);
                if (exporters.isEmpty()) {
                    try {
                        transportServer.close();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    initialized.set(false);
                }
            }
        };
    }

    @Override
    public <T> Invoker<T> refer(Class<T> type, URL url) {
        // 检测是否为直连模式（URL 参数中包含 direct=true）
        boolean isDirect = "true".equals(url.getParameter("direct"));
        Invoker<T> invoker = null;
        if (isDirect) {
            // 直连模式：绕过注册中心，直接创建 Invoker
            System.out.println("[Matrix RPC] Direct mode enabled, connecting to: " + url.getAddress());
            // 创建 直连的 Invoker
            invoker = createDirectInvoker(type, url);
        } else {
            // 注册中心模式：原有逻辑
            // 1. 订阅服务变化
            String serviceKey = serviceKey(url, type);
            registry.subscribe(type.getName(), urls -> {
                serviceUrls.put(serviceKey, urls);
                System.out.println("[Matrix RPC] Service updated: " + serviceKey + ", providers: " + urls.size());
            });

            // 2. 首次获取服务列表
            List<URL> urls = registry.lookup(type.getName(), url.getParameter("group"), url.getParameter("version"));
            serviceUrls.put(serviceKey, urls);

            // 3. 创建 Directory
            Directory<T> directory = new RegistryDirectory<>(type, () -> serviceUrls.get(serviceKey));

            // 4. 加载负载均衡策略（通过 SPI）
            String loadbalanceName = url.getParameter("loadbalance", "random");
            LoadBalance loadBalance = ExtensionLoader.getExtensionLoader(LoadBalance.class)
                    .getExtension(loadbalanceName);
            System.out.println("[Matrix RPC] Using loadbalance: " + loadbalanceName);

            // 5. 加载 Cluster（通过 SPI）
            String clusterName = url.getParameter("cluster", "failover");
            Cluster cluster = ExtensionLoader.getExtensionLoader(Cluster.class)
                    .getExtension(clusterName);
            System.out.println("[Matrix RPC] Using cluster: " + clusterName);

            // 6. 确保所有提供者都有客户端连接
            ensureClients(serviceKey);
            // 7. 创建 ClusterInvoker
            invoker = cluster.join(directory, loadBalance, clients);
        }

        // 8. 为 Invoker 包装 Consumer 端 Filter 链
        Invoker<T> filteredInvoker = FilterChainBuilder.buildInvokerChain(invoker, "CONSUMER");
        
        // 9. 包装 URL 参数附加器（将 tag 等参数传递到 Invocation attachments）
        return new URLAttachmentInvoker<>(filteredInvoker, url);
    }

    // 处理请求的核心方法
    private Result handleRequest(Invocation invocation) {
        String key = invocation.getServiceName() +
                ":" + invocation.getAttachments().getOrDefault("group", "") +
                ":" + invocation.getAttachments().getOrDefault("version", "1.0.0");
        Exporter<?> exporter = exporters.get(key);

        if (exporter == null) {
            return new Result(new IllegalStateException("Service not found: " + key));
        }

        try {
            return exporter.getInvoker().invoke(invocation);
        } catch (Exception e) {
            return new Result(e);
        }
    }

    /**
     * 确保所有服务提供者都有客户端连接
     */
    private void ensureClients(String serviceKey) {
        List<URL> providers = serviceUrls.get(serviceKey);
        if (providers != null) {
            for (URL provider : providers) {
                clients.computeIfAbsent(provider.getAddress(), k -> createClient(provider));
            }
        }
    }

    /**
     * 创建直连模式的 Invoker（绕过注册中心）
     */
    private <T> Invoker<T> createDirectInvoker(Class<T> type, URL url) {
        // 1. 创建客户端连接
        TransportClient client = createClient(url);

        // 2. 创建简单的 Invoker
        Invoker<T> invoker = new AbstractInvoker<T>(type) {
            @Override
            public Result invoke(Invocation invocation) {
                try {
                    // 从 URL 参数中获取超时时间，默认 3000ms
                    long timeout = url.getParameter("timeout", 3000);
                    return client.send(invocation, timeout);
                } catch (Exception e) {
                    return new Result(new RpcException("Direct invocation failed: " + e.getMessage(), e));
                }
            }
        };

        // 3. 包装 Filter 链
        return FilterChainBuilder.buildInvokerChain(invoker, "CONSUMER");
    }

    private TransportClient createClient(URL url) {
        // 直接创建NettyTransportClient（因为需要URL参数）
        NettyTransportClient client = new NettyTransportClient(url);
        try {
            client.connect();
        } catch (Exception e) {
            throw new RuntimeException("Failed to create client for " + url, e);
        }
        return client;
    }


    private String serviceKey(URL url, Class<?> type) {
        return type.getName() +
                ":" + url.getParameter("group", "") +
                ":" + url.getParameter("version", "1.0.0");
    }


    // 简化版导出器
    private static class AbstractExporter<T> implements Exporter<T> {
        private final Invoker<T> invoker;

        public AbstractExporter(Invoker<T> invoker) {
            this.invoker = invoker;
        }

        @Override
        public Invoker<T> getInvoker() {
            return invoker;
        }

        @Override
        public void unexport() {
            // 子类可重写
        }
    }
}