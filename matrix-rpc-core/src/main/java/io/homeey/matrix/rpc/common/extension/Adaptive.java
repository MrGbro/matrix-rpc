package io.homeey.matrix.rpc.common.extension;

import java.lang.annotation.*;

/**
 * 自适应扩展点注解
 * 运行时根据URL参数动态选择扩展实现
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface Adaptive {

    /**
     * URL参数key，用于决定使用哪个扩展实现
     */
    String[] value() default {};
}
