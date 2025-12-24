package io.homeey.nexa.core.rpc;

import lombok.Data;

import java.io.Serializable;

/**
 *
 * @author jt4mrg@gmail.com
 * @since 2025-12-24 23:10
 **/
@Data
public class Response implements Serializable {
    private static final long serialVersionUID = -9132555225847073163L;

    //响应状态
    public static final byte OK = 0;
    public static final byte CLIENT_TIMEOUT = 30;
    public static final byte SERVER_TIMEOUT = 31;
    public static final byte BAD_REQUEST = 40;
    public static final byte BAD_RESPONSE = 50;
    public static final byte SERVICE_NOT_FOUND = 60;
    public static final byte SERVICE_ERROR = 70;
    public static final byte SERVER_ERROR = 80;


    private long requestId;
    private final byte status = OK;
    private Result result;
    private String errorMsg;
}
