package com.ark.service.apidocs;

import com.ark.service.apidocs.core.DubboApiDocsAnnotationScanner;
import com.ark.service.apidocs.core.HttpApiDocsAnnotationScanner;
import org.springframework.context.annotation.Import;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 开启服务api文档注解.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
@Documented
@Inherited
@Import({DubboApiDocsAnnotationScanner.class, HttpApiDocsAnnotationScanner.class})
public @interface EnableApiDocs {
}
