package io.homeey.matrix.rpc.cluster.router;

import io.homeey.matrix.rpc.cluster.api.Router;
import io.homeey.matrix.rpc.core.Invocation;
import io.homeey.matrix.rpc.core.URL;
import io.homeey.matrix.rpc.spi.Activate;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 标签路由实现
 *
 * <p>路由逻辑：
 * <ol>
 *   <li>从 Invocation 的 attachments 中获取请求标签（如 tag=gray）</li>
 *   <li>从 URL 参数中获取实例标签（如 tag=gray）</li>
 *   <li>优先返回标签匹配的实例</li>
 *   <li>如果没有匹配实例，降级到无标签实例</li>
 *   <li>如果设置了 force=true，则不降级，返回空列表</li>
 * </ol>
 *
 * <p>使用示例：
 * <pre>
 * // Provider 端设置标签
 * RpcService.export(EchoService.class, new EchoServiceImpl(), 20880)
 *     .tag("gray")  // 设置为灰度实例
 *     .await();
 *
 * // Consumer 端指定标签
 * RpcReference.create(EchoService.class)
 *     .address("localhost", 20880)
 *     .tag("gray")  // 只调用灰度实例
 *     .get();
 * </pre>
 *
 * @author jt4mrg@gmail.com
 * @since 2026-01-10
 */
@Activate(order = 10)
public class TagRouter implements Router {

    private static final String TAG_KEY = "tag";
    private static final String TAG_FORCE_KEY = "tag.force";

    @Override
    public int getPriority() {
        return 10;
    }

    @Override
    public List<URL> route(List<URL> providers, Invocation invocation) {
        if (providers == null || providers.isEmpty()) {
            return Collections.emptyList();
        }

        // 1. 获取请求标签
        String requestTag = invocation.getAttachments().get(TAG_KEY);

        // 2. 无标签请求，只返回无标签实例
        if (requestTag == null || requestTag.isEmpty()) {
            return providers.stream()
                    .filter(url -> url.getParameter(TAG_KEY) == null || url.getParameter(TAG_KEY).isEmpty())
                    .collect(Collectors.toList());
        }

        // 3. 过滤匹配标签的实例
        List<URL> taggedProviders = providers.stream()
                .filter(url -> requestTag.equals(url.getParameter(TAG_KEY)))
                .collect(Collectors.toList());

        // 4. 如果有匹配的实例，直接返回
        if (!taggedProviders.isEmpty()) {
            return taggedProviders;
        }

        // 5. 降级逻辑：返回无标签实例作为兜底
        boolean force = "true".equals(invocation.getAttachments().get(TAG_FORCE_KEY));
        if (force) {
            // 强制标签路由，不降级
            System.out.println("[TagRouter] No provider available for tag: " + requestTag + ", force mode enabled, returning empty list");
            return Collections.emptyList();
        }

        // 返回无标签实例作为兜底
        List<URL> fallbackProviders = providers.stream()
                .filter(url -> url.getParameter(TAG_KEY) == null || url.getParameter(TAG_KEY).isEmpty())
                .collect(Collectors.toList());

        if (!fallbackProviders.isEmpty()) {
            System.out.println("[TagRouter] No provider available for tag: " + requestTag + ", fallback to " + fallbackProviders.size() + " untagged providers");
        }

        return fallbackProviders;
    }

    @Override
    public boolean isEnabled() {
        // 默认启用标签路由
        return true;
    }
}
