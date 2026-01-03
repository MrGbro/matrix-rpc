package io.homeey.matrix.rpc.core.invocation;

import io.homeey.matrix.rpc.core.Invocation;

import java.util.HashMap;
import java.util.Map;

public class DefaultInvocation implements Invocation {

    private final String serviceName;
    private final String methodName;
    private final Class<?>[] parameterTypes;
    private final Object[] arguments;
    private final Map<String, String> attachments = new HashMap<>();

    public DefaultInvocation(String serviceName,
                             String methodName,
                             Class<?>[] parameterTypes,
                             Object[] arguments) {
        this.serviceName = serviceName;
        this.methodName = methodName;
        this.parameterTypes = parameterTypes;
        this.arguments = arguments;
    }

    @Override
    public String getServiceName() {
        return serviceName;
    }

    @Override
    public String methodName() {
        return methodName;
    }

    @Override
    public Class<?>[] parameterTypes() {
        return parameterTypes;
    }

    @Override
    public Object[] arguments() {
        return arguments;
    }

    @Override
    public Map<String, String> getAttachments() {
        return attachments;
    }
}
