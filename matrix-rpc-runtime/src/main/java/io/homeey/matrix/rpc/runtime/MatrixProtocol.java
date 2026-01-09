package io.homeey.matrix.rpc.runtime;

import io.homeey.matrix.rpc.common.Result;
import io.homeey.matrix.rpc.common.RpcException;
import io.homeey.matrix.rpc.common.URL;
import io.homeey.matrix.rpc.core.Exporter;
import io.homeey.matrix.rpc.core.Invocation;
import io.homeey.matrix.rpc.core.Invoker;
import io.homeey.matrix.rpc.core.Protocol;
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
    private URL serverUrl;
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
            this.serverUrl = url;
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
        // 1. 订阅服务变化
        String serviceKey = serviceKey(url, type);
        registry.subscribe(type.getName(), urls -> {
            serviceUrls.put(serviceKey, urls);
            System.out.println("[Matrix RPC] Service updated: " + serviceKey + ", providers: " + urls.size());
        });

        // 2. 首次获取服务列表
        List<URL> urls = registry.lookup(type.getName(), url.getParameter("group"), url.getParameter("version"));
        serviceUrls.put(serviceKey, urls);

        // 3. 创建远程调用 Invoker
        Invoker<T> remoteInvoker = new AbstractInvoker<T>(type) {
            @Override
            public Result invoke(Invocation invocation) throws RpcException {
                // 1. 获取可用服务提供者
                List<URL> providers = serviceUrls.get(serviceKey);
                if (providers == null || providers.isEmpty()) {
                    throw new RpcException("No provider available for service: " + serviceKey);
                }

                // 2. 负载均衡选择 (Phase 2.2 实现)
                URL providerUrl = selectProvider(providers, invocation);

                // 3. 获取/创建客户端
                TransportClient client = clients.computeIfAbsent(
                        providerUrl.getAddress(),
                        k -> createClient(providerUrl)
                );

                // 4. 发送请求 (带超时)
                long timeout = 3000; // 默认3秒
                return client.send(invocation, timeout);
            }
        };

        // 4. 为 Invoker 包装 Consumer 端 Filter 链
        return FilterChainBuilder.buildInvokerChain(remoteInvoker, "CONSUMER");
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

    private URL selectProvider(List<URL> providers, Invocation invocation) {
        // 简化版：随机选择
        return providers.get((int) (Math.random() * providers.size()));
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