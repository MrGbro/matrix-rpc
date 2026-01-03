package io.homeey.matrix.rpc.core.remoting;

import java.io.Serial;
import java.io.Serializable;

/**
 *
 * @author jt4mrg@gmail.com
 * @since 2026-01-02
 **/
public class Request implements Serializable {

    @Serial
    private static final long serialVersionUID = 6145036490483629723L;
    /**
     * 请求ID
     */
    private long requestId;
    /**
     * 服务名称
     */
    private String serviceName;
    /**
     * 方法名称
     */
    private String methodName;

    /**
     * 参数类型数组
     */
    private Class<?>[] parameterTypes;
    /**
     * 参数值数组
     */
    private Object[] arguments;

    public long getRequestId() {
        return requestId;
    }

    public void setRequestId(long requestId) {
        this.requestId = requestId;
    }

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public String getMethodName() {
        return methodName;
    }

    public void setMethodName(String methodName) {
        this.methodName = methodName;
    }

    public Class<?>[] getParameterTypes() {
        return parameterTypes;
    }

    public void setParameterTypes(Class<?>[] parameterTypes) {
        this.parameterTypes = parameterTypes;
    }

    public Object[] getArguments() {
        return arguments;
    }

    public void setArguments(Object[] arguments) {
        this.arguments = arguments;
    }
}
