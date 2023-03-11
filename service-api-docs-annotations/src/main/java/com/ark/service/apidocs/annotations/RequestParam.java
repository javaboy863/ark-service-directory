package com.ark.service.apidocs.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 类属性/方法参数注解,标注请求参数
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Documented
@Inherited
public @interface RequestParam {

    /**
     * 参数名
     */
    String value() default "";

    /**
     * 是否必传参数
     */
    boolean required() default false;

    /**
     * 参数描述
     */
    String description();

    /**
     * 参数示例
     */
    String example() default "";

    /**
     * 参数默认值
     */
    String defaultValue() default "";

    /**
     * 允许的值,设置该属性后界面上将对参数生成下拉列表 <br />
     * 注:使用该属性后将生成下拉选择框<br />
     * 1. boolean 类型的参数不用设置该属性,将默认生成 true/false 的下拉列表<br />
     * 2. 枚举类型的参数会自动生成下拉列表,如果不想开放全部的枚举值,可以单独设置此属性.
     */
    String[] allowableValues() default {};

}
