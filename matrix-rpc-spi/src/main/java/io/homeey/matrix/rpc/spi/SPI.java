package io.homeey.matrix.rpc.spi;

import java.lang.annotation.*;

/**
 * @author jt4mrg@gmail.com
 * @since 2025-12-31
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface SPI {
    String value() default "";
}
