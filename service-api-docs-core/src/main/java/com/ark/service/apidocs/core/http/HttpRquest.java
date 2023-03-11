package com.ark.service.apidocs.core.http;

import java.util.HashSet;
import java.util.Set;

/**
 * http请求方式.
 */
public enum HttpRquest {
    GET,
    POST,
    PUT,
    DELETE,
    PATCH,
    HEAD,
    OPTIONS,
    ;

    public static HttpRquest of(String method) {
        return HttpRquest.valueOf(method.toUpperCase());
    }

    /**
     * 请求方法是否允许消息体
     */
    public boolean isAllowBody() {
        return sets.contains(this);
    }

    private static final Set<HttpRquest> sets = new HashSet<>();

    static {
        sets.add(POST);
        sets.add(PUT);
        sets.add(DELETE);
        sets.add(PATCH);
    }
}
