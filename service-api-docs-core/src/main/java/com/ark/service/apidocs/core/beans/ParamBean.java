package com.ark.service.apidocs.core.beans;

import com.alibaba.fastjson.annotation.JSONField;
import com.ark.service.apidocs.annotations.RequestParam;
import java.util.List;

/**
 * Parameter bean corresponding to {@link RequestParam}, for caching.
 */
public class ParamBean {

    /**
     * parameter name.
     */
    private String name;

    /**
     * parameter name, for display.
     */
    private String docName;

    /**
     * required.
     */
    private Boolean required;

    /**
     * description.
     */
    private String description;

    /**
     * example.
     */
    private String example;

    /**
     * default value.
     */
    private String defaultValue;

    /**
     * java type of parameter.
     */
    private String javaType;

    /**
     * What HTML elements should this parameter display.
     */
    private HtmlTypeEnum htmlType;

    /**
     * allowed values
     */
    private String[] allowableValues;

    /**
     * If the parameter in a request bean is not a basic data type,
     * the {@link #subParams} will have a value.
     * Because the HTML form is not easy to display this parameter,
     * it will be displayed as a text area, and the JSON string of this parameter will be filled in.
     */
    @JSONField(serialize = false)
    private List<ParamBean> subParams;

    /**
     * JSON string corresponding to {@link #subParams}.
     */
    private String subParamsJson;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDocName() {
        return docName;
    }

    public void setDocName(String docName) {
        this.docName = docName;
    }

    public Boolean getRequired() {
        return required;
    }

    public void setRequired(Boolean required) {
        this.required = required;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getExample() {
        return example;
    }

    public void setExample(String example) {
        this.example = example;
    }

    public String getDefaultValue() {
        return defaultValue;
    }

    public void setDefaultValue(String defaultValue) {
        this.defaultValue = defaultValue;
    }

    public String getJavaType() {
        return javaType;
    }

    public void setJavaType(String javaType) {
        this.javaType = javaType;
    }

    public HtmlTypeEnum getHtmlType() {
        return htmlType;
    }

    public void setHtmlType(HtmlTypeEnum htmlType) {
        this.htmlType = htmlType;
    }

    public String[] getAllowableValues() {
        return allowableValues;
    }

    public void setAllowableValues(String[] allowableValues) {
        this.allowableValues = allowableValues;
    }

    public List<ParamBean> getSubParams() {
        return subParams;
    }

    public void setSubParams(List<ParamBean> subParams) {
        this.subParams = subParams;
    }

    public String getSubParamsJson() {
        return subParamsJson;
    }

    public void setSubParamsJson(String subParamsJson) {
        this.subParamsJson = subParamsJson;
    }
}
