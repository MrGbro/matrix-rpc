package io.homeey.matrix.rpc.codec.protobuf;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import io.homeey.matrix.rpc.codec.api.Codec;
import io.homeey.matrix.rpc.spi.Activate;
import io.homeey.matrix.rpc.spi.ExtensionLoader;

/**
 *
 * @author jt4mrg@gmail.com
 * @since 2026-01-03
 **/
@Activate
public class ProtobufCodec implements Codec {
    private static final String DEFAULT_CODEC_TYPE = "kryo"; // 默认使用 Kryo
    
    @Override
    public byte[] encode(Object object) {
        if (object instanceof RpcProto.RpcRequest || object instanceof RpcProto.RpcResponse) {
            // Protobuf 消息本身直接编码
            if (object instanceof RpcProto.RpcRequest) {
                return ((RpcProto.RpcRequest) object).toByteArray();
            } else {
                return ((RpcProto.RpcResponse) object).toByteArray();
            }
        } else {
            // 普通对象使用默认序列化器
            return encodeComplexObject(object);
        }
    }

    @Override
    public <T> T decode(byte[] bytes, Class<T> clazz) {
        try {
            if (clazz == RpcProto.RpcRequest.class) {
                return (T) RpcProto.RpcRequest.parseFrom(bytes);
            } else if (clazz == RpcProto.RpcResponse.class) {
                return (T) RpcProto.RpcResponse.parseFrom(bytes);
            } else {
                // 普通对象使用默认反序列化器
                return decodeComplexObject(bytes, clazz);
            }
        } catch (InvalidProtocolBufferException e) {
            throw new RuntimeException("Decode failed", e);
        }
    }
    
    private byte[] encodeComplexObject(Object object) {
        Codec codec = ExtensionLoader.getExtensionLoader(Codec.class)
                .getExtension(DEFAULT_CODEC_TYPE);
        return codec.encode(object);
    }
    
    @SuppressWarnings("unchecked")
    private <T> T decodeComplexObject(byte[] bytes, Class<T> clazz) {
        Codec codec = ExtensionLoader.getExtensionLoader(Codec.class)
                .getExtension(DEFAULT_CODEC_TYPE);
        return (T) codec.decode(bytes, clazz);
    }
}
