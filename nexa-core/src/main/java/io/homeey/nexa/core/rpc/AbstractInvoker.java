package io.homeey.nexa.core.rpc;

import io.homeey.nexa.common.URL;
import io.homeey.nexa.common.exception.RpcException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author jt4mrg@gmail.com
 * @since 2025-12-24 22:46
 **/
public abstract class AbstractInvoker<T> implements Invoker<T> {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractInvoker.class.getName());

    private final Class<T> type;
    private final URL url;
    private volatile boolean available = true;

    public AbstractInvoker(Class<T> type, URL url) {
        if (url == null) {
            throw new IllegalArgumentException("URL cannot be null");
        }
        if (type == null) {
            throw new IllegalArgumentException("Interface cannot be null");
        }
        this.type = type;
        this.url = url;
    }

    @Override
    public URL getURL() {
        return this.url;
    }

    @Override
    public boolean isAvailable() {
        return available;
    }

    @Override
    public void destroy() {
        available = false;
    }

    @Override
    public Class<T> getInterface() {
        return this.type;
    }

    @Override
    public Result invoke(Invocation invocation) {
        if (!isAvailable()) {
            throw new RpcException("Invoker is destroy:" + this);
        }
        try {
            return doInvoke(invocation);
        } catch (Throwable e) {
            LOGGER.error("Failed to invoke remote service:" + invocation, e);
            Result result = new Result();
            result.setException(e);
            return result;
        }
    }

    protected abstract Result doInvoke(Invocation invocation) throws Throwable;
}
