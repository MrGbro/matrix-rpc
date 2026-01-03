# Matrix RPC

<p align="center">
  <b>轻量级、高性能、云原生的RPC框架</b>
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Java-8+-blue.svg" alt="Java">
  <img src="https://img.shields.io/badge/Spring%20Boot-3.x-green.svg" alt="Spring Boot">
  <img src="https://img.shields.io/badge/Netty-4.1-orange.svg" alt="Netty">
  <img src="https://img.shields.io/badge/License-Apache%202.0-blue.svg" alt="License">
</p>

---
# 简介


# 架构设计
```text
┌────────────────────────────────────────────┐
│           Application / Spring Boot        │
└────────────────────────────────────────────┘
                    │
        ┌───────────▼───────────┐
        │   matrix-rpc-starter   │
        │  自动装配 / Bean 注入  │
        └───────────▲───────────┘
                    │
┌───────────────────┴───────────────────────┐
│              matrix-rpc-core               │
│                                           │
│  ┌──────────────┐    ┌──────────────┐     │
│  │   Invoker    │◀──▶│   Protocol   │     │
│  └──────────────┘    └──────────────┘     │
│        ▲                     ▲            │
│        │                     │            │
│  ┌──────────────┐    ┌──────────────┐     │
│  │   Filter     │    │   Codec      │     │
│  └──────────────┘    └──────────────┘     │
│                                           │
│  ┌──────────────┐    ┌──────────────┐     │
│  │ LoadBalance  │    │   Registry   │     │
│  └──────────────┘    └──────────────┘     │
│                                           │
│        ┌──────────────────────────┐       │
│        │     Extension (SPI)       │       │
│        └──────────────────────────┘       │
└────────────────────────────────────────────┘
                    │
┌───────────────────┴───────────────────────┐
│        matrix-rpc-transport-*              │
│   Netty / HTTP2 / gRPC / Unix Socket       │
└────────────────────────────────────────────┘
                    │
┌───────────────────┴───────────────────────┐
│        matrix-rpc-registry-*               │
│    Nacos / Consul / Etcd / K8s             │
└────────────────────────────────────────────┘

```
# Maven模块设计
```text
matrix-rpc
├── matrix-rpc-bom                # 统一版本管理
├── matrix-rpc-common             # 通用工具 & 基础模型
├── matrix-rpc-spi                # SPI机制（微内核核心）
├── matrix-rpc-core               # RPC核心调度
│
├── matrix-rpc-transport
│   ├── matrix-rpc-transport-api
│   ├── matrix-rpc-transport-netty
│   ├── matrix-rpc-transport-http2
│   └── matrix-rpc-transport-grpc
│
├── matrix-rpc-codec
│   ├── matrix-rpc-codec-api
│   ├── matrix-rpc-codec-protobuf
│   └── matrix-rpc-codec-hessian
│
├── matrix-rpc-registry
│   ├── matrix-rpc-registry-api
│   ├── matrix-rpc-registry-nacos
│   ├── matrix-rpc-registry-etcd
│   └── matrix-rpc-registry-k8s
│
├── matrix-rpc-cluster
│   ├── loadbalance
│   ├── failover
│   └── router
│
├── matrix-rpc-observability
│   ├── tracing
│   ├── metrics
│   └── logging
│
├── matrix-rpc-spring
│   ├── matrix-rpc-spring-context
│   └── matrix-rpc-spring-boot-starter
│
└── matrix-rpc-examples

```
# 核心抽象设计（第一性原理）

这是 matrix-rpc 的“灵魂”。

1️⃣ Invocation & Invoker（调用语义）
```java
public interface Invocation {
    String service();
    String method();
    Class<?>[] parameterTypes();
    Object[] arguments();
    Map<String, String> attachments();
}

public interface Invoker<T> {
    Class<T> getInterface();
    Result invoke(Invocation invocation);
}
```


Invoker 是 RPC 世界的“函数指针”

2️⃣ Protocol（协议编排者）
@SPI("matrix")
public interface Protocol {

    <T> Exporter<T> export(Invoker<T> invoker);

    <T> Invoker<T> refer(Class<T> type, URL url);
}


协议 ≠ 传输

Protocol 负责 Invoker → 网络

3️⃣ Transport（纯通信能力）
public interface TransportServer {
void start();
void stop();
}

public interface TransportClient {
CompletableFuture<Response> send(Request request);
}


Transport 不懂 RPC，只懂 IO

4️⃣ Codec（序列化边界）
@SPI("protobuf")
public interface Codec {

    byte[] encode(Object obj);

    <T> T decode(byte[] data, Class<T> type);
}

5️⃣ Filter（调用链，极其重要）
@SPI
public interface Filter {

    Result invoke(Invoker<?> invoker, Invocation invocation);
}


所有：

超时

限流

熔断

Trace

Metrics
都通过 Filter 实现

6️⃣ Registry（服务治理）
@SPI("nacos")
public interface Registry {

    void register(ServiceInstance instance);

    void unregister(ServiceInstance instance);

    List<ServiceInstance> lookup(String serviceName);
}

四、SPI 机制设计（matrix 的“中枢神经”）
核心目标

不依赖 Java 原生 ServiceLoader

支持：

优先级

条件激活（URL / Profile / 环境）

Wrapper（责任链）

@SPI("netty")
public interface TransportFactory {
TransportServer createServer(URL url);
}

@Activate(group = "provider", order = 100)
public class MetricsFilter implements Filter {}


SPI 加载流程：

ExtensionLoader
├─ loadClass
├─ buildActivateExtensions
├─ sort by order
└─ wrap if needed


这里会是 第二阶段的重点源码实现

五、演进蓝图（非常重要）
🚀 Phase 1：最小可用内核（当前）

目标：

单机 RPC

TCP + Netty

SPI 可扩展

输出：

core / spi / transport-netty

echo demo 跑通

🚀 Phase 2：集群 & 注册中心

Nacos / K8s Registry

LoadBalance

Failover

🚀 Phase 3：Spring Boot Starter

@RpcService

@RpcReference

自动暴露 / 引用

🚀 Phase 4：云原生一等公民

K8s EndpointSlice

gRPC 协议

OpenTelemetry

🚀 Phase 5：性能对标 Dubbo

零拷贝

Pipeline 优化

Async invocation


