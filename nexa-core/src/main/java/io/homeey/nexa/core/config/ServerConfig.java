package io.homeey.nexa.core.config;

import io.homeey.nexa.common.URL;
import io.homeey.nexa.common.constants.Constants;
import io.homeey.nexa.common.utils.NetUtils;
import lombok.Getter;
import lombok.Setter;

import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author jt4mrg@gmail.com
 * @since 2025-12-25 23:28
 **/
@Getter
@Setter
public class ServerConfig<T> {
    private final Class<T> interfaceClass;

    private final T ref;

    private final String protocol = Constants.DEFAULT_PROTOCOL;
    private String host;
    private final Integer port = Constants.DEFAULT_PORT;

    private String version;
    private String group;
    private final Integer timeout = Constants.DEFAULT_TIMEOUT;

    private Map<String, String> parameters;

    public ServerConfig(Class<T> interfaceClass, T ref) {
        this.interfaceClass = interfaceClass;
        this.ref = ref;
    }

    public URL toUrl() {
        Map<String, String> params = new HashMap<>();

        //基本参数
        params.put(Constants.INTERFACE_KEY, interfaceClass.getName());
        params.put(Constants.SIDE_KEY, Constants.SIDE_PROVIDER);

        //可选参数
        if (version != null) {
            params.put(Constants.VERSION_KEY, version);
        }

        if (group != null) {
            params.put(Constants.GROUP_KEY, group);
        }

        if (timeout != null) {
            params.put(Constants.TIMEOUT_KEY, String.valueOf(timeout));
        }

        if (parameters != null) {
            params.putAll(parameters);
        }

        String serviceHost = host != null ? host : NetUtils.getLocalIp();
        int servicePort = port != null ? port : Constants.DEFAULT_PORT;
        return new URL(protocol, serviceHost, servicePort, interfaceClass.getName(), params);
    }


}
