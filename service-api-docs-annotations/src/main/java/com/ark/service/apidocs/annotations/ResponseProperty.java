package com.ark.service.apidocs.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 类属性注解, 标注响应参数
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD})
@Documented
@Inherited
public @interface ResponseProperty {

    /**
     * 参数名
     */
    String value();

    /**
     * 示例
     */
    String example() default "";

}
