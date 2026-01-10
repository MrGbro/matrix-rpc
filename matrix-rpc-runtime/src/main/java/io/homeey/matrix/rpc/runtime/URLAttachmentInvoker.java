package io.homeey.matrix.rpc.runtime;

import io.homeey.matrix.rpc.core.Invocation;
import io.homeey.matrix.rpc.core.Invoker;
import io.homeey.matrix.rpc.core.Result;
import io.homeey.matrix.rpc.core.SimpleInvocation;
import io.homeey.matrix.rpc.core.URL;

import java.util.HashMap;
import java.util.Map;

/**
 * URL 参数附加器
 *
 * <p>在调用前自动将 URL 中的关键参数添加到 Invocation 的 attachments 中，
 * 使得标签路由、超时配置等能够在调用链中传递。
 *
 * @author jt4mrg@gmail.com
 * @since 2026-01-10
 */
public class URLAttachmentInvoker<T> implements Invoker<T> {

    private final Invoker<T> delegate;
    private final URL url;

    public URLAttachmentInvoker(Invoker<T> delegate, URL url) {
        this.delegate = delegate;
        this.url = url;
    }

    @Override
    public Class<T> getInterface() {
        return delegate.getInterface();
    }

    @Override
    public Result invoke(Invocation invocation) {
        // 将 URL 参数附加到 Invocation
        Map<String, String> attachments = new HashMap<>(invocation.getAttachments());

        // 添加标签路由相关参数
        String tag = url.getParameter("tag");
        if (tag != null && !tag.isEmpty()) {
            attachments.put("tag", tag);
            String tagForce = url.getParameter("tag.force");
            if (tagForce != null) {
                attachments.put("tag.force", tagForce);
            }
        }

        // 添加其他参数
        String timeout = url.getParameter("timeout");
        if (timeout != null) {
            attachments.put("timeout", timeout);
        }
        String retries = url.getParameter("retries");
        if (retries != null) {
            attachments.put("retries", retries);
        }

        // 创建新的 Invocation（带附加参数）
        Invocation newInvocation = new SimpleInvocation(
                invocation.getServiceName(),
                invocation.methodName(),
                invocation.parameterTypes(),
                invocation.arguments(),
                attachments
        );

        // 委托给原始 Invoker
        return delegate.invoke(newInvocation);
    }
}
