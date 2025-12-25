package io.homeey.nexa.core.protocol;

import io.homeey.nexa.core.rpc.Invoker;

/**
 *
 * @author jt4mrg@gmail.com
 * @since 2025-12-25 23:15
 **/
public interface Exporter <T>{
    /**
     * 获取与该Exporter关联的Invoker对象
     * 
     * @return Invoker<T> 与Exporter关联的Invoker实例
     */
    Invoker<T> getInvoker();
    
    /**
     * 取消导出服务，释放相关资源
     */
    void unExport();
}
