package io.homeey.matrix.rpc.protocol.http2;

import io.homeey.matrix.rpc.core.Invocation;

import java.util.HashMap;
import java.util.Map;

/**
 * HTTP/2 请求封装
 * 
 * <p>封装 HTTP/2 请求的所有信息，包括：
 * <ul>
 *   <li>HTTP Method (GET/POST)</li>
 *   <li>请求路径</li>
 *   <li>Headers（包含 RPC 元数据）</li>
 *   <li>Body（序列化后的参数）</li>
 * </ul>
 * 
 * @author Matrix RPC Team
 * @since 1.0.0
 */
public class Http2Request {
    
    /**
     * HTTP 方法（通常为 POST）
     */
    private String method = "POST";
    
    /**
     * 请求路径（格式：/ServiceName/MethodName）
     */
    private String path;
    
    /**
     * HTTP Headers
     */
    private final Map<String, String> headers = new HashMap<>();
    
    /**
     * 请求体（序列化后的数据）
     */
    private byte[] body;
    
    /**
     * RPC Invocation（用于服务端反序列化）
     */
    private Invocation invocation;
    
    public String getMethod() {
        return method;
    }
    
    public void setMethod(String method) {
        this.method = method;
    }
    
    public String getPath() {
        return path;
    }
    
    public void setPath(String path) {
        this.path = path;
    }
    
    public Map<String, String> getHeaders() {
        return headers;
    }
    
    public String getHeader(String name) {
        return headers.get(name);
    }
    
    public void addHeader(String name, String value) {
        headers.put(name, value);
    }
    
    public byte[] getBody() {
        return body;
    }
    
    public void setBody(byte[] body) {
        this.body = body;
    }
    
    public Invocation getInvocation() {
        return invocation;
    }
    
    public void setInvocation(Invocation invocation) {
        this.invocation = invocation;
    }
    
    @Override
    public String toString() {
        return "Http2Request{" +
                "method='" + method + '\'' +
                ", path='" + path + '\'' +
                ", headers=" + headers.size() +
                ", bodySize=" + (body != null ? body.length : 0) +
                '}';
    }
}
