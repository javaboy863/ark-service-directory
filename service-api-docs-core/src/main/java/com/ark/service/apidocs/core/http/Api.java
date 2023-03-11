package com.ark.service.apidocs.core.http;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 接口信息
 *
 */
public class Api {
    /**
     * 应用code
     */
    private String appCode;
    /**
     * 路径
     */
    private String path;

    /**
     * 请求方法
     */
    private HttpRquest method;

    /**
     * 概述标题
     */
    private String summary;

    /**
     * 标签
     */
    private List<String> tags;

    /**
     * 描述
     */
    private String description;

    /**
     * 是否标记过期
     */
    private Boolean deprecated;

    /**
     * 参数
     */
    private List<Property> parameters;

    /**
     * 请求体类型
     */
    private RequestBodyType requestBodyType;

    /**
     * 请求体参数
     */
    private Property requestBody;

    /**
     * 请求体表单
     */
    private List<Property> requestBodyForm;

    /**
     * 响应体
     */
    private Property responses;

    /**
     * 分类
     */
    private String category;

    public List<Property> getParametersByIn(ParameterIn in) {
        if (parameters == null) {
            return Collections.emptyList();
        }
        return parameters.stream().filter(p -> p.getIn() == in).collect(Collectors.toList());
    }

    //------------------generated--------------------//


    public String getAppCode() {
        return appCode;
    }

    public void setAppCode(String appCode) {
        this.appCode = appCode;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public HttpRquest getMethod() {
        return method;
    }

    public void setMethod(HttpRquest method) {
        this.method = method;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Boolean getDeprecated() {
        return deprecated;
    }

    public void setDeprecated(Boolean deprecated) {
        this.deprecated = deprecated;
    }

    public List<Property> getParameters() {
        return parameters;
    }

    public void setParameters(List<Property> parameters) {
        this.parameters = parameters;
    }

    public RequestBodyType getRequestBodyType() {
        return requestBodyType;
    }

    public void setRequestBodyType(RequestBodyType requestBodyType) {
        this.requestBodyType = requestBodyType;
    }

    public Property getRequestBody() {
        return requestBody;
    }

    public void setRequestBody(Property requestBody) {
        this.requestBody = requestBody;
    }

    public Property getResponses() {
        return responses;
    }

    public void setResponses(Property responses) {
        this.responses = responses;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public List<Property> getRequestBodyForm() {
        return requestBodyForm;
    }

    public void setRequestBodyForm(List<Property> requestBodyForm) {
        this.requestBodyForm = requestBodyForm;
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }
}
