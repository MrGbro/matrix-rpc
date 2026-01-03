package io.homeey.matrix.rpc.cluster.api;

import io.homeey.matrix.rpc.common.URL;
import io.homeey.matrix.rpc.core.Invocation;
import io.homeey.matrix.rpc.spi.SPI;

import java.util.List;
@SPI
public interface LoadBalance {
    /**
     * 从提供者列表中选择一个URL用于处理调用
     *
     * @param providers 提供者URL列表
     * @param invocation 调用对象，包含方法名、参数等信息
     * @return 选中的提供者URL
     */
    URL select(List<URL> providers, Invocation invocation);
}