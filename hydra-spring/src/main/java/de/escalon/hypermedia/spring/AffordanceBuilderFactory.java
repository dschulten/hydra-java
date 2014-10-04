/*
 * Copyright (c) 2014. Escalon System-Entwicklung, Dietrich Schulten
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package de.escalon.hypermedia.spring;

import de.escalon.hypermedia.action.Action;
import de.escalon.hypermedia.spring.action.ActionDescriptor;
import de.escalon.hypermedia.spring.action.ActionInputParameter;
import org.apache.commons.lang3.text.WordUtils;
import org.springframework.core.MethodParameter;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.hateoas.MethodLinkBuilderFactory;
import org.springframework.hateoas.UriTemplate;
import org.springframework.hateoas.core.AnnotationMappingDiscoverer;
import org.springframework.hateoas.core.DummyInvocationUtils;
import org.springframework.hateoas.core.MappingDiscoverer;
import org.springframework.hateoas.core.MethodParameters;
import org.springframework.hateoas.mvc.UriComponentsContributor;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.util.*;

/**
 * Created by dschulten on 03.10.2014.
 */
public class AffordanceBuilderFactory implements MethodLinkBuilderFactory<AffordanceBuilder> {

    private static final MappingDiscoverer DISCOVERER = new AnnotationMappingDiscoverer(RequestMapping.class);

    private List<UriComponentsContributor> uriComponentsContributors = new ArrayList<UriComponentsContributor>();

    @Override
    public AffordanceBuilder linkTo(Method method, Object... parameters) {
        return AffordanceBuilder.linkTo(method, parameters);
    }

    @Override
    public AffordanceBuilder linkTo(Class<?> type, Method method, Object... parameters) {
        return AffordanceBuilder.linkTo(type, method, parameters);
    }

    @Override
    public AffordanceBuilder linkTo(Class<?> target) {
        return AffordanceBuilder.linkTo(target);
    }

    @Override
    public AffordanceBuilder linkTo(Class<?> target, Object... parameters) {
        return AffordanceBuilder.linkTo(target, parameters);
    }

    @Override
    public AffordanceBuilder linkTo(Object invocationValue) {

        Assert.isInstanceOf(DummyInvocationUtils.LastInvocationAware.class, invocationValue);
        DummyInvocationUtils.LastInvocationAware invocations = (DummyInvocationUtils.LastInvocationAware) invocationValue;

        DummyInvocationUtils.MethodInvocation invocation = invocations.getLastInvocation();
        Iterator<Object> classMappingParameters = invocations.getObjectParameters();
        Method invokedMethod = invocation.getMethod();

        String mapping = DISCOVERER.getMapping(invokedMethod);
        UriComponentsBuilder builder = AffordanceBuilder.getBuilder()
                .path(mapping);

        UriTemplate template = new UriTemplate(mapping);
        Map<String, Object> values = new HashMap<String, Object>();

        Iterator<String> names = template.getVariableNames()
                .iterator();
        while (classMappingParameters.hasNext()) {
            values.put(names.next(), classMappingParameters.next());
        }
        RequestMethod requestMethod = getRequestMethod(invokedMethod);

        final Action action = getAnnotation(invokedMethod, Action.class);
        String actionName;
        if (action != null) {
            actionName = action.value() + "Action";
        } else {
            actionName = WordUtils.capitalize(invokedMethod.getName()) + "Action";
        }

        ActionDescriptor actionDescriptor = new ActionDescriptor(actionName, requestMethod);

        // the action descriptor needs to know the param type, value and name
        Map<String, ActionInputParameter> requestParamMap = getActionInputParameters(invocation, RequestParam.class);

        for (Map.Entry<String, ActionInputParameter> entry : requestParamMap.entrySet()) {
            ActionInputParameter value = entry.getValue();
            if (value != null) {
                final String key = entry.getKey();
                actionDescriptor.addRequestParam(key, value);
                values.put(key, value.getCallValueFormatted());
            }
        }

        Map<String, ActionInputParameter> pathVariableMap = getActionInputParameters(invocation, PathVariable.class);
        for (Map.Entry<String, ActionInputParameter> entry : pathVariableMap.entrySet()) {
            ActionInputParameter value = entry.getValue();
            if (value != null) {
                final String key = entry.getKey();
                actionDescriptor.addPathVariable(key, value);
                values.put(key, value.getCallValueFormatted());
            }
        }

        // TODO make contributor configurable, find out what it does
        UriComponents components = applyUriComponentsContributor(builder, invocation).buildAndExpand(values);
        UriComponentsBuilder linkBuilder = UriComponentsBuilder.fromUriString(components.toUriString());

        UriComponents uri = linkBuilder.build();
        UriComponentsBuilder actionUriBuilder = UriComponentsBuilder.newInstance();
        actionUriBuilder.scheme(uri.getScheme())
                .userInfo(uri.getUserInfo())
                .host(uri.getHost())
                .port(uri.getPort())
                .path(uri.getPath());

        actionDescriptor.setActionLink(actionUriBuilder.build());
        // TODO hold uri in Affordance, not in ActionDescriptor?

        return new AffordanceBuilder(actionUriBuilder, actionDescriptor);

    }

    // TODO reuse same code from  JacksonHydraSerializer
    private <T extends Annotation> T getAnnotation(AnnotatedElement annotated, Class<T> annotationClass) {
        T ret;
        if (annotated == null) {
            ret = null;
        } else {
            ret = annotated.getAnnotation(annotationClass);
        }
        return ret;
    }

    private static RequestMethod getRequestMethod(Method method) {
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
     * Returns {@link ActionInputParameter}s contained in the recorded {@link org.springframework.hateoas.core.DummyInvocationUtils.MethodInvocation}.
     *
     * @param invocation must not be {@literal null}.
     * @return maps parameter names to parameter info
     */
    private static Map<String, ActionInputParameter> getActionInputParameters(
            DummyInvocationUtils.MethodInvocation invocation,
            Class<? extends Annotation> annotation) {

        Assert.notNull(invocation, "MethodInvocation must not be null!");

        MethodParameters parameters = new MethodParameters(invocation.getMethod());
        Object[] arguments = invocation.getArguments();
        Map<String, ActionInputParameter> result = new HashMap<String, ActionInputParameter>();

        for (MethodParameter parameter : parameters.getParametersWith(annotation)) {
            result.put(parameter.getParameterName(),
                    new ActionInputParameter(parameter, arguments[parameter.getParameterIndex()]));
        }

        return result;
    }

    /**
     * Applies the configured {@link UriComponentsContributor}s to the given {@link UriComponentsBuilder}.
     *
     * @param builder    will never be {@literal null}.
     * @param invocation will never be {@literal null}.
     * @return
     */
    protected UriComponentsBuilder applyUriComponentsContributor(UriComponentsBuilder builder,
                                                                 DummyInvocationUtils.MethodInvocation invocation) {

        MethodParameters parameters = new MethodParameters(invocation.getMethod());
        Iterator<Object> parameterValues = Arrays.asList(invocation.getArguments())
                .iterator();

        for (MethodParameter parameter : parameters.getParameters()) {
            Object parameterValue = parameterValues.next();
            for (UriComponentsContributor contributor : uriComponentsContributors) {
                if (contributor.supportsParameter(parameter)) {
                    contributor.enhance(builder, parameter, parameterValue);
                }
            }
        }

        return builder;
    }
}
