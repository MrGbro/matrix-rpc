package io.homeey.nexa.core.rpc;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 *
 * @author jt4mrg@gmail.com
 * @since 2025-12-24 22:37
 **/
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Invocation implements Serializable {
    private static final long serialVersionUID = -1752329367986205091L;

    private String serviceName;

    private String methodName;

    private Class<?>[] parameterTypes;

    private Object[] arguments;
}
