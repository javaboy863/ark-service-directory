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
     * 远程服务器的http请求路径
     */
    private static final String REMOTE_SERVER_HTTP_PATH = "/http/api/saveHttpApi";
    /**
     * 创建单个线程
     */
    private ExecutorService executorService = new ThreadPoolExecutor(1, 1,
            0L, TimeUnit.MILLISECONDS,
            new LinkedBlockingQueue<>(),
            r -> new Thread(r, "Thread-httpReport"));
    /**
     * 路径分隔符
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
     * 执行事件
     */
    private void execute() {
        List<Api> moduleApiList = new ArrayList<>();
        // 获取所有controller
        Map<String, Object> requestMappings = applicationContext.getBeansWithAnnotation(Controller.class);
        // 遍历所有controller，将ApiDoc注解内容上报到服务目录
        requestMappings.forEach((beanName, controllerBean) -> {
            Class<?> controllerClass;
            if (AopUtils.isAopProxy(controllerBean)) {
                controllerClass = AopUtils.getTargetClass(controllerBean);
            } else {
                controllerClass = controllerBean.getClass();
            }
            // 遍历所有方法
            Method[] apiModuleMethods = controllerClass.getMethods();
            Api api = null;
            for (Method method : apiModuleMethods) {
                if (method.isAnnotationPresent(ApiDoc.class)) {
                    try {
                        // 解析方法为api
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
        // 上报api到服务目录
        reportHttpApi(moduleApiList);
    }

    /**
     * 解析存在apiDoc注解的http方法
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
        // 组装结果
        Api retApi = new Api();
        // 请求路径
        retApi.setPath(requestPath(beanReqMapping, methodReqMapping));
        // 请求方法
        retApi.setMethod(HttpRquest.of(requestHttpMethod(method, beanReqMapping, methodReqMapping)));
        // 接口名称
        retApi.setSummary(apiDoc.value());
        // 描述
        retApi.setDescription(apiDoc.description());
        // 是否过期
        retApi.setDeprecated(method.getAnnotation(Deprecated.class) != null || bean.getClass().getAnnotation(Deprecated.class) != null);
        // 完善请求数据
        fullRequestInfo(retApi, method);
        // 完善返回数据
        fullResponseInfo(retApi, method);
        return retApi;
    }


    /**
     * 获取请求路径
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
     * 获取http的请求方法
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
        // requestMapping注解上没有添加http的method方法，通过参数数量和注解判断
        int parameterCount = method.getParameterCount();
        if (parameterCount == 0) {
            // 默认
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
        // 默认POST通吃一切请求
        return HttpRquest.POST.name();
    }


    /**
     * 请求数据接口文档请求数据解析
     *
     * @param api
     * @param method
     */
    private void fullRequestInfo(Api api, Method method) {
        // header参数设置
        ParameterIn parameterIn = null;
        if (api.getMethod() == HttpRquest.GET) {
            api.setRequestBodyType(RequestBodyType.raw);
            parameterIn = ParameterIn.query;
        } else {
            api.setRequestBodyType(requestBodyType(method));
        }
        // 请求参数
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
     * 返回结果对象文档解析
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
            // 是json对象
            property.setProperties(parseRespObjectGeneric(clazz));
        }
        ApiDoc apiDoc = getTargetClassAnno(method.getAnnotations(), ApiDoc.class);
        property.setDescription(apiDoc != null ? apiDoc.responseClassDescription() : "");
        api.setResponses(property);
    }

    /**
     * http的body请求类型
     *
     * @param method
     * @return
     */
    private RequestBodyType requestBodyType(Method method) {
        // requestMapping注解上没有添加http的method方法，通过参数数量和注解判断
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
     * 方法参数解析
     *
     * @param clazz
     * @param annotations
     * @param parameters
     * @return
     */
    private Property parseParamBeanType(Class<?> clazz, Annotation[] annotations, Parameter parameters, ParameterIn in) {
        Property property = new Property();
        // 名称
        property.setName(parameters.getName());
        // 类型
        property.setType(getHttpDataType(clazz));
        // 参数位置
        property.setIn(in);
        // 是否必填
        property.setRequired(false);
        // 注解内容优先级大， 放在最后
        RequestParam requestParam = getTargetClassAnno(annotations, RequestParam.class);
        if (requestParam != null) {
            // 名称
            // property.setName(requestParam.value());
            // 描述
            property.setDescription(requestParam.description());
            // 是否必填
            property.setRequired(requestParam.required());
            // 请求示例
            property.setExample(requestParam.example());
        }
        if (DataTypes.OBJECT.equals(property.getType())) {
            property.setProperties(parseObjectGeneric(clazz));
        }


        return property;
    }

    /**
     * 解析Bean对象
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
     * 解析返回Bean对象
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
     * 解析array或collection中的泛型对象
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
            // 空对象和相同对象不解析
            return null;
        }
        Property property = new Property();
        property.setType(getHttpDataType(targetClazz));
        // 解析 targetClazz 类型
        if (DataTypes.OBJECT.equals(property.getType())) {
            property.setProperties(parseObjectGeneric(targetClazz));
        }
        return property;
    }

    /**
     * 解析array或collection中的泛型对象
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
            // 空对象和相同对象不解析
            return null;
        }
        Property property = new Property();
        property.setType(getHttpDataType(targetClazz));
        // 解析 targetClazz 类型
        if (DataTypes.OBJECT.equals(property.getType())) {
            property.setProperties(parseRespObjectGeneric(targetClazz));
        }
        return property;
    }


    /**
     * 解析对象字段
     *
     * @param clazz
     * @param annotations
     * @param field
     * @return
     */
    private Property parseBeanField(Class<?> clazz, Annotation[] annotations, Field field) {
        RequestParam requestParam = getTargetClassAnno(annotations, RequestParam.class);
        if (requestParam == null) {
            // 没有注解@RequestParam，不解析
            return null;
        }
        Property retProperty = new Property();
        // 字段名称
        // retProperty.setName(StringUtils.defaultIfBlank(requestParam.value(), field.getName()));
        // 类型
        retProperty.setType(getHttpDataType(clazz));
        // 描述
        retProperty.setDescription(requestParam.description());
        // 是否必须
        retProperty.setRequired(requestParam.required());
        // 请求示例
        retProperty.setExample(requestParam.example());
        // 默认值
        retProperty.setDefaultValue(requestParam.defaultValue());
        // 值列表
        retProperty.setValues(getValueList(requestParam, clazz));
        // 当type为array
        if (DataTypes.ARRAY.equals(retProperty.getType())) {
            retProperty.setItems(parseArrayGeneric(field));
        }
        // 当type为object
        if (DataTypes.OBJECT.equals(retProperty.getType())) {
            retProperty.setProperties(parseObjectGeneric(field.getType()));
        }
        return retProperty;
    }

    /**
     * 解析对象字段
     *
     * @param clazz
     * @param annotations
     * @param field
     * @return
     */
    private Property parseRespBeanField(Class<?> clazz, Annotation[] annotations, Field field) {
        ResponseProperty responseProperty = getTargetClassAnno(annotations, ResponseProperty.class);
        if (responseProperty == null) {
            // 没有注解@responseProperty，不解析
            return null;
        }
        Property retProperty = new Property();
        // 类型
        retProperty.setType(getHttpDataType(clazz));
        // 描述
        retProperty.setDescription(responseProperty.value());
        // 返回示例
        retProperty.setExample(responseProperty.example());
        // 值列表
        retProperty.setValues(getValueList(null, clazz));
        // 当type为array
        if (DataTypes.ARRAY.equals(retProperty.getType())) {
            retProperty.setItems(parseRespArrayGeneric(field));
        }
        // 当type为object
        if (DataTypes.OBJECT.equals(retProperty.getType())) {
            retProperty.setProperties(parseRespObjectGeneric(field.getType()));
        }
        return retProperty;
    }

    /**
     * 获取requestMapping注解
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
     * 解析候选值列表
     *
     * @param requestParam
     * @param classType
     * @return
     */
    private List<Value> getValueList(RequestParam requestParam, Class classType) {
        List<Value> resultList = null;
        // 注解类中给定的候选值优先处理
        String[] allowableValues = requestParam != null ? requestParam.allowableValues() : null;
        if (allowableValues != null && allowableValues.length > 0) {
            resultList = new ArrayList<>();
            for (String str : allowableValues) {
                resultList.add(new Value(str, null));
            }
            return resultList;
        }
        // boolean或者枚举类型，则给定候选值
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
     * 上报http接口内容到远程服务目录服务器
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
     * 通过类型，判断http的字段类型
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
     * 获取给定路径
     *
     * @param path
     * @param defaultPath 候选默认路径
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
     * 查找给定类型T的注解
     *
     * @param annotations
     * @param targetAnnoClass
     * @param <T>             泛型T
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
     * 获取数组的第一个下标位置
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
     * 绝对路径开头处理
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


