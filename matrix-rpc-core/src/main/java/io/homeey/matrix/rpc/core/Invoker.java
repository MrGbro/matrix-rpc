package io.homeey.matrix.rpc.core;


/**
 * 调用器接口，用于执行远程方法调用
 *
 * @param <T> 服务接口类型
 * @author jt4mrg@gmail.com
 * @since 1.0.0
 */
public interface Invoker<T> {
    /**
     * 获取调用器对应的接口类型
     *
     * @return 服务接口类
     */
    Class<T> getInterface();

    /**
     * 执行方法调用
     *
     * @param invocation 调用信息，包括方法名、参数等
     * @return 调用结果
     */
    Result invoke(Invocation invocation);
}