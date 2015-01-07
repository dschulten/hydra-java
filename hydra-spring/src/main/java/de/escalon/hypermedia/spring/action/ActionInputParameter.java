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

package de.escalon.hypermedia.spring.action;

import de.escalon.hypermedia.DataType;
import de.escalon.hypermedia.action.Input;
import de.escalon.hypermedia.action.Options;
import de.escalon.hypermedia.action.Select;
import de.escalon.hypermedia.action.Type;
import org.jetbrains.annotations.Nullable;
import org.springframework.core.MethodParameter;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.Property;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.format.support.DefaultFormattingConversionService;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ValueConstants;

import java.lang.annotation.Annotation;
import java.util.*;

/**
 * Holds a method parameter value.
 *
 * @author Dietrich Schulten
 */
public class ActionInputParameter {

    public static final String MIN = "min";
    public static final String MAX = "max";
    public static final String STEP = "step";
    public static final String MIN_LENGTH = "minLength";
    public static final String MAX_LENGTH = "maxLength";
    public static final String PATTERN = "pattern";
    private final TypeDescriptor typeDescriptor;
    private final RequestBody requestBody;
    private final RequestParam requestParam;
    private final PathVariable pathVariable;
    private MethodParameter methodParameter;
    private Object value;
    private Boolean arrayOrCollection = null;
    private Input inputAnnotation;
    private Map<String, Object> inputConstraints = new HashMap<String, Object>();

    private ConversionService conversionService = new DefaultFormattingConversionService();

    /**
     * Creates input parameter descriptor.
     *
     * @param methodParameter   to describe
     * @param value             used during sample invocation
     * @param conversionService to apply to value
     */
    public ActionInputParameter(MethodParameter methodParameter, Object value, ConversionService conversionService) {
        this.methodParameter = methodParameter;
        this.value = value;
        this.requestBody = methodParameter.getParameterAnnotation(RequestBody.class);
        this.requestParam = methodParameter.getParameterAnnotation(RequestParam.class);
        this.pathVariable = methodParameter.getParameterAnnotation(PathVariable.class);
        // always determine input constraints,
        // might be a nested property which is neither requestBody, requestParam nor pathVariable
        this.inputAnnotation = methodParameter.getParameterAnnotation(Input.class);
        if (inputAnnotation != null) {
            putInputConstraint(MIN, Integer.MIN_VALUE, inputAnnotation.min());
            putInputConstraint(MAX, Integer.MAX_VALUE, inputAnnotation.max());
            putInputConstraint(MIN_LENGTH, Integer.MIN_VALUE, inputAnnotation.minLength());
            putInputConstraint(MAX_LENGTH, Integer.MAX_VALUE, inputAnnotation.maxLength());
            putInputConstraint(STEP, 0, inputAnnotation.step());
            putInputConstraint(PATTERN, "", inputAnnotation.pattern());
        }

        this.conversionService = conversionService;
        this.typeDescriptor = TypeDescriptor.nested(methodParameter, 0);
    }

    /**
     * Creates new ActionInputParameter with default formatting conversion service.
     *
     * @param methodParameter holding metadata about the parameter
     * @param value           during sample method invocation
     */
    public ActionInputParameter(MethodParameter methodParameter, Object value) {
        this(methodParameter, value, new DefaultFormattingConversionService());
    }


    private void putInputConstraint(String key, Object defaultValue, Object value) {
        if (!value.equals(defaultValue)) {
            inputConstraints.put(key, value);
        }
    }

    /**
     * The value of the parameter at invocation time.
     *
     * @return value, may be null
     */
    public Object getCallValue() {
        return value;
    }

    /**
     * The value of the parameter at invocation time, formatted according to conversion configuration.
     *
     * @return value, may be null
     */
    @Nullable
    public String getCallValueFormatted() {
        String ret;
        if (value == null) {
            ret = null;
        } else {
            ret = (String) conversionService.convert(value, typeDescriptor, TypeDescriptor.valueOf(String.class));
        }
        return ret;
    }

    /**
     * Gets parameter type for input field according to {@link Type} annotation.
     *
     * @return the type
     */
    public Type getHtmlInputFieldType() {
        final Type ret;
        if (inputAnnotation == null || inputAnnotation.value() == Type.FROM_JAVA) {
            if (isNumber()) {
                ret = Type.NUMBER;
            } else {
                ret = Type.TEXT;
            }
        } else {
            ret = inputAnnotation.value();
        }
        return ret;
    }


    public boolean isRequestBody() {
        return requestBody != null;
    }

    public boolean isRequestParam() {
        return requestParam != null;
    }

    public boolean isPathVariable() {
        return pathVariable != null;
    }

    public boolean hasInputConstraints() {
        return !inputConstraints.isEmpty();
    }

    public <T extends Annotation> T getAnnotation(Class<T> annotation) {
        return methodParameter.getParameterAnnotation(annotation);
    }

    public Object[] getPossibleValues(ActionDescriptor actionDescriptor) {
        try {
            Class<?> parameterType = getParameterType();
            Object[] possibleValues;
            Class<?> nested;
            if (Enum[].class.isAssignableFrom(parameterType)) {
                possibleValues = parameterType.getComponentType()
                        .getEnumConstants();
            } else if (Enum.class.isAssignableFrom(parameterType)) {
                possibleValues = parameterType.getEnumConstants();
            } else if (Collection.class.isAssignableFrom(parameterType)
                    && Enum.class.isAssignableFrom(nested = TypeDescriptor.nested(methodParameter, 1)
                    .getType())) {
                possibleValues = nested.getEnumConstants();
            } else {
                Select select = methodParameter.getParameterAnnotation(Select.class);
                if (select != null) {
                    Class<? extends Options> options = select.options();
                    Options instance = options.newInstance();
                    List<Object> from = new ArrayList<Object>();
                    for (String paramName : select.args()) {
                        ActionInputParameter parameterValue = actionDescriptor.getActionInputParameter(paramName);
                        if (parameterValue != null) {
                            from.add(parameterValue.getCallValue());
                        }
                    }

                    Object[] args = from.toArray();
                    possibleValues = instance.get(select.value(), args);
                } else {
                    possibleValues = new Object[0];
                }
            }
            return possibleValues;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    public Object[] getPossibleValues(Property property, ActionDescriptor actionDescriptor) {
        // TODO remove code duplication of getPossibleValues
        try {
            Class<?> parameterType = property.getType();
            Object[] possibleValues;
            Class<?> nested;
            if (Enum[].class.isAssignableFrom(parameterType)) {
                possibleValues = parameterType.getComponentType()
                        .getEnumConstants();
            } else if (Enum.class.isAssignableFrom(parameterType)) {
                possibleValues = parameterType.getEnumConstants();
            } else if (Collection.class.isAssignableFrom(parameterType)
                    && Enum.class.isAssignableFrom(nested = TypeDescriptor.nested(property, 1)
                    .getType())) {
                possibleValues = nested.getEnumConstants();
            } else {
                Annotation[][] parameterAnnotations = property.getWriteMethod().getParameterAnnotations();
                // setter has exactly one param
                Select select = getSelectAnnotationFromFirstParam(parameterAnnotations[0]);
                if (select != null) {
                    Class<? extends Options> optionsClass = select.options();
                    Options options = optionsClass.newInstance();
                    // collect call values to pass to options.get
                    List<Object> from = new ArrayList<Object>();
                    for (String paramName : select.args()) {
                        ActionInputParameter parameterValue = actionDescriptor.getActionInputParameter(paramName);
                        if (parameterValue != null) {
                            from.add(parameterValue.getCallValue());
                        }
                    }

                    Object[] args = from.toArray();
                    possibleValues = options.get(select.value(), args);
                } else {
                    possibleValues = new Object[0];
                }
            }
            return possibleValues;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private Select getSelectAnnotationFromFirstParam(Annotation[] parameterAnnotation) {
        Select select = null;
        Annotation[] annotationsOnParameter = parameterAnnotation;
        for (Annotation annotation : annotationsOnParameter) {
            if (annotation.getClass() == Select.class) {
                select = (Select) annotation;
                break;
            }
        }
        return select;
    }

    public boolean isArrayOrCollection() {
        if (arrayOrCollection == null) {
            Class<?> parameterType = getParameterType();
            arrayOrCollection = DataType.isArrayOrCollection(parameterType);
        }
        return arrayOrCollection;
    }

    public boolean isBoolean() {
        return DataType.isBoolean(getParameterType());
    }

    public boolean isNumber() {
        return DataType.isNumber(getParameterType());
    }


    public boolean isRequired() {
        boolean ret;
        if (isRequestBody()) {
            ret = requestBody.required();
        } else if (isRequestParam()) {
            ret = ValueConstants.DEFAULT_NONE != requestParam.defaultValue() || requestParam.required();
        } else {
            ret = true;
        }
        return ret;
    }

    /**
     * Determines default value of request param, if available.
     *
     * @return value or null
     */
    public String getDefaultValue() {
        String ret;
        if (isRequestParam()) {
            ret = ValueConstants.DEFAULT_NONE != requestParam.defaultValue() ? requestParam.defaultValue() : null;
        } else {
            ret = null;
        }
        return ret;
    }

    public Object[] getCallValues() {
        Object[] callValues;
        if (!isArrayOrCollection()) {
            throw new UnsupportedOperationException("parameter is not an array or collection");
        }
        Object callValue = getCallValue();
        if (callValue == null) {
            callValues = new Object[0];
        } else {
            Class<?> parameterType = getParameterType();
            if (parameterType.isArray()) {
                callValues = (Object[]) callValue;
            } else {
                callValues = ((Collection<?>) callValue).toArray();
            }
        }
        return callValues;
    }

    public boolean hasCallValue() {
        return value != null;
    }

    public String getParameterName() {
        return methodParameter.getParameterName();
    }

    public Class<?> getParameterType() {
        return methodParameter.getParameterType();
    }

    public java.lang.reflect.Type getGenericParameterType() {
        return methodParameter.getGenericParameterType();
    }

    public Class<?> getNestedParameterType() {
        return methodParameter.getNestedParameterType();
    }

    public Map<String, Object> getInputConstraints() {
        return inputConstraints;
    }

}
