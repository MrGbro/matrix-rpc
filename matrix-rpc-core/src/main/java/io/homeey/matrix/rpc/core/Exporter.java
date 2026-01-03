package io.homeey.matrix.rpc.core;

/**
 *
 * @author jt4mrg@gmail.com
 * @since 2026-01-03
 **/
public interface Exporter<T> {

    /**
     * 获取与该Exporter关联的Invoker对象
     *
     * @return Invoker<T> 与Exporter关联的Invoker
     */
    Invoker<T> getInvoker();

    /**
     * 取消导出服务，释放相关资源
     */
    void unexport();
}