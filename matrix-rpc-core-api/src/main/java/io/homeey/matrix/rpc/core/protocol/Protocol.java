package io.homeey.matrix.rpc.core.protocol;

import io.homeey.matrix.rpc.common.URL;
import io.homeey.matrix.rpc.core.invoker.Invoker;
import io.homeey.matrix.rpc.spi.SPI;

@SPI("local")
public interface Protocol {

    /**
     * Export a remote service to local
     *
     * @param invoker the invoker of the service
     * @param <T>     the type of the service
     * @return the exporter of the service
     */
    <T> Exporter<T> export(Invoker<T> invoker);

    /**
     * Refer a remote service to local
     *
     * @param type the type of the service
     * @param url  the url of the service
     * @param <T>  the type of the service
     * @return the invoker of the service
     */
    <T> Invoker<T> refer(Class<T> type, URL url);
}
