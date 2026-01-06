package io.homeey.matrix.rpc.core;


import java.util.Collections;
import java.util.Map;
import java.util.Objects;

public class SimpleInvocation implements Invocation {
    private final String serviceName;
    private final String methodName;
    private final Class<?>[] parameterTypes;
    private final Object[] arguments;
    private final Map<String, String> attachments;
    
    public SimpleInvocation(String serviceName, String methodName,
                            Class<?>[] parameterTypes, Object[] arguments) {
        this(serviceName, methodName, parameterTypes, arguments, Collections.emptyMap());
    }
    
    public SimpleInvocation(String serviceName, String methodName,
                            Class<?>[] parameterTypes, Object[] arguments,
                            Map<String, String> attachments) {
        this.serviceName = Objects.requireNonNull(serviceName, "Service name cannot be null");
        this.methodName = Objects.requireNonNull(methodName, "Method name cannot be null");
        this.parameterTypes = Objects.requireNonNull(parameterTypes, "Parameter types cannot be null");
        this.arguments = Objects.requireNonNull(arguments, "Arguments cannot be null");
        this.attachments = attachments != null ? Collections.unmodifiableMap(attachments) : Collections.emptyMap();
        
        if (parameterTypes.length != arguments.length) {
            throw new IllegalArgumentException("Parameter types length must match arguments length");
        }
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
        return parameterTypes.clone();
    }
    
    @Override
    public Object[] arguments() {
        return arguments.clone();
    }
    
    @Override
    public Map<String, String> getAttachments() {
        return attachments;
    }
    
    @Override
    public String toString() {
        return "SimpleInvocation{" +
                "service='" + serviceName + '\'' +
                ", method='" + methodName + '\'' +
                ", parameters=" + java.util.Arrays.toString(parameterTypes) +
                ", arguments=" + java.util.Arrays.toString(arguments) +
                '}';
    }
}