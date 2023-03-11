package com.ark.service.apidocs.core.providers;

import com.ark.service.apidocs.core.beans.ModuleCacheItem;

import java.util.List;

/**
 * The api used by Dubbo doc, get the parsed API information.
 */
public interface IDubboDocProvider {

    /**
     * Get basic information of all modules, excluding API parameter information.
     * @return java.lang.String
     */
    String apiModuleList();

    /**
     * Get all information of all modules , including API parameter information.
     * @return java.lang.String
     */
    List<ModuleCacheItem> apiModuleListAndApiInfo();

    /**
     * Get module information according to the complete class name of Dubbo provider interface.
     * @param apiInterfaceClassName
     * @return java.lang.String
     */
    String apiModuleInfo(String apiInterfaceClassName);

    /**
     * Get method parameters and return information according to the complete class name and method name of Dubbo provider interface.
     * @param apiInterfaceClassNameMethodName
     * @return java.lang.String
     */
    String apiParamsResponseInfo(String apiInterfaceClassNameMethodName);

}
