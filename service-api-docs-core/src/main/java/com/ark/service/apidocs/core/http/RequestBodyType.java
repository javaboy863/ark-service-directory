package com.ark.service.apidocs.core.http;

/**
 * 请求体类型
 */
public enum RequestBodyType {
    form("application/x-www-form-urlencoded"),
    form_data("multipart/form-data"),
    json("application/json;charset=utf-8"),
    raw(""),
    ;

    private final String contentType;

    RequestBodyType(String contentType) {
        this.contentType = contentType;
    }

    public String getContentType() {
        return contentType;
    }
}
