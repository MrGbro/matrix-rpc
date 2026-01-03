package io.homeey.matrix.rpc.transport.api;

import io.homeey.matrix.rpc.common.URL;

/**
 *
 * @author jt4mrg@gmail.com
 * @since 2026-01-02
 **/
public interface Transporter {
    Client connect(URL url);

    Server bind(URL url);
}
