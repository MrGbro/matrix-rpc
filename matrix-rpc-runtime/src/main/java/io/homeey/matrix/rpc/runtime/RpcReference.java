package io.homeey.matrix.rpc.runtime;

import io.homeey.matrix.rpc.common.Result;
import io.homeey.matrix.rpc.common.URL;
import io.homeey.matrix.rpc.core.Invocation;
import io.homeey.matrix.rpc.core.Invoker;
import io.homeey.matrix.rpc.proxy.api.ProxyFactory;
import io.homeey.matrix.rpc.spi.ExtensionLoader;
import io.homeey.matrix.rpc.transport.api.TransportClient;

import java.io.Closeable;
import java.util.HashMap;
import java.util.Map;

/**
 * RPC 服务引用的简化入口。
 * <p>
 * 用法示例：
 * <pre>
 * // 一行代码获取远程服务代理
 * EchoService service = RpcReference.refer(EchoService.class, "localhost", 20880);
 * String result = service.echo("Hello");
 * 
 * // 或使用 Builder 模式进行更多配置
 * EchoService service = RpcReference.create(EchoService.class)
 *     .address("localhost", 20880)
 *     .timeout(5000)
 *     .get();
 * </pre>
 * 
 * @param <T> 服务接口类型
 */
public class RpcReference<T> implements Closeable {

    private final Class<T> interfaceClass;
    private String host = "localhost";
    private int port = 20880;
    private int timeout = 3000;
    private String protocol = "matrix";
    private String proxyType = "jdk";
    
    private TransportClient client;
    private T proxy;

    private RpcReference(Class<T> interfaceClass) {
        this.interfaceClass = interfaceClass;
    }

    /**
     * 一行代码创建远程服务代理（使用默认配置）
     */
    public static <T> T refer(Class<T> interfaceClass, String host, int port) {
        return create(interfaceClass)
                .address(host, port)
                .get();
    }

    /**
     * 创建 RpcReference Builder
     */
    public static <T> RpcReference<T> create(Class<T> interfaceClass) {
        return new RpcReference<>(interfaceClass);
    }

    /**
     * 设置服务端地址
     */
    public RpcReference<T> address(String host, int port) {
        this.host = host;
        this.port = port;
        return this;
    }

    /**
     * 设置调用超时时间（毫秒）
     */
    public RpcReference<T> timeout(int timeout) {
        this.timeout = timeout;
        return this;
    }

    /**
     * 设置协议类型（默认 matrix）
     */
    public RpcReference<T> protocol(String protocol) {
        this.protocol = protocol;
        return this;
    }

    /**
     * 设置代理类型（默认 jdk）
     */
    public RpcReference<T> proxyType(String proxyType) {
        this.proxyType = proxyType;
        return this;
    }

    /**
     * 获取远程服务代理对象
     */
    public T get() {
        if (proxy != null) {
            return proxy;
        }

        try {
            // 1. 创建 URL
            Map<String, String> params = new HashMap<>();
            params.put("timeout", String.valueOf(timeout));
            URL url = new URL(protocol, host, port, interfaceClass.getName(), params);

            // 2. 通过 SPI 获取 TransportClient 并连接
            client = ExtensionLoader.getExtensionLoader(TransportClient.class)
                    .getDefaultExtension();
            client.init(url);
            client.connect();

            // 3. 创建 Invoker
            Invoker<T> invoker = createInvoker(url);

            // 4. 通过 SPI 获取 ProxyFactory 创建代理
            ProxyFactory proxyFactory = ExtensionLoader.getExtensionLoader(ProxyFactory.class)
                    .getExtension(proxyType);
            proxy = proxyFactory.getProxy(invoker);

            return proxy;
        } catch (Exception e) {
            throw new RuntimeException("Failed to create RPC reference for " + interfaceClass.getName(), e);
        }
    }

    private Invoker<T> createInvoker(URL url) {
        final TransportClient transportClient = this.client;
        final int invokeTimeout = this.timeout;
        
        return new Invoker<T>() {
            @Override
            public Class<T> getInterface() {
                return interfaceClass;
            }

            @Override
            public Result invoke(Invocation invocation) {
                return transportClient.send(invocation, invokeTimeout);
            }
        };
    }

    /**
     * 关闭连接
     */
    @Override
    public void close() {
        if (client != null) {
            try {
                client.close();
            } catch (Exception e) {
                // ignore
            }
            client = null;
        }
        proxy = null;
    }

    /**
     * 获取当前连接状态
     */
    public boolean isConnected() {
        return client != null && client.isConnected();
    }
}
