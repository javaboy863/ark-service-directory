package com.ark.service.apidocs.core.beans;

/**
 * html type enum.
 */
public enum HtmlTypeEnum {

    /**
     * Textbox.
     */
    TEXT,

    /**
     * Textbox, This type will be converted to byte before calling dubbo API.
     */
    TEXT_BYTE,

    /**
     * Textbox, will be limited to one character. This type will be converted to char before calling dubbo API.
     */
    TEXT_CHAR,

    /**
     * Numeric input box, integer.
     */
    NUMBER_INTEGER,

    /**
     * Numeric input box, decimal.
     */
    NUMBER_DECIMAL,

    /**
     * Drop down selection box.
     */
    SELECT,

    /**
     * Text area, which is generally used to show the JSON string of the Java Bean contained in the parameter.
     */
    TEXT_AREA,

    /**
     * date selector.
     */
    DATE_SELECTOR,

    /**
     * datetime selector.
     */
    DATETIME_SELECTOR,
    ;

}
