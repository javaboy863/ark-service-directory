package com.ark.service.apidocs.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 类注解, 接口模块信息,用于标注一个接口类模块的用途
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
@Documented
public @interface ApiModule {

    /**
     * 模块名称
     */
    String value();

    /**
     * 提供者实现的接口
     */
    Class<?> apiInterface();

    /**
     * 模块版本
     */
    @Deprecated
    String version() default "";

}
