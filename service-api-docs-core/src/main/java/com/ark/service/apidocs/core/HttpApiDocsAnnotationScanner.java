package com.ark.service.apidocs.core;

import com.alibaba.fastjson.JSON;
import com.ark.service.apidocs.annotations.ApiDoc;
import com.ark.service.apidocs.annotations.RequestParam;
import com.ark.service.apidocs.annotations.ResponseProperty;
import com.ark.service.apidocs.core.http.Api;
import com.ark.service.apidocs.core.http.DataTypes;
import com.ark.service.apidocs.core.http.HttpRquest;
import com.ark.service.apidocs.core.http.ParameterIn;
import com.ark.service.apidocs.core.http.Property;
import com.ark.service.apidocs.core.http.RequestBodyType;
import com.ark.service.apidocs.core.http.RequestMappingAnno;
import com.ark.service.apidocs.core.http.Value;
import com.ark.service.apidocs.utils.HttpClientUtils;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

/**
 * Scan and process http doc annotations.
 *
 */
@Order(Ordered.LOWEST_PRECEDENCE)
public class HttpApiDocsAnnotationScanner implements ApplicationListener<ApplicationReadyEvent> {
    private static final Logger LOG = LoggerFactory.getLogger(HttpApiDocsAnnotationScanner.class);
    /**
     * ??????????????????http????????????
     */
    private static final String REMOTE_SERVER_HTTP_PATH = "/http/api/saveHttpApi";
    /**
     * ??????????????????
     */
    private ExecutorService executorService = new ThreadPoolExecutor(1, 1,
            0L, TimeUnit.MILLISECONDS,
            new LinkedBlockingQueue<>(),
            r -> new Thread(r, "Thread-httpReport"));
    /**
     * ???????????????
     */
    private static final String PATH_DELIMITER = "/";

    @Autowired
    private ApplicationContext applicationContext;

    @org.springframework.beans.factory.annotation.Value("${appCode}")
    private String appCode;

    @org.springframework.beans.factory.annotation.Value("${service.directory.report.url}")
    private String reportUrl;

    @Override
    public void onApplicationEvent(ApplicationReadyEvent applicationReadyEvent) {
        if (reportUrl == null || "".equals(reportUrl)) {
            LOG.info("================= Http API Docs--not exist reportUrl and exited ================");
            return;
        }
        try {
            LOG.info("================= Http API Docs--Start scanning and processing doc annotations ================");
            execute();
        } catch (Exception e) {
            LOG.info("================= Http API Docs-- doc annotations scanning and processing error ================", e);
        }
        LOG.info("================= Http API Docs-- doc annotations scanning and processing completed ================");
    }

    /**
     * ????????????
     */
    private void execute() {
        List<Api> moduleApiList = new ArrayList<>();
        // ????????????controller
        Map<String, Object> requestMappings = applicationContext.getBeansWithAnnotation(Controller.class);
        // ????????????controller??????ApiDoc?????????????????????????????????
        requestMappings.forEach((beanName, controllerBean) -> {
            Class<?> controllerClass;
            if (AopUtils.isAopProxy(controllerBean)) {
                controllerClass = AopUtils.getTargetClass(controllerBean);
            } else {
                controllerClass = controllerBean.getClass();
            }
            // ??????????????????
            Method[] apiModuleMethods = controllerClass.getMethods();
            Api api = null;
            for (Method method : apiModuleMethods) {
                if (method.isAnnotationPresent(ApiDoc.class)) {
                    try {
                        // ???????????????api
                        api = parseMethod(controllerBean, method);
                        if (api != null) {
                            LOG.info("================= Http API Docs-- processing " + controllerClass.getName() + "#" + method.getName() + "() completed, api:" + JSON.toJSONString(api) + " ================");
                            moduleApiList.add(api);
                        }
                    } catch (Exception ex) {
                        LOG.error("================= Http API Docs-- doc annotations processing error. method:" + controllerClass.getName() + "#" + method.getName() + " ================", ex);
                    }
                }
            }
        });
        // ??????api???????????????
        reportHttpApi(moduleApiList);
    }

    /**
     * ????????????apiDoc?????????http??????
     *
     * @param bean
     * @param method
     * @return
     */
    private Api parseMethod(Object bean, Method method) {
        ApiDoc apiDoc = method.getAnnotation(ApiDoc.class);
        if (apiDoc == null) {
            return null;
        }
        RequestMappingAnno beanReqMapping = getRequestMappingAnno(bean.getClass().getAnnotations());
        RequestMappingAnno methodReqMapping = getRequestMappingAnno(method.getAnnotations());
        if (StringUtils.isBlank(beanReqMapping.getPath()) && StringUtils.isBlank(methodReqMapping.getPath())) {
            return null;
        }
        // ????????????
        Api retApi = new Api();
        // ????????????
        retApi.setPath(requestPath(beanReqMapping, methodReqMapping));
        // ????????????
        retApi.setMethod(HttpRquest.of(requestHttpMethod(method, beanReqMapping, methodReqMapping)));
        // ????????????
        retApi.setSummary(apiDoc.value());
        // ??????
        retApi.setDescription(apiDoc.description());
        // ????????????
        retApi.setDeprecated(method.getAnnotation(Deprecated.class) != null || bean.getClass().getAnnotation(Deprecated.class) != null);
        // ??????????????????
        fullRequestInfo(retApi, method);
        // ??????????????????
        fullResponseInfo(retApi, method);
        return retApi;
    }


    /**
     * ??????????????????
     *
     * @param beanReqMapping
     * @param methodReqMapping
     * @return
     */
    private String requestPath(RequestMappingAnno beanReqMapping, RequestMappingAnno methodReqMapping) {
        StringBuffer retBuffer = new StringBuffer();
        if (beanReqMapping != null && StringUtils.isNotEmpty(beanReqMapping.getPath())) {
            retBuffer.append(pathResolve(beanReqMapping.getPath()));
        }
        if (methodReqMapping != null && StringUtils.isNotEmpty(methodReqMapping.getPath())) {
            retBuffer.append(pathResolve(methodReqMapping.getPath()));
        }
        return retBuffer.toString();
    }

    /**
     * ??????http???????????????
     *
     * @param method
     * @param beanReqMapping
     * @param methodReqMapping
     * @return
     */
    private String requestHttpMethod(Method method, RequestMappingAnno beanReqMapping, RequestMappingAnno methodReqMapping) {
        if (methodReqMapping != null && StringUtils.isNotBlank(methodReqMapping.getMethod())) {
            return methodReqMapping.getMethod();
        }
        if (beanReqMapping != null && StringUtils.isNotBlank(beanReqMapping.getMethod())) {
            return beanReqMapping.getMethod();
        }
        // requestMapping?????????????????????http???method??????????????????????????????????????????
        int parameterCount = method.getParameterCount();
        if (parameterCount == 0) {
            // ??????
            return HttpRquest.GET.name();
        }
        Annotation[][] parameterAnnotations = method.getParameterAnnotations();
        HttpRquest getRequest = null;
        for (int i = 0; i < parameterCount; i++) {
            if (getTargetClassAnno(parameterAnnotations[i], RequestParam.class) != null) {
                return HttpRquest.POST.name();
            } else if (getTargetClassAnno(parameterAnnotations[i], RequestBody.class) != null) {
                return HttpRquest.POST.name();
            } else if (getTargetClassAnno(parameterAnnotations[i], PathVariable.class) != null) {
                getRequest = HttpRquest.GET;
            }
        }
        if (getRequest != null) {
            return getRequest.name();
        }
        if (parameterCount == 1 && parameterAnnotations[0].length == 0) {
            return HttpRquest.POST.name();
        }
        // ??????POST??????????????????
        return HttpRquest.POST.name();
    }


    /**
     * ??????????????????????????????????????????
     *
     * @param api
     * @param method
     */
    private void fullRequestInfo(Api api, Method method) {
        // header????????????
        ParameterIn parameterIn = null;
        if (api.getMethod() == HttpRquest.GET) {
            api.setRequestBodyType(RequestBodyType.raw);
            parameterIn = ParameterIn.query;
        } else {
            api.setRequestBodyType(requestBodyType(method));
        }
        // ????????????
        Class<?>[] argsClass = method.getParameterTypes();
        Annotation[][] argsAnns = method.getParameterAnnotations();
        Parameter[] parameters = method.getParameters();
        List<Property> properties = new ArrayList<>(argsClass.length);
        for (int i = 0; i < argsClass.length; i++) {
            properties.add(parseParamBeanType(argsClass[i], argsAnns[i], parameters[i], parameterIn));
        }
        if (properties.isEmpty()) {
            return;
        }
        if (api.getRequestBodyType() == RequestBodyType.json) {
            if (properties.size() > 0) {
                api.setRequestBody(properties.get(0));
            }
        } else if (api.getRequestBodyType() == RequestBodyType.form) {
            api.setRequestBodyForm(properties);
        } else {
            api.setParameters(properties);
        }
    }

    /**
     * ??????????????????????????????
     *
     * @param api
     * @param method
     */
    private void fullResponseInfo(Api api, Method method) {
        Class<?> clazz = method.getReturnType();
        String httpDataType = getHttpDataType(clazz);
        Property property = new Property();
        property.setType(httpDataType);
        if (DataTypes.OBJECT.equals(httpDataType)) {
            // ???json??????
            property.setProperties(parseRespObjectGeneric(clazz));
        }
        ApiDoc apiDoc = getTargetClassAnno(method.getAnnotations(), ApiDoc.class);
        property.setDescription(apiDoc != null ? apiDoc.responseClassDescription() : "");
        api.setResponses(property);
    }

    /**
     * http???body????????????
     *
     * @param method
     * @return
     */
    private RequestBodyType requestBodyType(Method method) {
        // requestMapping?????????????????????http???method??????????????????????????????????????????
        int parameterCount = method.getParameterCount();
        if (parameterCount == 0) {
            return RequestBodyType.raw;
        }
        Annotation[][] parameterAnnotations = method.getParameterAnnotations();
        HttpRquest getRequest = null;
        for (int i = 0; i < parameterCount; i++) {
            if (getTargetClassAnno(parameterAnnotations[i], RequestParam.class) != null) {
                return RequestBodyType.form;
            } else if (getTargetClassAnno(parameterAnnotations[i], RequestBody.class) != null) {
                return RequestBodyType.json;
            } else if (getTargetClassAnno(parameterAnnotations[i], PathVariable.class) != null) {
                getRequest = HttpRquest.GET;
            }
        }
        if (getRequest != null) {
            return RequestBodyType.raw;
        }
        if (parameterCount == 1 && parameterAnnotations[0].length == 0) {
            return RequestBodyType.json;
        }

        return RequestBodyType.json;
    }

    /**
     * ??????????????????
     *
     * @param clazz
     * @param annotations
     * @param parameters
     * @return
     */
    private Property parseParamBeanType(Class<?> clazz, Annotation[] annotations, Parameter parameters, ParameterIn in) {
        Property property = new Property();
        // ??????
        property.setName(parameters.getName());
        // ??????
        property.setType(getHttpDataType(clazz));
        // ????????????
        property.setIn(in);
        // ????????????
        property.setRequired(false);
        // ??????????????????????????? ????????????
        RequestParam requestParam = getTargetClassAnno(annotations, RequestParam.class);
        if (requestParam != null) {
            // ??????
            // property.setName(requestParam.value());
            // ??????
            property.setDescription(requestParam.description());
            // ????????????
            property.setRequired(requestParam.required());
            // ????????????
            property.setExample(requestParam.example());
        }
        if (DataTypes.OBJECT.equals(property.getType())) {
            property.setProperties(parseObjectGeneric(clazz));
        }


        return property;
    }

    /**
     * ??????Bean??????
     *
     * @param clazz
     * @return
     */
    private Map<String, Property> parseObjectGeneric(Class clazz) {
        if (clazz == null) {
            return null;
        }
        if (Map.class.isAssignableFrom(clazz)) {
            return null;
        } else {
            Field[] fields = clazz.getDeclaredFields();
            Map<String, Property> properties = new HashMap<>(fields.length);
            for (Field field : fields) {
                Property fieldProperty = parseBeanField(field.getType(), field.getAnnotations(), field);
                if (fieldProperty == null) {
                    continue;
                }
                properties.put(field.getName(), fieldProperty);
            }
            return properties;
        }
    }

    /**
     * ????????????Bean??????
     *
     * @param clazz
     * @return
     */
    private Map<String, Property> parseRespObjectGeneric(Class clazz) {
        if (clazz == null) {
            return null;
        }
        if (Map.class.isAssignableFrom(clazz)) {
            return null;
        } else {
            Field[] fields = clazz.getDeclaredFields();
            Map<String, Property> properties = new HashMap<>(fields.length);
            for (Field field : fields) {
                Property fieldProperty = parseRespBeanField(field.getType(), field.getAnnotations(), field);
                if (fieldProperty == null) {
                    continue;
                }
                properties.put(field.getName(), fieldProperty);
            }
            return properties;
        }
    }

    /**
     * ??????array???collection??????????????????
     *
     * @param field
     * @return
     */
    private Property parseArrayGeneric(Field field) {
        Class targetClazz = null;
        Class srcClazz = field.getType();
        if (Collection.class.isAssignableFrom(srcClazz)) {
            ParameterizedType stringListType = (ParameterizedType) field.getGenericType();
            targetClazz = (Class) stringListType.getActualTypeArguments()[0];
        } else if (srcClazz.isArray()) {
            targetClazz = srcClazz.getComponentType();
        }
        if (targetClazz == null || targetClazz == srcClazz) {
            // ?????????????????????????????????
            return null;
        }
        Property property = new Property();
        property.setType(getHttpDataType(targetClazz));
        // ?????? targetClazz ??????
        if (DataTypes.OBJECT.equals(property.getType())) {
            property.setProperties(parseObjectGeneric(targetClazz));
        }
        return property;
    }

    /**
     * ??????array???collection??????????????????
     *
     * @param field
     * @return
     */
    private Property parseRespArrayGeneric(Field field) {
        Class targetClazz = null;
        Class srcClazz = field.getType();
        if (Collection.class.isAssignableFrom(srcClazz)) {
            ParameterizedType stringListType = (ParameterizedType) field.getGenericType();
            targetClazz = (Class) stringListType.getActualTypeArguments()[0];
        } else if (srcClazz.isArray()) {
            targetClazz = srcClazz.getComponentType();
        }
        if (targetClazz == null || targetClazz == srcClazz) {
            // ?????????????????????????????????
            return null;
        }
        Property property = new Property();
        property.setType(getHttpDataType(targetClazz));
        // ?????? targetClazz ??????
        if (DataTypes.OBJECT.equals(property.getType())) {
            property.setProperties(parseRespObjectGeneric(targetClazz));
        }
        return property;
    }


    /**
     * ??????????????????
     *
     * @param clazz
     * @param annotations
     * @param field
     * @return
     */
    private Property parseBeanField(Class<?> clazz, Annotation[] annotations, Field field) {
        RequestParam requestParam = getTargetClassAnno(annotations, RequestParam.class);
        if (requestParam == null) {
            // ????????????@RequestParam????????????
            return null;
        }
        Property retProperty = new Property();
        // ????????????
        // retProperty.setName(StringUtils.defaultIfBlank(requestParam.value(), field.getName()));
        // ??????
        retProperty.setType(getHttpDataType(clazz));
        // ??????
        retProperty.setDescription(requestParam.description());
        // ????????????
        retProperty.setRequired(requestParam.required());
        // ????????????
        retProperty.setExample(requestParam.example());
        // ?????????
        retProperty.setDefaultValue(requestParam.defaultValue());
        // ?????????
        retProperty.setValues(getValueList(requestParam, clazz));
        // ???type???array
        if (DataTypes.ARRAY.equals(retProperty.getType())) {
            retProperty.setItems(parseArrayGeneric(field));
        }
        // ???type???object
        if (DataTypes.OBJECT.equals(retProperty.getType())) {
            retProperty.setProperties(parseObjectGeneric(field.getType()));
        }
        return retProperty;
    }

    /**
     * ??????????????????
     *
     * @param clazz
     * @param annotations
     * @param field
     * @return
     */
    private Property parseRespBeanField(Class<?> clazz, Annotation[] annotations, Field field) {
        ResponseProperty responseProperty = getTargetClassAnno(annotations, ResponseProperty.class);
        if (responseProperty == null) {
            // ????????????@responseProperty????????????
            return null;
        }
        Property retProperty = new Property();
        // ??????
        retProperty.setType(getHttpDataType(clazz));
        // ??????
        retProperty.setDescription(responseProperty.value());
        // ????????????
        retProperty.setExample(responseProperty.example());
        // ?????????
        retProperty.setValues(getValueList(null, clazz));
        // ???type???array
        if (DataTypes.ARRAY.equals(retProperty.getType())) {
            retProperty.setItems(parseRespArrayGeneric(field));
        }
        // ???type???object
        if (DataTypes.OBJECT.equals(retProperty.getType())) {
            retProperty.setProperties(parseRespObjectGeneric(field.getType()));
        }
        return retProperty;
    }

    /**
     * ??????requestMapping??????
     *
     * @return
     */
    private RequestMappingAnno getRequestMappingAnno(Annotation[] annotations) {
        if (annotations == null || annotations.length == 0) {
            return null;
        }
        RequestMappingAnno result = new RequestMappingAnno();
        RequestMapping requestMapping = getTargetClassAnno(annotations, RequestMapping.class);
        if (requestMapping != null) {
            result.setPath(getPath(requestMapping.path(), requestMapping.value()));
            RequestMethod method = getArrayZeroIndex(requestMapping.method());
            if (method != null) {
                result.setMethod(method.name());
            }
            return result;
        }
        GetMapping getMapping = getTargetClassAnno(annotations, GetMapping.class);
        if (getMapping != null) {
            result.setPath(getPath(getMapping.path(), getMapping.value()));
            result.setMethod(RequestMethod.GET.name());
            return result;
        }
        PostMapping postMapping = getTargetClassAnno(annotations, PostMapping.class);
        if (postMapping != null) {
            result.setPath(getPath(postMapping.path(), postMapping.value()));
            result.setMethod(RequestMethod.POST.name());
            return result;
        }
        PutMapping putMapping = getTargetClassAnno(annotations, PutMapping.class);
        if (putMapping != null) {
            result.setPath(getPath(putMapping.path(), putMapping.value()));
            result.setMethod(RequestMethod.PUT.name());
            return result;
        }
        PatchMapping patchMapping = getTargetClassAnno(annotations, PatchMapping.class);
        if (patchMapping != null) {
            result.setPath(getPath(patchMapping.path(), patchMapping.value()));
            result.setMethod(RequestMethod.PATCH.name());
            return result;
        }
        return result;
    }

    /**
     * ?????????????????????
     *
     * @param requestParam
     * @param classType
     * @return
     */
    private List<Value> getValueList(RequestParam requestParam, Class classType) {
        List<Value> resultList = null;
        // ??????????????????????????????????????????
        String[] allowableValues = requestParam != null ? requestParam.allowableValues() : null;
        if (allowableValues != null && allowableValues.length > 0) {
            resultList = new ArrayList<>();
            for (String str : allowableValues) {
                resultList.add(new Value(str, null));
            }
            return resultList;
        }
        // boolean???????????????????????????????????????
        if (Boolean.class.isAssignableFrom(classType) || boolean.class.isAssignableFrom(classType)) {
            resultList = Arrays.asList(new Value(Constants.ALLOWABLE_BOOLEAN_TRUE, null), new Value(Constants.ALLOWABLE_BOOLEAN_FALSE, null));
        } else if (Enum.class.isAssignableFrom(classType)) {
            Object[] enumConstants = classType.getEnumConstants();
            resultList = new ArrayList<>(enumConstants.length);
            try {
                Method getNameMethod = classType.getMethod(Constants.METHOD_NAME_NAME);
                for (int i = 0; i < enumConstants.length; i++) {
                    Object obj = enumConstants[i];
                    Value value = new Value((String) getNameMethod.invoke(obj), null);
                    resultList.add(value);
                }
            } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
                LOG.error(e.getMessage(), e);
            }
            if (resultList.size() == 0) {
                for (int i = 0; i < enumConstants.length; i++) {
                    Value value = new Value(String.valueOf(enumConstants[i]), null);
                    resultList.add(value);
                }
            }
        }
        return resultList;
    }

    /**
     * ??????http??????????????????????????????????????????
     *
     * @param moduleApiList
     */
    private void reportHttpApi(List<Api> moduleApiList) {
        if (moduleApiList == null || moduleApiList.size() == 0) {
            LOG.info("================= Http API Docs-- http api is empty , exited ================");
            return;
        }
        executorService.execute(() -> {
            String serverUrl = !reportUrl.endsWith(PATH_DELIMITER) ? reportUrl : reportUrl.substring(0, reportUrl.length() - 1);
            serverUrl += REMOTE_SERVER_HTTP_PATH;
            LOG.info("================= Http API Docs-- start repot api serverPath:" + serverUrl + " ================");
            for (Api api : moduleApiList) {
                api.setAppCode(appCode);
                String params = JSON.toJSONString(api);
                HttpClientUtils.post(serverUrl, params);
            }
        });
    }


    /**
     * ?????????????????????http???????????????
     *
     * @param classType
     * @return
     */
    private String getHttpDataType(Class classType) {
        String resultDataType = DataTypes.OBJECT;

        if (String.class.isAssignableFrom(classType)) {
            resultDataType = DataTypes.STRING;
        } else if (Integer.class.isAssignableFrom(classType) || int.class.isAssignableFrom(classType)) {
            resultDataType = DataTypes.INTEGER;
        } else if (Byte.class.isAssignableFrom(classType) || byte.class.isAssignableFrom(classType)) {
            resultDataType = DataTypes.STRING;
        } else if (Long.class.isAssignableFrom(classType) || long.class.isAssignableFrom(classType)) {
            resultDataType = DataTypes.INTEGER;
        } else if (Double.class.isAssignableFrom(classType) || double.class.isAssignableFrom(classType)) {
            resultDataType = DataTypes.NUMBER;
        } else if (Float.class.isAssignableFrom(classType) || float.class.isAssignableFrom(classType)) {
            resultDataType = DataTypes.NUMBER;
        } else if (Character.class.isAssignableFrom(classType) || char.class.isAssignableFrom(classType)) {
            resultDataType = DataTypes.STRING;
        } else if (Short.class.isAssignableFrom(classType) || short.class.isAssignableFrom(classType)) {
            resultDataType = DataTypes.INTEGER;
        } else if (Date.class.isAssignableFrom(classType) || LocalDateTime.class.isAssignableFrom(classType)) {
            resultDataType = DataTypes.DATETIME;
        } else if (LocalDate.class.isAssignableFrom(classType)) {
            resultDataType = DataTypes.DATETIME;
        } else if (classType.isArray() || Collection.class.isAssignableFrom(classType)) {
            resultDataType = DataTypes.ARRAY;
        } else if (Boolean.class.isAssignableFrom(classType) || boolean.class.isAssignableFrom(classType)) {
            resultDataType = DataTypes.BOOLEAN;
        } else if (Enum.class.isAssignableFrom(classType)) {
            resultDataType = DataTypes.STRING;
        } else if (BigDecimal.class.isAssignableFrom(classType) || BigInteger.class.isAssignableFrom(classType)) {
            resultDataType = DataTypes.NUMBER;
        }

        return resultDataType;
    }

    /**
     * ??????????????????
     *
     * @param path
     * @param defaultPath ??????????????????
     * @return
     */
    private String getPath(String[] path, String[] defaultPath) {
        String result = getArrayZeroIndex(path);
        if (result != null) {
            return result;
        }
        return getArrayZeroIndex(defaultPath);
    }


    /**
     * ??????????????????T?????????
     *
     * @param annotations
     * @param targetAnnoClass
     * @param <T>             ??????T
     * @return
     */
    private <T> T getTargetClassAnno(Annotation[] annotations, Class<T> targetAnnoClass) {
        if (annotations == null || annotations.length == 0) {
            return null;
        }
        for (Annotation ann : annotations) {
            if (ann.annotationType() == targetAnnoClass) {
                return (T) ann;
            }
        }
        return null;
    }

    /**
     * ????????????????????????????????????
     *
     * @param arr
     * @param <T>
     * @return
     */
    public <T> T getArrayZeroIndex(T[] arr) {
        if (arr != null && arr.length > 0) {
            return arr[0];
        }
        return null;
    }

    /**
     * ????????????????????????
     *
     * @param path
     * @return
     */
    private String pathResolve(String path) {
        StringBuffer retStr = new StringBuffer();
        if (!path.startsWith(PATH_DELIMITER)) {
            retStr.append(PATH_DELIMITER);
        }
        retStr.append(path);
        if (path.endsWith(PATH_DELIMITER)) {
            retStr.setLength(retStr.length() - 1);
        }
        return retStr.toString();
    }
}


