package com.ark.service.apidocs.core.providers;

import com.ark.service.apidocs.core.DubboApiDocsCache;
import com.ark.service.apidocs.core.beans.ModuleCacheItem;

import java.util.List;

/**
 * The api implementation of Dubbo doc.
 */
public class DubboDocProviderImpl implements IDubboDocProvider {

    @Override
    public String apiModuleList() {
        return DubboApiDocsCache.getBasicApiModuleInfo();
    }

    @Override
    public List<ModuleCacheItem> apiModuleListAndApiInfo() {
        return DubboApiDocsCache.getAllApiModuleInfo();
    }

    @Override
    public String apiModuleInfo(String apiInterfaceClassName) {
        return DubboApiDocsCache.getApiModuleStr(apiInterfaceClassName);
    }

    @Override
    public String apiParamsResponseInfo(String apiInterfaceClassNameMethodName) {
        return DubboApiDocsCache.getApiParamsAndRespStr(apiInterfaceClassNameMethodName);
    }
}
