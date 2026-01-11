package io.homeey.matrix.rpc.runtime;

import io.homeey.matrix.rpc.core.Result;
import io.homeey.matrix.rpc.core.URL;
import io.homeey.matrix.rpc.core.Exporter;
import io.homeey.matrix.rpc.core.Invocation;
import io.homeey.matrix.rpc.core.Invoker;
import io.homeey.matrix.rpc.core.Protocol;
import io.homeey.matrix.rpc.spi.ExtensionLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

/**
 * RPC 服务暴露的简化入口。
 * <p>
 * 用法示例：
 * <pre>
 * // 一行代码暴露服务
 * RpcService.export(EchoService.class, new EchoServiceImpl(), 20880).await();
 *
 * // 或使用 Builder 模式进行更多配置
 * RpcService.create(EchoService.class, new EchoServiceImpl())
 *     .port(20880)
 *     .version("2.0.0")
 *     .export()
 *     .await();
 * </pre>
 *
 * @param <T> 服务接口类型
 */
public class RpcService<T> implements Closeable {
    private static final Logger logger = LoggerFactory.getLogger(RpcService.class);

    private final Class<T> interfaceClass;
    private final T implementation;

    private String host = "0.0.0.0";
    private int port = 20880;
    private String protocol = "matrix";
    private String group = "";
    private String version = "1.0.0";
    private String tag = "";      // 服务标签（用于灰度发布、环境隔离等）
    private int weight = 100;   // 服务权重（用于权重路由）

    private Exporter<T> exporter;
    private volatile boolean exported = false;

    private RpcService(Class<T> interfaceClass, T implementation) {
        if (interfaceClass == null) {
            throw new IllegalArgumentException("Interface class cannot be null");
        }
        if (implementation == null) {
            throw new IllegalArgumentException("Implementation cannot be null");
        }
        if (!interfaceClass.isInterface()) {
            throw new IllegalArgumentException(interfaceClass.getName() + " is not an interface");
        }
        if (!interfaceClass.isAssignableFrom(implementation.getClass())) {
            throw new IllegalArgumentException("Implementation does not implement " + interfaceClass.getName());
        }
        this.interfaceClass = interfaceClass;
        this.implementation = implementation;
    }

    /**
     * 一行代码暴露服务（使用默认配置）
     *
     * @param interfaceClass 服务接口类
     * @param implementation 服务实现实例
     * @param port           监听端口
     * @return RpcService 实例（可调用 await() 阻塞等待）
     */
    public static <T> RpcService<T> export(Class<T> interfaceClass, T implementation, int port) {
        return create(interfaceClass, implementation)
                .port(port)
                .export();
    }

    /**
     * 创建 RpcService Builder
     *
     * @param interfaceClass 服务接口类
     * @param implementation 服务实现实例
     * @return RpcService Builder
     */
    public static <T> RpcService<T> create(Class<T> interfaceClass, T implementation) {
        return new RpcService<>(interfaceClass, implementation);
    }

    /**
     * 设置监听端口
     */
    public RpcService<T> port(int port) {
        this.port = port;
        return this;
    }

    /**
     * 设置绑定主机
     */
    public RpcService<T> host(String host) {
        this.host = host;
        return this;
    }

    /**
     * 设置协议类型（默认 matrix）
     */
    public RpcService<T> protocol(String protocol) {
        this.protocol = protocol;
        return this;
    }

    /**
     * 设置服务分组
     */
    public RpcService<T> group(String group) {
        this.group = group;
        return this;
    }

    /**
     * 设置服务版本
     */
    public RpcService<T> version(String version) {
        this.version = version;
        return this;
    }

    /**
     * 设置服务标签（用于灰度发布、环境隔离）
     * 
     * @param tag 标签名称（如 gray, beta, prod）
     */
    public RpcService<T> tag(String tag) {
        this.tag = tag;
        return this;
    }

    /**
     * 设置服务权重（用于权重路由）
     * 
     * @param weight 权重值（范围 1-1000，默认 100）
     */
    public RpcService<T> weight(int weight) {
        if (weight < 1 || weight > 1000) {
            throw new IllegalArgumentException("Weight must be between 1 and 1000");
        }
        this.weight = weight;
        return this;
    }

    /**
     * 暴露服务
     *
     * @return this（支持链式调用）
     */
    public RpcService<T> export() {
        if (exported) {
            return this;
        }

        try {
            // 1. 创建通用反射 Invoker
            Invoker<T> invoker = createReflectiveInvoker();

            // 2. 构建 URL
            Map<String, String> params = new HashMap<>();
            if (group != null && !group.isEmpty()) {
                params.put("group", group);
            }
            params.put("version", version);
            if (tag != null && !tag.isEmpty()) {
                params.put("tag", tag);
            }
            params.put("weight", String.valueOf(weight));
            URL url = new URL(protocol, host, port, interfaceClass.getName(), params);

            // 3. 通过 SPI 获取 Protocol 并导出
            Protocol protocolInstance = ExtensionLoader.getExtensionLoader(Protocol.class)
                    .getExtension(protocol);
            exporter = protocolInstance.export(invoker, url);

            exported = true;
            return this;
        } catch (Exception e) {
            throw new RuntimeException("Failed to export service " + interfaceClass.getName(), e);
        }
    }

    /**
     * 创建基于反射的通用 Invoker
     */
    private Invoker<T> createReflectiveInvoker() {
        final T target = this.implementation;
        final Class<T> iface = this.interfaceClass;

        return new Invoker<T>() {
            @Override
            public Class<T> getInterface() {
                return iface;
            }

            @Override
            public Result invoke(Invocation invocation) {
                try {
                    // 通过反射调用目标方法
                    Method method = target.getClass().getMethod(
                            invocation.methodName(),
                            invocation.parameterTypes()
                    );
                    Object result = method.invoke(target, invocation.arguments());
                    return new Result(result);
                } catch (Exception e) {
                    Throwable cause = e.getCause() != null ? e.getCause() : e;
                    return new Result(cause);
                }
            }
        };
    }

    /**
     * 阻塞等待服务运行（通常在 main 方法末尾调用）
     */
    public void await() {
        if (!exported) {
            throw new IllegalStateException("Service not exported yet. Call export() first.");
        }

        // 保留控制台输出，给用户直观的反馈
        System.out.println("========================================");
        System.out.println("Service [" + interfaceClass.getSimpleName() + "] exported at port: " + port);
        System.out.println("Press Ctrl+C to stop the server...");
        System.out.println("========================================");

        try {
            Thread.currentThread().join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * 取消服务暴露
     */
    public void unexport() {
        if (exporter != null) {
            exporter.unexport();
            exporter = null;
        }
        exported = false;
    }

    /**
     * 关闭服务（同 unexport）
     */
    @Override
    public void close() {
        unexport();
    }

    /**
     * 服务是否已暴露
     */
    public boolean isExported() {
        return exported;
    }

    /**
     * 获取服务接口类
     */
    public Class<T> getInterface() {
        return interfaceClass;
    }

    /**
     * 获取服务实现实例
     */
    public T getImplementation() {
        return implementation;
    }
}
