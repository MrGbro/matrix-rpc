package io.homeey.matrix.rpc.codec.kryo;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import io.homeey.matrix.rpc.codec.api.Codec;
import io.homeey.matrix.rpc.spi.Activate;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

/**
 * 基于 Kryo 的序列化实现，用于支持复杂对象的高效序列化
 *
 * @author jt4mrg@gmail.com
 * @since 2026-01-09
 */
@Activate
public class KryoCodec implements Codec {
    
    private final ThreadLocal<Kryo> kryoHolder = ThreadLocal.withInitial(() -> {
        Kryo kryo = new Kryo();
        // 设置为true以支持未注册的类（提高灵活性，但安全性稍低）
        kryo.setRegistrationRequired(false);
        // 支持循环引用
        kryo.setReferences(true);
        return kryo;
    });

    @Override
    public byte[] encode(Object object) {
        if (object == null) {
            return new byte[0];
        }
        
        Kryo kryo = kryoHolder.get();
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        Output output = new Output(byteArrayOutputStream);
        
        try {
            kryo.writeClassAndObject(output, object);
            output.flush();
            return byteArrayOutputStream.toByteArray();
        } finally {
            output.close();
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T decode(byte[] bytes, Class<T> clazz) {
        if (bytes == null || bytes.length == 0) {
            return null;
        }
        
        Kryo kryo = kryoHolder.get();
        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(bytes);
        Input input = new Input(byteArrayInputStream);
        
        try {
            return (T) kryo.readClassAndObject(input);
        } finally {
            input.close();
        }
    }
}