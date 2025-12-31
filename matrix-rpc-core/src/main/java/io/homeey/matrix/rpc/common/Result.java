package io.homeey.matrix.rpc.common;

import java.io.Serializable;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * 调用结果封装
 */
public interface Result extends Serializable {
    
    Object getValue();
    
    Throwable getException();
    
    boolean hasException();
    
    Map<String, Object> getAttachments();
    
    default CompletableFuture<Result> toFuture() {
        return CompletableFuture.completedFuture(this);
    }
}
