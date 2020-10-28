/*
 * Copyright (c) 2014. Escalon System-Entwicklung, Dietrich Schulten
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for
 * the specific language governing permissions and limitations under the License.
 */

package de.escalon.hypermedia.spring;

import de.escalon.hypermedia.PropertyUtils;
import de.escalon.hypermedia.action.Action;
import de.escalon.hypermedia.action.Cardinality;
import de.escalon.hypermedia.action.Input;
import de.escalon.hypermedia.action.ResourceHandler;
import de.escalon.hypermedia.affordance.ActionDescriptor;
import de.escalon.hypermedia.affordance.ActionInputParameter;
import de.escalon.hypermedia.affordance.DataType;
import de.escalon.hypermedia.affordance.PartialUriTemplate;

import java.beans.PropertyDescriptor;
import java.lang.annotation.Annotation;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jetbrains.annotations.NotNull;
import org.springframework.core.MethodParameter;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.server.MethodLinkBuilderFactory;
import org.springframework.hateoas.server.core.AnnotationMappingDiscoverer;
import org.springframework.hateoas.server.core.DummyInvocationUtils;
import org.springframework.hateoas.server.core.LastInvocationAware;
import org.springframework.hateoas.server.core.MappingDiscoverer;
import org.springframework.hateoas.server.core.MethodInvocation;
import org.springframework.hateoas.server.core.MethodParameters;
import org.springframework.http.HttpEntity;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Factory for {@link AffordanceBuilder}s in a Spring MVC rest service. Normally one should use the static methods of
 * AffordanceBuilder to get an AffordanceBuilder. Created by dschulten on 03.10.2014.
 */
public class AffordanceBuilderFactory implements MethodLinkBuilderFactory<AffordanceBuilder> {

    private static final MappingDiscoverer MAPPING_DISCOVERER = new AnnotationMappingDiscoverer(RequestMapping.class);

    @NotNull
    @Override
    public AffordanceBuilder linkTo(@NotNull Method method, @NotNull Object... parameters) {
        return linkTo(method.getDeclaringClass(), method, parameters);
    }

    @NotNull
    @Override
    public AffordanceBuilder linkTo(@NotNull Class<?> controller, @NotNull Method method, Object... parameters) {
        String pathMapping = MAPPING_DISCOVERER.getMapping(controller, method);

        Map<String, String> requestParamNames = getRequestParamNames(method);
        Map<String, String> inputBeanParamNames = getInputBeanParamNames(method);

        String query = join(requestParamNames, inputBeanParamNames);
        // explicitly write out params where variable name and param are different, rest as continuation
        String mapping = StringUtils.isEmpty(query) ? pathMapping : pathMapping + query;

        PartialUriTemplate partialUriTemplate = new PartialUriTemplate(AffordanceBuilder.getBuilder()
                .build()
                .toString() + mapping);

        Map<String, Object> values = new HashMap<>();
        Iterator<String> variableNames = partialUriTemplate.getVariableNames()
                .iterator();
        // there may be more or less mapping variables than arguments
        for (Object parameter : parameters) {
            if (!variableNames.hasNext()) {
                break;
            }
            values.put(variableNames.next(), parameter);
        }
        // there may be more or less mapping variables than arguments
        // do not use input bean param names here
        for (Object argument : parameters) {
            if (!variableNames.hasNext()) {
                break;
            }
            String variableName = variableNames.next();
            if (!inputBeanParamNames.containsKey(variableName)) {
                values.put(variableName, argument);
            }
        }

        ActionDescriptor actionDescriptor = createActionDescriptor(method, values, parameters);

        return new AffordanceBuilder(partialUriTemplate.expand(values), Collections.singletonList(actionDescriptor));
    }

    private String join(Map<String, String>... params) {
        StringBuilder levelFourQuery = new StringBuilder();
        for (Map<String, String> paramMap : params) {

            for (Map.Entry<String, String> parameter : paramMap.entrySet()) {
                if (levelFourQuery.length() > 0) {
                    levelFourQuery.append(",");
                }
                levelFourQuery.append(parameter.getValue());
            }
        }
        StringBuilder ret = new StringBuilder();
        if (levelFourQuery.length() > 0) {
            ret.append("{?")
                    .append(levelFourQuery)
                    .append("}");
        }
        return ret.toString();
    }

    @NotNull
    @Override
    public AffordanceBuilder linkTo(@NotNull Class<?> target) {
        return linkTo(target, new Object[0]);
    }


    @NotNull
    @Override
    public AffordanceBuilder linkTo(@NotNull Class<?> controller, Object... parameters) {
        Assert.notNull(controller, "Controller should be not null");

        String mapping = MAPPING_DISCOVERER.getMapping(controller);

        PartialUriTemplate partialUriTemplate = new PartialUriTemplate(mapping == null ? "/" : mapping);

        Map<String, Object> values = new HashMap<>();
        Iterator<String> names = partialUriTemplate.getVariableNames()
                .iterator();
        // there may be more or less mapping variables than arguments
        for (Object parameter : parameters) {
            if (!names.hasNext()) {
                break;
            }
            values.put(names.next(), parameter);
        }
        return new AffordanceBuilder().slash(partialUriTemplate.expand(values));
    }

    @NotNull
    @Override
    public AffordanceBuilder linkTo(@NotNull Class<?> controller, @NotNull Map<String, ?> parameters) {
        String mapping = MAPPING_DISCOVERER.getMapping(controller);
        PartialUriTemplate partialUriTemplate = new PartialUriTemplate(mapping == null ? "/" : mapping);
        return new AffordanceBuilder().slash(partialUriTemplate.expand(parameters));
    }

    @NotNull
    @Override
    public AffordanceBuilder linkTo(@NotNull Object invocationValue) {
        Assert.isInstanceOf(LastInvocationAware.class, invocationValue);
        LastInvocationAware invocations = DummyInvocationUtils.getLastInvocationAware(invocationValue);
        if (invocations == null) {
            throw new IllegalStateException(String.format("Could not obtain previous invocation from %s!", invocationValue));
        }

        MethodInvocation invocation = invocations.getLastInvocation();
        Method invokedMethod = invocation.getMethod();

        String pathMapping = MAPPING_DISCOVERER.getMapping(invokedMethod);
        Iterator<Object> classMappingParameters = invocations.getObjectParameters();

        Map<String, String> requestParamNames = getRequestParamNames(invokedMethod);
        Map<String, String> inputBeanParamNames = getInputBeanParamNames(invokedMethod);

        String query = join(requestParamNames, inputBeanParamNames);
        String mapping = StringUtils.isEmpty(query) ? pathMapping : pathMapping + query;

        PartialUriTemplate partialUriTemplate = new PartialUriTemplate(AffordanceBuilder.getBuilder()
                .build()
                .toString() + mapping);

        Map<String, Object> values = new HashMap<>();
        Iterator<String> variableNames = partialUriTemplate.getVariableNames()
                .iterator();
        while (classMappingParameters.hasNext()) {
            values.put(variableNames.next(), classMappingParameters.next());
        }

        // there may be more or less mapping variables than arguments
        // do not use input bean param names here
        for (Object argument : invocation.getArguments()) {
            if (!variableNames.hasNext()) {
                break;
            }
            String variableName = variableNames.next();
            if (!inputBeanParamNames.containsKey(variableName)) {
                values.put(variableName, argument);
            }
        }
        ActionDescriptor actionDescriptor = createActionDescriptor(
                invocation.getMethod(), values, invocation.getArguments());

        return new AffordanceBuilder(partialUriTemplate.expand(values), Collections.singletonList(actionDescriptor));
    }

    private Map<String, String> getInputBeanParamNames(Method invokedMethod) {
        MethodParameters parameters = MethodParameters.of(invokedMethod);

        final List<MethodParameter> inputParams = parameters.getParametersWith(Input.class);

        Map<String, String> ret = new LinkedHashMap<>(inputParams.size());
        for (MethodParameter inputParam : inputParams) {
            Class<?> parameterType = inputParam.getParameterType();
            // only use @Input param which is a bean or map and has no other annotations
            // can't use Spring RequestParam etc. to avoid Spring MVC dependency
            if (inputParam.getParameterAnnotations().length == 1 &&
                    !(DataType.isSingleValueType(parameterType) || DataType.isArrayOrCollection(parameterType))) {
                Input inputAnnotation = inputParam.getParameterAnnotation(Input.class);

                Set<String> explicitlyIncludedParams = new LinkedHashSet<>(inputParams.size());

                Collections.addAll(explicitlyIncludedParams, inputAnnotation.include());
                Collections.addAll(explicitlyIncludedParams, inputAnnotation.hidden());
                Collections.addAll(explicitlyIncludedParams, inputAnnotation.readOnly());

                if (Map.class.isAssignableFrom(parameterType)) {
                    for (String explicitlyIncludedParam : explicitlyIncludedParams) {
                        ret.put(explicitlyIncludedParam, explicitlyIncludedParam);
                    }
                } else {
                    Set<String> inputBeanPropertyNames = getWritablePropertyNames(parameterType);

                    if (explicitlyIncludedParams.isEmpty()) {
                        for (String inputBeanPropertyName : inputBeanPropertyNames) {
                            ret.put(inputBeanPropertyName, inputBeanPropertyName);
                        }
                    } else {
                        for (String explicitlyIncludedParam : explicitlyIncludedParams) {
                            assertInputAnnotationConsistency(inputParam, inputBeanPropertyNames,
                                    explicitlyIncludedParam, "includes");
                            // TODO: have different Qbe property name and variable name?
                            ret.put(explicitlyIncludedParam, explicitlyIncludedParam);
                        }
                    }
                    String[] excludedParams = inputAnnotation.exclude();
                    for (String excludedParam : excludedParams) {
                        assertInputAnnotationConsistency(inputParam, inputBeanPropertyNames,
                                excludedParam, "excludes");
                        ret.remove(excludedParam);
                    }
                }
                break;
            }
        }
        return ret;
    }

    @NotNull
    private Set<String> getWritablePropertyNames(Class<?> parameterType) {
        Set<String> inputBeanPropertyNames = new LinkedHashSet<>();
        Map<String, PropertyDescriptor> propertyDescriptors = PropertyUtils.getPropertyDescriptors
                (parameterType);
        for (PropertyDescriptor propertyDescriptor : propertyDescriptors.values()) {
            if (propertyDescriptor.getWriteMethod() != null) {
                inputBeanPropertyNames.add(propertyDescriptor.getName());
            }
        }
        return inputBeanPropertyNames;
    }

    private void assertInputAnnotationConsistency(MethodParameter inputParam, Set<String> propertiesToCheckAgainst,
                                                  String propertyToCheck, String argumentKind) {
        if (!propertiesToCheckAgainst.contains(propertyToCheck)) {
            throw new IllegalStateException("@Include " +
                    "annotation on parameter '" + inputParam
                    .getParameterName() + "' of method '" + inputParam.getMethod()
                    .toGenericString() +
                    "' " + argumentKind + " property '" +
                    propertyToCheck + "' but there is no such property on " + inputParam
                    .getParameterType()
                    .getName());
        }
    }

    private Map<String, String> getRequestParamNames(Method invokedMethod) {
        MethodParameters parameters = MethodParameters.of(invokedMethod);
        final List<MethodParameter> requestParams = parameters.getParametersWith(RequestParam.class);
        Map<String, String> params = new LinkedHashMap<>(requestParams.size());
        for (MethodParameter requestParam : requestParams) {
            RequestParam requestParamAnnotation = requestParam.getParameterAnnotation(RequestParam.class);
            params.put(requestParam.getParameterName(), requestParamAnnotation.value()
                    .isEmpty() ? requestParam.getParameterName() : requestParamAnnotation.value());
        }

        return params;
    }

    private ActionDescriptor createActionDescriptor(Method invokedMethod,
                                                    Map<String, Object> values, Object[] arguments) {
        RequestMethod httpMethod = getHttpMethod(invokedMethod);
        Type genericReturnType = invokedMethod.getGenericReturnType();

        SpringActionDescriptor actionDescriptor =
                new SpringActionDescriptor(invokedMethod.getName(), httpMethod.name());

        actionDescriptor.setCardinality(getCardinality(invokedMethod, httpMethod, genericReturnType));

        final Action actionAnnotation = AnnotationUtils.getAnnotation(invokedMethod, Action.class);
        if (actionAnnotation != null) {
            actionDescriptor.setSemanticActionType(actionAnnotation.value());
        }

        Map<String, ActionInputParameter> requestBodyMap = getActionInputParameters(RequestBody.class, invokedMethod,
                arguments);
        Assert.state(requestBodyMap.size() < 2, "found more than one request body on " + invokedMethod.getName());
        for (ActionInputParameter value : requestBodyMap.values()) {
            actionDescriptor.setRequestBody(value);
        }

        // the action descriptor needs to know the param type, value and name
        Map<String, ActionInputParameter> requestParamMap =
                getActionInputParameters(RequestParam.class, invokedMethod, arguments);
        for (Map.Entry<String, ActionInputParameter> entry : requestParamMap.entrySet()) {
            ActionInputParameter value = entry.getValue();
            if (value != null) {
                final String key = entry.getKey();
                actionDescriptor.addRequestParam(key, value);
                if (!value.isRequestBody()) {
                    values.put(key, value.getValueFormatted());
                }
            }
        }

        Map<String, ActionInputParameter> pathVariableMap =
                getActionInputParameters(PathVariable.class, invokedMethod, arguments);
        for (Map.Entry<String, ActionInputParameter> entry : pathVariableMap.entrySet()) {
            ActionInputParameter actionInputParameter = entry.getValue();
            if (actionInputParameter != null) {
                final String key = entry.getKey();
                actionDescriptor.addPathVariable(key, actionInputParameter);
                if (!actionInputParameter.isRequestBody()) {
                    values.put(key, actionInputParameter.getValueFormatted());
                }
            }
        }

        Map<String, ActionInputParameter> requestHeadersMap =
                getActionInputParameters(RequestHeader.class, invokedMethod, arguments);

        for (Map.Entry<String, ActionInputParameter> entry : requestHeadersMap.entrySet()) {
            ActionInputParameter actionInputParameter = entry.getValue();
            if (actionInputParameter != null) {
                final String key = entry.getKey();
                actionDescriptor.addRequestHeader(key, actionInputParameter);
                if (!actionInputParameter.isRequestBody()) {
                    values.put(key, actionInputParameter.getValueFormatted());
                }
            }
        }

        return actionDescriptor;
    }

    private Cardinality getCardinality(Method invokedMethod, RequestMethod httpMethod, Type genericReturnType) {
        Cardinality cardinality;

        ResourceHandler resourceAnn = AnnotationUtils.findAnnotation(invokedMethod, ResourceHandler.class);
        if (resourceAnn != null) {
            cardinality = resourceAnn.value();
        } else {
            if (RequestMethod.POST == httpMethod || containsCollection(genericReturnType)) {
                cardinality = Cardinality.COLLECTION;
            } else {
                cardinality = Cardinality.SINGLE;
            }
        }
        return cardinality;
    }

    private boolean containsCollection(Type genericReturnType) {
        final boolean ret;
        if (genericReturnType instanceof ParameterizedType) {
            ParameterizedType t = (ParameterizedType) genericReturnType;
            Type rawType = t.getRawType();
            Assert.state(rawType instanceof Class<?>, "raw type is not a Class: " + rawType.toString());
            Class<?> cls = (Class<?>) rawType;
            if (HttpEntity.class.isAssignableFrom(cls)) {
                Type[] typeArguments = t.getActualTypeArguments();
                ret = containsCollection(typeArguments[0]);
            } else if (CollectionModel.class.isAssignableFrom(cls) || Collection.class.isAssignableFrom(cls)) {
                ret = true;
            } else {
                ret = false;
            }
        } else if (genericReturnType instanceof GenericArrayType) {
            ret = true;
        } else if (genericReturnType instanceof WildcardType) {
            WildcardType t = (WildcardType) genericReturnType;
            ret = containsCollection(getBound(t.getLowerBounds())) || containsCollection(getBound(t.getUpperBounds()));
        } else if (genericReturnType instanceof TypeVariable) {
            ret = false;
        } else if (genericReturnType instanceof Class) {
            Class<?> cls = (Class<?>) genericReturnType;
            ret = CollectionModel.class.isAssignableFrom(cls) ||
                    Collection.class.isAssignableFrom(cls);
        } else {
            ret = false;
        }
        return ret;
    }

    private Type getBound(Type[] lowerBounds) {
        Type ret;
        if (lowerBounds != null && lowerBounds.length > 0) {
            ret = lowerBounds[0];
        } else {
            ret = null;
        }
        return ret;
    }

    private static RequestMethod getHttpMethod(Method method) {
        RequestMapping methodRequestMapping = AnnotationUtils.findAnnotation(method, RequestMapping.class);
        RequestMethod requestMethod;
        if (methodRequestMapping != null) {
            RequestMethod[] methods = methodRequestMapping.method();
            if (methods.length == 0) {
                requestMethod = RequestMethod.GET;
            } else {
                requestMethod = methods[0];
            }
        } else {
            requestMethod = RequestMethod.GET; // default
        }
        return requestMethod;
    }

    /**
     * Returns {@link ActionInputParameter}s contained in the method link.
     *
     * @param annotation to inspect
     * @param method     must not be {@literal null}.
     * @param arguments  to the method link
     * @return maps parameter names to parameter info
     */
    private static Map<String, ActionInputParameter> getActionInputParameters(Class<? extends Annotation> annotation,
                                                                              Method method, Object... arguments
    ) {

        Assert.notNull(method, "MethodInvocation must not be null!");

        MethodParameters parameters = MethodParameters.of(method);
        Map<String, ActionInputParameter> result = new HashMap<>();

        for (MethodParameter parameter : parameters.getParametersWith(annotation)) {
            final int parameterIndex = parameter.getParameterIndex();
            final Object argument;
            if (parameterIndex < arguments.length) {
                argument = arguments[parameterIndex];
            } else {
                argument = null;
            }
            ActionInputParameter inputParameter = new SpringActionInputParameter(parameter, argument);
            result.put(inputParameter.getParameterName(), inputParameter);
        }

        return result;
    }
}
