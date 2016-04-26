package de.escalon.hypermedia.affordance;

import de.escalon.hypermedia.action.Input;
import de.escalon.hypermedia.action.Type;
import org.springframework.core.MethodParameter;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Map;

/**
 * Interface to represent an input parameter to a resource handler method, independent of a particular ReST framework.
 * Created by Dietrich on 05.04.2015.
 */
public interface AnnotatedParameter {

    Object getCallValue();

    String getCallValueFormatted();

    Type getHtmlInputFieldType();

    boolean isRequestBody();

    boolean isRequestParam();

    boolean isPathVariable();

    boolean isInputParameter();

    String getRequestHeaderName();

    boolean hasInputConstraints();

    <T extends Annotation> T getAnnotation(Class<T> annotation);

    /**
     * Property is hidden according to {@link Input#hidden()}
     * @param property name or property path
     * @return true if hidden
     */
    boolean isHidden(String property);

    /**
     * Determines if request body input parameter has a read-only input property.
     *
     * @param property
     *         name or property path
     * @return true if read-only
     */
    boolean isReadOnly(String property);

    /**
     * Determines if request body input parameter should be included, considering all of {@link Input#include}, {@link
     * Input#hidden} and {@link Input#readOnly}.
     *
     * @param property
     *         name or property path
     * @return true if included or no include statements found
     */
    boolean isIncluded(String property);

    /**
     * Checks if property is excluded according to {@link Input#exclude()}.
     * @param property name or property path
     * @return true if hidden
     */
    boolean isExcluded(String property);

    /**
     * Gets possible values for this parameter.
     *
     * @param annotatedParameters
     *         in case that access to the other parameters is necessary to determine the possible values.
     * @return possible values
     */
    Object[] getPossibleValues(AnnotatedParameters annotatedParameters);

    /**
     * Gets possible values for a method parameter.
     *
     * @param annotatedParameters
     *         in case that access to the other parameters is necessary to determine the possible values.
     * @param method
     *         having parameter
     * @param parameterIndex
     *         of parameter
     * @return possible values or null
     */
    Object[] getPossibleValues(Method method, int parameterIndex, AnnotatedParameters annotatedParameters);

    /**
     * Gets possible values for a constructor parameter.
     *
     * @param annotatedParameters
     *         in case that access to the other parameters is necessary to determine the possible values.
     * @param constructor
     *         having parameter
     * @param parameterIndex
     *         of parameter
     * @return possible values
     */
    Object[] getPossibleValues(Constructor constructor, int parameterIndex, AnnotatedParameters annotatedParameters);

    /**
     * Gets possible values for a constructor parameter.
     *
     * @param annotatedParameters
     *         in case that access to the other parameters is necessary to determine the possible values.
     * @param methodParameter
     *         to get possible values for
     * @return possible values
     */
    Object[] getPossibleValues(MethodParameter methodParameter, AnnotatedParameters annotatedParameters);

    boolean isArrayOrCollection();

    /**
     * Is this action input parameter required, based on the presence of a default value,
     * the parameter annotations and the kind of input parameter.
     *
     * @return true if required
     */
    boolean isRequired();

    String getDefaultValue();

    Object[] getCallValues();

    boolean hasCallValue();

    String getParameterName();

    Class<?> getDeclaringClass();

    Class<?> getParameterType();

    java.lang.reflect.Type getGenericParameterType();

    Map<String, Object> getInputConstraints();
}
