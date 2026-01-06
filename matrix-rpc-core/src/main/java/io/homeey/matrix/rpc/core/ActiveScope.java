package io.homeey.matrix.rpc.core;

/**
 *
 * @author jt4mrg@gmail.com
 * @since 2026-01-07
 **/
public enum ActiveScope {
    /**
     * 示该过滤器只适用于消费者
     */
    CONSUMER,
    /**
     * 表示该过滤器只适用于提供者
     */
    PROVIDER
}
