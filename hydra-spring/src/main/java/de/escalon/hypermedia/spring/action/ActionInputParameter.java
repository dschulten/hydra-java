/*
 * Copyright (c) 2014. Escalon System-Entwicklung, Dietrich Schulten
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package de.escalon.hypermedia.spring.action;

import de.escalon.hypermedia.action.Input;
import de.escalon.hypermedia.action.Options;
import de.escalon.hypermedia.action.Select;
import de.escalon.hypermedia.action.Type;
import org.springframework.core.MethodParameter;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.format.support.DefaultFormattingConversionService;

import java.util.*;

/**
 * Holds a method parameter value.
 *
 * @author Dietrich Schulten
 */
public class ActionInputParameter {

    private final TypeDescriptor typeDescriptor;
    private MethodParameter methodParameter;
    private Object value;
    private Boolean arrayOrCollection = null;
    private Input inputAnnotation;
    private Map<String, Object> inputConditions = new HashMap<String, Object>();
    private int upToCollectionItems = 3;

    private ConversionService conversionService = new DefaultFormattingConversionService();


    public ActionInputParameter(MethodParameter methodParameter, Object value, ConversionService conversionService) {
        this.methodParameter = methodParameter;
        this.value = value;
        this.inputAnnotation = methodParameter.getParameterAnnotation(Input.class);
        if (inputAnnotation != null) {
            putInputCondition("min", Integer.MIN_VALUE, inputAnnotation.min());
            putInputCondition("max", Integer.MAX_VALUE, inputAnnotation.max());
            putInputCondition("step", 0, inputAnnotation.step());
            this.upToCollectionItems = inputAnnotation.upTo();
        }
        this.conversionService = conversionService;
        this.typeDescriptor = TypeDescriptor.nested(methodParameter, 0);
    }

    public ActionInputParameter(MethodParameter methodParameter, Object value) {
        this(methodParameter, value, new DefaultFormattingConversionService());
    }

    public int getUpToCollectionItems() {
        return upToCollectionItems;
    }

    private void putInputCondition(String key, int defaultValue, int value) {
        if (value != defaultValue) {
            inputConditions.put(key, value);
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
    public String getCallValueFormatted() {
        String ret;
        if (value == null) {
            ret = null;
        } else {
            ret = (String) conversionService.convert(value, typeDescriptor, TypeDescriptor.valueOf(String.class));
        }
        return ret;
    }

    public Type getInputFieldType() {
        final Type ret;
        if (inputAnnotation == null || inputAnnotation.value() == Type.FROM_JAVA) {
            Class<?> parameterType = getParameterType();
            if (Number.class.isAssignableFrom(parameterType)) {
                ret = Type.NUMBER;
            } else {
                ret = Type.TEXT;
            }
        } else {
            ret = inputAnnotation.value();
        }
        return ret;
    }

    public boolean hasInputConditions() {
        return !inputConditions.isEmpty();
    }


    public Object[] getPossibleValues(ActionDescriptor actionDescriptor) {
        // TODO: other sources of possible values, e.g. max, min, step
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
                        ActionInputParameter parameterValue = actionDescriptor.getParameterValue(paramName);
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

    public boolean isArrayOrCollection() {
        if (arrayOrCollection == null) {
            Class<?> parameterType = getParameterType();
            arrayOrCollection = (parameterType.isArray() || Collection.class.isAssignableFrom(parameterType));
        }
        return arrayOrCollection;
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

    Class<?> getParameterType() {
        return methodParameter.getParameterType();
    }

    public Map<String, Object> getInputConditions() {
        return inputConditions;
    }

}
