package io.homeey.matrix.rpc.protocol.http2;

import java.util.HashMap;
import java.util.Map;

/**
 * HTTP/2 响应封装
 * 
 * <p>封装 HTTP/2 响应的所有信息，包括：
 * <ul>
 *   <li>HTTP Status Code</li>
 *   <li>Headers（包含异常信息等）</li>
 *   <li>Body（序列化后的结果）</li>
 * </ul>
 * 
 * @author Matrix RPC Team
 * @since 1.0.0
 */
public class Http2Response {
    
    /**
     * HTTP 状态码
     */
    private int status = 200;
    
    /**
     * HTTP Headers
     */
    private final Map<String, String> headers = new HashMap<>();
    
    /**
     * 响应体（序列化后的数据）
     */
    private byte[] body;
    
    /**
     * 结果对象（反序列化后）
     */
    private Object result;
    
    public int getStatus() {
        return status;
    }
    
    public void setStatus(int status) {
        this.status = status;
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
    
    public Object getResult() {
        return result;
    }
    
    public void setResult(Object result) {
        this.result = result;
    }
    
    @Override
    public String toString() {
        return "Http2Response{" +
                "status=" + status +
                ", headers=" + headers.size() +
                ", bodySize=" + (body != null ? body.length : 0) +
                '}';
    }
}
