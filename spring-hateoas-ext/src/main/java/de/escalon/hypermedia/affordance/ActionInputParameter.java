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
public interface ActionInputParameter {

    /**
     * Raw field value.
     * @return value
     */
    Object getCallValue();

    /**
     * Formatted field value to be used as preset value.
     * @return
     */
    String getCallValueFormatted();

    /**
     * Type of parameter when used in html-like contexts (e.g. Siren, Uber, XHtml)
     * @return type
     */
    Type getHtmlInputFieldType();

    /**
     * Parameter is a complex body.
     * @return
     */
    boolean isRequestBody();

    /**
     * Parameter is a request header.
     * @return
     */
    boolean isRequestHeader();

    /**
     * Parameter is a query parameter.
     * @return
     */
    boolean isRequestParam();

    /**
     * Parameter is a path variable.
     * @return
     */
    boolean isPathVariable();

    /**
     * Parameter has an @Input annotation.
     * @return
     */
    boolean isInputParameter();

    /**
     * Gets request header name.
     * @return name
     */
    String getRequestHeaderName();

    /**
     * Parameter has input constraints (like range, step etc.)
     * @return
     */
    boolean hasInputConstraints();

    /**
     * If the action input parameter is annotation-based, provide access to annotation
     * @param annotation to look for
     * @param <T> type of annotation
     * @return annotation or null
     */
    <T extends Annotation> T getAnnotation(Class<T> annotation);

    /**
     * Property is hidden, e.g. according to {@link Input#hidden()}
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
     * Determines if request body input parameter should be included. E.g. considering all of {@link Input#include}, {@link
     * Input#hidden} and {@link Input#readOnly}.
     *
     * @param property
     *         name or property path
     * @return true if included
     */
    boolean isIncluded(String property);

    /**
     * Checks if property should be excluded according to {@link Input#exclude()}.
     * @param property name or property path
     * @return true if excluded
     */
    boolean isExcluded(String property);

    /**
     * Gets possible values for this parameter.
     *
     * @param actionDescriptor
     *         in case that access to the other parameters is necessary to determine the possible values.
     * @return possible values or empty array
     */
    Object[] getPossibleValues(ActionDescriptor actionDescriptor);

    /**
     * Gets possible values for a method parameter.
     *
     * @param actionDescriptor
     *         in case that access to the other parameters is necessary to determine the possible values.
     * @param method
     *         having parameter
     * @param parameterIndex
     *         of parameter
     * @return possible values or empty array
     */
    Object[] getPossibleValues(Method method, int parameterIndex, ActionDescriptor actionDescriptor);

    /**
     * Gets possible values for a constructor parameter.
     *
     * @param actionDescriptor
     *         in case that access to the other parameters is necessary to determine the possible values.
     * @param constructor
     *         having parameter
     * @param parameterIndex
     *         of parameter
     * @return possible values or empty array
     */
    Object[] getPossibleValues(Constructor constructor, int parameterIndex, ActionDescriptor actionDescriptor);

    /**
     * Gets possible values for a constructor parameter.
     *
     * @param actionDescriptor
     *         in case that access to the other parameters is necessary to determine the possible values.
     * @param methodParameter
     *         to get possible values for
     * @return possible values or empty array
     */
    Object[] getPossibleValues(MethodParameter methodParameter, ActionDescriptor actionDescriptor);

    /**
     * Parameter is an array or collection, think {?val*} in uri template
     * @return
     */
    boolean isArrayOrCollection();

    /**
     * Is this action input parameter required, based on the presence of a default value,
     * the parameter annotations and the kind of input parameter.
     *
     * @return true if required
     */
    boolean isRequired();

//    /**
//     *
//     * @return
//     */
//    String getDefaultValue();

    /**
     * If parameter is an array or collection, the default values.
     * @return values
     */
    Object[] getCallValues();

    /**
     * Does the parameter have a default value?
     * @return true if a default is present
     */
    boolean hasCallValue();

    /**
     * Name of parameter.
     * @return
     */
    String getParameterName();

    /**
     * Type of parameter.
     * @return
     */
    Class<?> getParameterType();

    /**
     * Generic type of parameter.
     * @return generic type
     */
    java.lang.reflect.Type getGenericParameterType();

    /**
     * Gets input constraints.
     * @return constraints where the key is one of {@link Input#MAX} etc. and the value is a string or number,
     * depending on the input constraint.
     * @see Input#MAX
     * @see Input#MIN
     * @see Input#MAX_LENGTH
     * @see Input#MIN_LENGTH
     * @see Input#STEP
     * @see Input#PATTERN
     * @see Input#READONLY
     */
    Map<String, Object> getInputConstraints();
}
