package io.homeey.nexa.core.rpc;

import lombok.Data;

import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author jt4mrg@gmail.com
 * @since 2025-12-24 22:55
 **/
@Data
public class RpcContext {
    private static final ThreadLocal<RpcContext> LOCAL = ThreadLocal.withInitial(RpcContext::new);

    private final Map<String, Object> attachments = new HashMap<>();

    private String remoteHost;
    private String remotePort;
    private String localHost;
    private String localPort;

    public static RpcContext getContext() {
        return LOCAL.get();
    }

    public static void removeContext() {
        LOCAL.remove();
    }

    public RpcContext setAttachment(String key, Object value) {
        attachments.put(key, value);
        return this;
    }

    public Object getAttachment(String key) {
        return attachments.get(key);
    }

    public String getRemoteAddress() {
        return remoteHost + ":" + remotePort;
    }

    public String getLocalAddress() {
        return localHost + ":" + localPort;
    }
}
