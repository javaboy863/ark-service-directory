package com.ark.service.apidocs.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 方法注解, 接口信息,用于标注一个接口的用途
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
@Documented
public @interface ApiDoc {

    /**
     * 接口名称
     */
    String value();

    /**
     * 接口描述
     */
    String description() default "";

    /**
     * 响应的数据的描述
     */
    String responseClassDescription() default "";

}
