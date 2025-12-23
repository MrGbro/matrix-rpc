package io.homeey.nexa.common;

import lombok.Getter;
import lombok.ToString;

import java.io.Serializable;
import java.util.Collections;
import java.util.Map;

/**
 *
 * @author jt4mrg@gmail.com
 * @version 0.0.1
 * @since 2025-12-23 23:37
 **/
@Getter
@ToString
public class URL implements Serializable {
    private final String protocol;
    private final String host;
    private final int port;
    private final String path;
    private final Map<String, String> parameters;


    public URL(String protocol, String host, int port, String path, Map<String, String> parameters) {
        this.protocol = protocol;
        this.host = host;
        this.port = port;
        this.path = path;
        this.parameters = parameters;
    }

    public URL(String path, int port, String host, String protocol) {
        this(protocol, host, port, path, Collections.emptyMap());
    }

    public static URL valueOf(String url) {
        if (url == null || url.isEmpty()) {
            throw new IllegalArgumentException("url can not be null");
        }
        //todo 待实现
        return null;
    }
}
