package io.homeey.matrix.rpc.codec.api;

import io.homeey.matrix.rpc.spi.Activate;
import io.homeey.matrix.rpc.spi.SPI;

/**
 *
 * @author jt4mrg@gmail.com
 * @since 2026-01-03
 **/
@SPI("matrix")
public interface Codec {
    /**
     * 将对象编码为字节数组
     *
     * @param object 需要编码的对象
     * @return 编码后的字节数组
     */
    byte[] encode(Object object);

    /**
     * 将字节数组解码为指定类型的对象
     *
     * @param bytes 字节数组
     * @param clazz 目标对象类型
     * @param <T>   目标对象类型泛型
     * @return 解码后的对象
     */
    <T> T decode(byte[] bytes, Class<T> clazz);
}
