package com.ark.service.apidocs.core.http;

/**
 * requestMap注解
 *
 */
public class RequestMappingAnno {

    private String path;

    private String method;

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }
}
