package io.homeey.nexa.core.config;

import io.homeey.nexa.common.URL;
import io.homeey.nexa.common.constants.Constants;
import lombok.Getter;
import lombok.Setter;

import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author jt4mrg@gmail.com
 * @since 2025-12-25 23:42
 **/
@Getter
@Setter
public class ReferenceConfig<T> {
    private Class<T> interfaceClass;
    private String url;

    private String version;
    private String group;
    private final Integer timeout = Constants.DEFAULT_TIMEOUT;


    private Map<String, String> parameters;

    public ReferenceConfig() {
    }

    public ReferenceConfig(Class<T> interfaceClass) {
        this.interfaceClass = interfaceClass;
    }


    public URL toUrl() {
        if (url != null && !url.isEmpty()) {
            return URL.valueOf(url);
        }
        Map<String, String> parameters = new HashMap<>();
        parameters.put(Constants.INTERFACE_KEY, interfaceClass.getName());
        parameters.put(Constants.SIDE_KEY, Constants.SIDE_CONSUMER);

        if (version != null && !version.isEmpty()) {
            parameters.put(Constants.VERSION_KEY, version);
        }

        if (group != null && !group.isEmpty()) {
            parameters.put(Constants.GROUP_KEY, group);
        }

        if (timeout != null) {
            parameters.put(Constants.TIMEOUT_KEY, String.valueOf(timeout));
        }

        if (parameters != null) {
            parameters.putAll(this.parameters);
        }
        //todo 从注册中心获取服务器地址 （Milestone 3实现），当前使用直连
        throw new UnsupportedOperationException("Milestone 3");
    }
}

