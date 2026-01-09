# Matrix RPC Framework

[![License](https://img.shields.io/badge/license-Apache--2.0-blue.svg)](https://github.com/homeey-io/matrix-rpc/blob/main/LICENSE)
[![Java Version](https://img.shields.io/badge/java-21+-blue.svg)](https://docs.oracle.com/en/java/javase/21/)
[![Build Status](https://img.shields.io/badge/build-passing-brightgreen.svg)]()

Matrix RPC 是一个轻量级、云原生的分布式RPC框架，专为现代微服务架构设计。它提供了高性能、低延迟的服务间通信能力，并具备灵活的扩展机制和丰富的中间件支持。

## 🌟 特性

- **高性能**: 基于Netty的异步非阻塞IO模型
- **轻量级**: 最小依赖，快速启动
- **模块化设计**: 清晰的模块划分，易于扩展
- **多编解码支持**: 支持Kryo、Protobuf等多种序列化方式
- **服务发现**: 支持Nacos等注册中心
- **负载均衡**: 提供随机、轮询等多种负载均衡策略
- **过滤器链**: 支持自定义过滤器，便于实现监控、认证等功能
- **SPI扩展**: 灵活的插件化扩展机制
- **优雅停机**: 支持服务优雅上下线

## 🏗️ 架构设计

Matrix RPC 采用经典的分层架构设计：

```
┌─────────────────────────────────────────┐
│              Application Layer          │
├─────────────────────────────────────────┤
│              Proxy Layer                │
├─────────────────────────────────────────┤
│              Cluster Layer              │
├─────────────────────────────────────────┤
│              Protocol Layer             │
├─────────────────────────────────────────┤
│              Transport Layer            │
├─────────────────────────────────────────┤
│              Codec Layer                │
├─────────────────────────────────────────┤
│              Registry Layer             │
└─────────────────────────────────────────┘
```

### 核心模块

- **matrix-rpc-common**: 通用工具和数据结构
- **matrix-rpc-core**: 核心抽象接口（Protocol、Invoker、Exporter等）
- **matrix-rpc-spi**: 服务提供接口，支持插件化扩展
- **matrix-rpc-transport**: 传输层抽象（Netty实现）
- **matrix-rpc-codec**: 编解码层（Kryo、Protobuf实现）
- **matrix-rpc-registry**: 注册中心抽象（Nacos、内存实现）
- **matrix-rpc-cluster**: 集群容错（负载均衡、容错机制）
- **matrix-rpc-filter**: 过滤器链机制
- **matrix-rpc-proxy**: 代理工厂（JDK动态代理）
- **matrix-rpc-runtime**: 运行时实现，集成各模块

## 🚀 快速开始

### 服务提供者（Provider）

```java
import io.homeey.matrix.rpc.example.api.EchoService;
import io.homeey.matrix.rpc.runtime.RpcService;

public class ProviderMain {
    public static void main(String[] args) {
        // 一行代码暴露服务！
        RpcService.export(EchoService.class, new EchoServiceImpl(), 20880).await();
    }
}
```

### 服务消费者（Consumer）

```java
import io.homeey.matrix.rpc.example.api.EchoService;
import io.homeey.matrix.rpc.runtime.RpcReference;

public class ConsumerMain {
    public static void main(String[] args) {
        // 一行代码获取远程服务代理！
        EchoService echoService = RpcReference.refer(EchoService.class, "localhost", 20880);
        
        // 调用远程方法
        String result = echoService.echo("Hello Matrix RPC!");
        System.out.println("Result: " + result);
    }
}
```

### 使用Builder模式进行更详细的配置

```java
// Provider配置
RpcService.create(EchoService.class, new EchoServiceImpl())
    .port(20880)
    .version("2.0.0")
    .group("test")
    .export()
    .await();

// Consumer配置
EchoService service = RpcReference.create(EchoService.class)
    .address("localhost", 20880)
    .timeout(5000)
    .get();
```

## ⚙️ 配置选项

### 系统属性配置

- `matrix.registry.address`: 注册中心地址，默认为 `memory://localhost`
- `matrix.filter.accesslog.enabled`: 访问日志过滤器开关
- `matrix.filter.exception.enabled`: 异常处理过滤器开关

### 服务级配置

- `group`: 服务分组
- `version`: 服务版本
- `timeout`: 调用超时时间

## 🔧 扩展机制

Matrix RPC 提供了丰富的扩展点：

### 协议扩展
```java
@SPI("matrix")
public interface Protocol {
    <T> Exporter<T> export(Invoker<T> invoker, URL url);
    <T> Invoker<T> refer(Class<T> type, URL url);
}
```

### 编解码扩展
```java
@SPI("kryo")
public interface Codec {
    byte[] encode(Object obj);
    <T> T decode(byte[] data, Class<T> clazz);
}
```

### 传输层扩展
```java
@SPI("netty")
public interface TransportServer {
    void start(URL url, RequestHandler handler);
    void close();
}
```

### 过滤器扩展
```java
@Activate(group = {"PROVIDER"}, order = 100)
public class CustomFilter implements Filter {
    @Override
    public Result invoke(Invoker<?> invoker, Invocation invocation) {
        // 自定义逻辑
        return invoker.invoke(invocation);
    }
}
```

## 🛠️ 编译构建

```bash
# 克隆项目
git clone https://github.com/homeey-io/matrix-rpc.git
cd matrix-rpc

# Maven编译
mvn clean install

# 运行示例
mvn exec:java -Dexec.mainClass="io.homeey.matrix.rpc.example.provider.ProviderMain"
mvn exec:java -Dexec.mainClass="io.homeey.matrix.rpc.example.consumer.ConsumerMain"
```

## 📋 技术栈

- **Java 21+**: 使用最新的Java特性
- **Netty 4.2+**: 高性能网络通信框架
- **SLF4J/Logback**: 日志框架
- **Protobuf**: 序列化协议
- **Kryo**: 高性能序列化库
- **Nacos**: 服务注册与发现

## 🤝 贡献指南

我们欢迎任何形式的贡献：

1. Fork 项目
2. 创建特性分支
3. 提交你的代码
4. 发起 Pull Request

## 📄 许可证

Matrix RPC 遵循 Apache License 2.0 许可证。

## 📞 社区支持

如果您在使用过程中遇到任何问题，请：

- 查看 [Issues](https://github.com/homeey-io/matrix-rpc/issues)
- 提交新的 Issue
- 参与社区讨论

---

**Matrix RPC** - 让分布式服务通信更简单、更高效！