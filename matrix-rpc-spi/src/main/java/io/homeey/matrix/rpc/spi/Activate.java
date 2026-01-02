package io.homeey.matrix.rpc.spi;

import java.lang.annotation.*;

/**
 *
 * @author jt4mrg@gmail.com
 * @since 2026-01-01
 **/
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Activate {
    /**
     * 作用域,默认为全部作用域
     */
    String[] scope() default {"CONSUMER", "PROVIDER"};

    /**
     * 激活优先级，值越小越靠前
     */
    int order() default 0;

    /**
     * 条件 key（预留）
     */
    String[] value() default {};
}
