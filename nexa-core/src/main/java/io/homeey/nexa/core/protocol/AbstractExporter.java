package io.homeey.nexa.core.protocol;

import io.homeey.nexa.core.rpc.Invoker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author jt4mrg@gmail.com
 * @since 2025-12-25 23:16
 **/
public abstract class AbstractExporter<T> implements Exporter<T> {
    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractExporter.class);
    private final Invoker<T> invoker;
    private volatile boolean exported = true;

    public AbstractExporter(Invoker<T> invoker) {
        if (invoker == null) {
            throw new IllegalArgumentException("invoker == null");
        }
        this.invoker = invoker;
    }

    @Override
    public void unExport() {
        if (exported) {
            this.exported = false;
            this.getInvoker().destroy();
        }
    }

    @Override
    public Invoker<T> getInvoker() {
        return invoker;
    }

    @Override
    public String toString() {
        return getInvoker().toString();
    }
}
