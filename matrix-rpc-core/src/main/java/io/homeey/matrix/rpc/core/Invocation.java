package io.homeey.matrix.rpc.core;

import java.util.Map;

public interface Invocation {

    /**
     * 获取服务名称
     *
     * @return 服务名称
     */
    String getServiceName();

    /**
     * 获取方法名称
     *
     * @return 方法名称
     */
    String methodName();

    /**
     * 获取参数类型数组
     *
     * @return 参数类型数组
     */
    Class<?>[] parameterTypes();

    /**
     * 获取参数值数组
     *
     * @return 参数值数组
     */
    Object[] arguments();

    /**
     * 获取附件信息
     *
     * @return 附件信息映射
     */
    Map<String, String> getAttachments();
}