package io.homeey.matrix.rpc.runtime;

import io.homeey.matrix.rpc.core.Protocol;
import io.homeey.matrix.rpc.core.URL;
import io.homeey.matrix.rpc.core.Invoker;
import io.homeey.matrix.rpc.proxy.api.ProxyFactory;
import io.homeey.matrix.rpc.spi.ExtensionLoader;

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
    private String loadbalance = "random";  // 负载均衡策略：random/roundrobin/weightedrandom/consistenthash
    private String cluster = "failover";     // 容错策略：failover/failfast/failsafe
    private int retries = 2;                 // 重试次数（仅 failover 有效）
    private boolean directConnect = false;   // 是否启用直连模式（绕过注册中心）
    private String tag = "";                 // 请求标签（用于标签路由）
    private boolean tagForce = false;        // 强制标签路由（不降级）

    private Invoker<T> invoker;  // 改为保存 Invoker，而不是 client
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
        this.directConnect = true;
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
     * 设置负载均衡策略（默认 random）
     *
     * @param loadbalance 支持: random, roundrobin, weightedrandom, consistenthash
     */
    public RpcReference<T> loadbalance(String loadbalance) {
        this.loadbalance = loadbalance;
        return this;
    }

    /**
     * 设置容错策略（默认 failover）
     *
     * @param cluster 支持: failover(失败重试), failfast(快速失败), failsafe(失败安全)
     */
    public RpcReference<T> cluster(String cluster) {
        this.cluster = cluster;
        return this;
    }

    /**
     * 设置重试次数（仅 failover 策略有效，默认 2 次）
     */
    public RpcReference<T> retries(int retries) {
        this.retries = retries;
        return this;
    }

    /**
     * 设置请求标签（用于标签路由）
     * 
     * @param tag 标签名称（如 gray, beta, prod）
     */
    public RpcReference<T> tag(String tag) {
        this.tag = tag;
        return this;
    }

    /**
     * 设置强制标签路由（不降级到无标签实例）
     * 
     * @param force 是否强制
     */
    public RpcReference<T> tagForce(boolean force) {
        this.tagForce = force;
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
            params.put("loadbalance", loadbalance);
            params.put("cluster", cluster);
            params.put("retries", String.valueOf(retries));

            // 添加标签路由参数
            if (tag != null && !tag.isEmpty()) {
                params.put("tag", tag);
                if (tagForce) {
                    params.put("tag.force", "true");
                }
            }

            // 如果启用直连模式，添加 direct 参数
            if (directConnect) {
                params.put("direct", "true");
            }

            URL url = new URL(protocol, host, port, interfaceClass.getName(), params);

            // 2. 通过 SPI 获取 Protocol
            Protocol matrixProtocol = ExtensionLoader.getExtensionLoader(Protocol.class)
                    .getExtension(protocol);

            // 3. 调用 Protocol.refer() 获取完整的 Invoker（含 Cluster + LoadBalance + Filter）
            invoker = matrixProtocol.refer(interfaceClass, url);

            // 4. 通过 SPI 获取 ProxyFactory 创建代理
            ProxyFactory proxyFactory = ExtensionLoader.getExtensionLoader(ProxyFactory.class)
                    .getExtension(proxyType);
            proxy = proxyFactory.getProxy(invoker);

            return proxy;
        } catch (Exception e) {
            throw new RuntimeException("Failed to create RPC reference for " + interfaceClass.getName(), e);
        }
    }

    /**
     * 关闭连接
     */
    @Override
    public void close() {
        invoker = null;
        proxy = null;
    }

    /**
     * 获取当前连接状态
     */
    public boolean isConnected() {
        return invoker != null;
    }
}
