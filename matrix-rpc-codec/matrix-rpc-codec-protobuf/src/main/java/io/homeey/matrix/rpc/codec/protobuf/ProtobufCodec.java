package io.homeey.matrix.rpc.codec.protobuf;

import com.google.protobuf.InvalidProtocolBufferException;
import io.homeey.matrix.rpc.codec.api.Codec;
import io.homeey.matrix.rpc.spi.Activate;

/**
 *
 * @author jt4mrg@gmail.com
 * @since 2026-01-03
 **/
@Activate
public class ProtobufCodec implements Codec {
    @Override
    public byte[] encode(Object object) {
        if (object instanceof RpcProto.RpcRequest) {
            return ((RpcProto.RpcRequest) object).toByteArray();
        } else if (object instanceof RpcProto.RpcResponse) {
            return ((RpcProto.RpcResponse) object).toByteArray();
        }
        throw new IllegalArgumentException("Unsupported object type: " + object.getClass());
    }

    @Override
    public <T> T decode(byte[] bytes, Class<T> clazz) {
        try {
            if (clazz == RpcProto.RpcRequest.class) {
                return (T) RpcProto.RpcRequest.parseFrom(bytes);
            } else if (clazz == RpcProto.RpcResponse.class) {
                return (T) RpcProto.RpcResponse.parseFrom(bytes);
            }
        } catch (InvalidProtocolBufferException e) {
            throw new RuntimeException("Decode failed", e);
        }
        throw new IllegalArgumentException("Unsupported type: " + clazz);
    }
}
