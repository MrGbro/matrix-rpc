package io.homeey.matrix.rpc.core;

import java.io.Serial;
import java.io.Serializable;
import java.util.*;

/**
 * 统一资源定位符 - 框架配置总线
 * 承载服务地址、协议、参数等所有配置信息
 * 格式: protocol://host:port/path?key1=value1&key2=value2
 */
public class URL implements Serializable {
    @Serial
    private static final long serialVersionUID = 8248119106126431701L;

    /**
     * 协议，例如：dubbo、http等
     */
    private final String protocol;
    /**
     * 主机地址
     */
    private final String host;
    /**
     * 端口号
     */
    private final int port;
    /**
     * 路径，通常是服务的上下文路径
     */
    private final String path;
    /**
     * 参数集合，包含URL中的查询参数等配置信息
     */
    private final Map<String, String> parameters;

    public URL(String protocol, String host, int port, String path, Map<String, String> parameters) {
        this.protocol = protocol;
        this.host = host;
        this.port = port;
        this.path = path;
        this.parameters = parameters != null ? new HashMap<>(parameters) : new HashMap<>();
    }

    public static URL valueOf(String url) {
        if (url == null || url.isEmpty()) {
            throw new IllegalArgumentException("url is empty");
        }

        String protocol = null, host = null, path = null;
        int port = 0;
        Map<String, String> parameters = new HashMap<>();

        int protocolIndex = url.indexOf("://");
        if (protocolIndex >= 0) {
            protocol = url.substring(0, protocolIndex);
            url = url.substring(protocolIndex + 3);
        }

        int paramIndex = url.indexOf('?');
        if (paramIndex >= 0) {
            String paramStr = url.substring(paramIndex + 1);
            url = url.substring(0, paramIndex);
            for (String param : paramStr.split("&")) {
                String[] kv = param.split("=");
                if (kv.length == 2) {
                    parameters.put(kv[0], kv[1]);
                }
            }
        }

        int pathIndex = url.indexOf('/');
        if (pathIndex >= 0) {
            path = url.substring(pathIndex + 1);
            url = url.substring(0, pathIndex);
        }

        int portIndex = url.indexOf(':');
        if (portIndex >= 0) {
            host = url.substring(0, portIndex);
            port = Integer.parseInt(url.substring(portIndex + 1));
        } else {
            host = url;
        }

        return new URL(protocol, host, port, path, parameters);
    }

    public String getProtocol() {
        return protocol;
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public String getPath() {
        return path;
    }

    public String getAddress() {
        return host + ":" + port;
    }

    public String getParameter(String key) {
        return parameters.get(key);
    }

    public String getParameter(String key, String defaultValue) {
        String value = parameters.get(key);
        return value != null ? value : defaultValue;
    }

    public int getParameter(String key, int defaultValue) {
        String value = parameters.get(key);
        return value != null ? Integer.parseInt(value) : defaultValue;
    }

    public URL addParameter(String key, String value) {
        Map<String, String> newParams = new HashMap<>(parameters);
        newParams.put(key, value);
        return new URL(protocol, host, port, path, newParams);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        if (protocol != null) sb.append(protocol).append("://");
        if (host != null) sb.append(host);
        if (port > 0) sb.append(":").append(port);
        if (path != null) sb.append("/").append(path);
        if (!parameters.isEmpty()) {
            sb.append("?");
            boolean first = true;
            for (Map.Entry<String, String> e : parameters.entrySet()) {
                if (!first) sb.append("&");
                sb.append(e.getKey()).append("=").append(e.getValue());
                first = false;
            }
        }
        return sb.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        URL url = (URL) o;
        return port == url.port &&
                Objects.equals(protocol, url.protocol) &&
                Objects.equals(host, url.host) &&
                Objects.equals(path, url.path);
    }

    @Override
    public int hashCode() {
        return Objects.hash(protocol, host, port, path);
    }
}
