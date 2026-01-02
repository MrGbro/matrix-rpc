package io.homeey.matrix.rpc.core.protocol;

import io.homeey.matrix.rpc.core.invoker.Invoker;

public interface Exporter<T> {

    /**
     * 获取与该 Exporter 关联的 Invoker
     *
     * @return 与该 Exporter 关联的 Invoker
     */
    Invoker<T> getInvoker();

    /**
     * 取消导出，释放相关资源
     */
    void unexport();
}
