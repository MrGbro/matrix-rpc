package io.homeey.matrix.rpc.common.extension;

import java.lang.annotation.*;

/**
 * SPI扩展点标记注解
 * 标记接口为可扩展的SPI接口
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface SPI {

    /**
     * 默认扩展实现名称
     */
    String value() default "";
}
