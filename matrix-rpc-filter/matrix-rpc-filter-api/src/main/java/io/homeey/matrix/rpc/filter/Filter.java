package io.homeey.matrix.rpc.filter;

import io.homeey.matrix.rpc.core.Result;
import io.homeey.matrix.rpc.core.Invocation;
import io.homeey.matrix.rpc.core.Invoker;
import io.homeey.matrix.rpc.spi.SPI;

/**
 *
 * @author jt4mrg@gmail.com
 * @since 2026-01-01
 **/
@SPI
public interface Filter {
    /**
     * 过滤器
     *
     * @param invoker    调用者
     * @param invocation 调用参数
     * @return 调用结果
     */
    Result invoke(Invoker<?> invoker, Invocation invocation);
}
