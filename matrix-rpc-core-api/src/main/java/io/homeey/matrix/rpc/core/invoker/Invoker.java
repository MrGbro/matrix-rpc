package io.homeey.matrix.rpc.core.invoker;

import io.homeey.matrix.rpc.core.invocation.Invocation;
import io.homeey.matrix.rpc.core.invocation.Result;

public interface Invoker<T> {

    /**
     * 获取Invoker关联的服务接口类型
     * 
     * @return 服务接口的Class对象
     */
    Class<T> getInterface();

    /**
     * 执行远程调用
     * 
     * @param invocation 调用参数，包含方法名、参数类型、参数值等信息
     * @return 调用结果，封装了返回值或异常信息
     */
    Result invoke(Invocation invocation);
}
