package io.homeey.matrix.rpc.transport.api;

import io.homeey.matrix.rpc.core.remoting.Request;
import io.homeey.matrix.rpc.core.remoting.Response;

/**
 *
 * @author jt4mrg@gmail.com
 * @since 2026-01-02
 **/
public interface Client {
    Response send(Request request);
}
