/*
 * Copyright (c) 2014. Escalon System-Entwicklung, Dietrich Schulten
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package de.escalon.hypermedia.spring.de.escalon.hypermedia.spring.jackson;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import de.escalon.hypermedia.DataType;
import de.escalon.hypermedia.hydra.mapping.Expose;
import de.escalon.hypermedia.spring.Affordance;
import de.escalon.hypermedia.spring.action.ActionDescriptor;
import de.escalon.hypermedia.spring.action.ActionInputParameter;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;
import org.springframework.core.convert.Property;
import org.springframework.hateoas.IanaRels;
import org.springframework.hateoas.Link;
import org.springframework.util.Assert;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.*;

/**
 * Serializer to convert Link to json-ld representation.
 * Created by dschulten on 19.09.2014.
 */
public class LinkListSerializer extends StdSerializer<List<Link>> {

    public static final boolean schemaOrgActions = false;

    private static final String IANA_REL_PREFIX = "urn:iana:link-relations:";


    public LinkListSerializer() {
        super(List.class, false);
    }

    @Override
    public void serialize(List<Link> links, JsonGenerator jgen,
                          SerializerProvider provider) throws IOException {

        try {
            Collection<Link> simpleLinks = new ArrayList<Link>();
            Collection<Affordance> actions = new ArrayList<Affordance>();
            Collection<Link> uriTemplates = new ArrayList<Link>();
            Collection<Affordance> uriTemplateActions = new ArrayList<Affordance>();
            for (Link link : links) {
                if (link instanceof Affordance) {
                    final Affordance affordance = (Affordance) link;
                    final ActionDescriptor actionDescriptor = affordance.getActionDescriptor();
                    Assert.notNull(actionDescriptor);
                    if (affordance.isTemplated()) {
                        uriTemplateActions.add(affordance);
                    } else {
                        actions.add(affordance);
                    }
                } else if (link.isTemplated()) {
                    uriTemplates.add(link);
                } else {
                    simpleLinks.add(link);
                }
            }

            for (Affordance uriTemplateAction : uriTemplateActions) {
                // we only have the template, no access to method params
                jgen.writeObjectFieldStart(uriTemplateAction.getRel());

                final ActionDescriptor actionDescriptor = uriTemplateAction.getActionDescriptor();
                jgen.writeStringField("@type", "hydra:IriTemplate");
                jgen.writeStringField("hydra:template", uriTemplateAction.getHref());
                jgen.writeArrayFieldStart("hydra:mapping");
                writeHydraVariableMapping(jgen, actionDescriptor, actionDescriptor.getPathVariableNames());
                writeHydraVariableMapping(jgen, actionDescriptor, actionDescriptor.getRequestParamNames());
                jgen.writeEndArray();

                jgen.writeEndObject();
            }
            for (Link action : uriTemplates) {
                // we only have the template, no access to method params
                jgen.writeObjectFieldStart(action.getRel());

                jgen.writeStringField("@type", "hydra:IriTemplate");
                jgen.writeStringField("hydra:template", action.getHref());

                jgen.writeArrayFieldStart("hydra:mapping");
                writeHydraVariableMapping(jgen, null, action.getVariableNames());
                jgen.writeEndArray();

                jgen.writeEndObject();
            }

            if (!actions.isEmpty()) {
                jgen.writeArrayFieldStart("hydra:operation");
            }
            for (Affordance action : actions) {
                final String rel = action.getRel();

                final ActionDescriptor actionDescriptor = action.getActionDescriptor();

                jgen.writeStartObject(); // begin hydra:operation

                // TODO recognize DeleteOperation by httpMethod
                final String semanticActionType = actionDescriptor.getSemanticActionType();
                if(semanticActionType != null) {
                    jgen.writeStringField("@type", semanticActionType);
                }
                jgen.writeStringField("hydra:method", actionDescriptor.getHttpMethod()
                        .name());

                final ActionInputParameter requestBodyInputParameter = actionDescriptor.getRequestBody();
                if (requestBodyInputParameter != null) {

                    jgen.writeObjectFieldStart("hydra:expects"); // begin hydra:expects

                    final Class<?> clazz = requestBodyInputParameter.getNestedParameterType();
                    final Expose classExpose = clazz.getAnnotation(Expose.class);
                    final String typeName;
                    if (classExpose != null) {
                        typeName = classExpose.value();
                    } else {
                        typeName = requestBodyInputParameter.getNestedParameterType()
                                .getSimpleName();
                    }
                    // TODO see slides of Markus' talk, as mentioned in his response to "Event Api"
//                                jgen.writeStringField("@id", typeName);
                    jgen.writeStringField("@type", "hydra:Class");
                    jgen.writeStringField("hydra:subClassOf", typeName);

                    jgen.writeArrayFieldStart("hydra:supportedProperty"); // begin hydra:supportedProperty
                    // TODO check need for actionDescriptor and requestBodyInputParameter here:
                    recurseSupportedProperties(jgen, clazz, actionDescriptor, requestBodyInputParameter);
                    jgen.writeEndArray(); // end hydra:supportedProperty

                    jgen.writeEndObject(); // end hydra:expects

                }
                jgen.writeEndObject(); // end hydra:Operation
            }
            if (!actions.isEmpty()) {
                jgen.writeEndArray(); // end hydra:operation
            }

            for (Link simpleLink : simpleLinks) {
                final String rel = simpleLink.getRel();
                if (Link.REL_SELF.equals(rel)) {
                    jgen.writeStringField("@id", simpleLink.getHref());
                } else {
                    String linkAttributeName = IanaRels.isIanaRel(rel) ? IANA_REL_PREFIX + rel : rel;
                    jgen.writeObjectFieldStart(linkAttributeName);
                    jgen.writeStringField("@id", simpleLink.getHref());
                    jgen.writeEndObject();
                }
            }
        } catch (IntrospectionException e) {
            throw new RuntimeException(e);
        }
    }

    private void recurseSupportedProperties(JsonGenerator jgen, Class<?> beanType, ActionDescriptor actionDescriptor,
                                            ActionInputParameter actionInputParameter) throws IntrospectionException, IOException {
        // TODO support Option provider by other method args?
        final BeanInfo beanInfo = Introspector.getBeanInfo(beanType);
        final PropertyDescriptor[] propertyDescriptors = beanInfo.getPropertyDescriptors();
        // TODO collection and map

        for (PropertyDescriptor propertyDescriptor : propertyDescriptors) {
            final Method writeMethod = propertyDescriptor.getWriteMethod();
            if (writeMethod == null) {
                continue;
            }
            final Class<?> propertyType = propertyDescriptor.getPropertyType();
            if (DataType.isScalar(propertyType)) {
                // TODO: the property name must be a valid URI - need to check context for terms?
                String propertyName = getWritableExposedPropertyOrPropertyName(propertyDescriptor);

                final Property property = new Property(beanType,
                        propertyDescriptor.getReadMethod(),
                        propertyDescriptor.getWriteMethod(),
                        propertyDescriptor.getName());

                final Object[] possiblePropertyValues =
                        actionInputParameter.getPossibleValues(property, actionDescriptor);

                // TODO recursively make actionInputParameter from every
                // scalar or array beanProperty and handle like below
                // possibleValues, isArrayOrCollection, isNumber, isBoolean

                writeSupportedProperty(jgen, actionInputParameter,
                        propertyName, property, possiblePropertyValues);
            } else {
                recurseSupportedProperties(jgen, propertyType, actionDescriptor, actionInputParameter);
            }
        }
    }


    private void writeSupportedProperty(JsonGenerator jgen, ActionInputParameter actionInputParameter,
                                        String propertyName, Property property,
                                        Object[] possiblePropertyValues) throws IOException {

        jgen.writeStartObject();
        // TODO do we need this:
//        jgen.writeArrayFieldStart("@type");
//        jgen.writeString("hydra:SupportedProperty");

        // TODO detect schema.org in context and make sure we add it for PropertyValueSpec
        // TODO when recursing through bean for context creation
        // TODO comment in for writePossiblePropertyValues (hydra:option)
//        jgen.writeString("PropertyValueSpecification");
//        jgen.writeEndArray();

        jgen.writeStringField("hydra:property", propertyName);

        // TODO comment in for writePossiblePropertyValues (hydra:option):
//        writePossiblePropertyValues(jgen, actionInputParameter, property, possiblePropertyValues);


        jgen.writeEndObject();
    }

    private void writePossiblePropertyValues(JsonGenerator jgen, ActionInputParameter actionInputParameter,
                                             Property property, Object[] possiblePropertyValues) throws IOException {
        // TODO comment in for hydra:option
        jgen.writeArrayFieldStart("hydra:option");
        if (actionInputParameter.isArrayOrCollection()) {
            jgen.writeBooleanField("multipleValues", true);
        }
//        valueRequired (hard to say, using @Access on Event is for all update requests - or make
//           specific request beans for different
//           purposes rather than always passing an instance of e.g. Event?)
//             -> update is a different use case than create - or maybe have an @Requires("eventStatus")
//                annotation alongside requestBody to tell which attributes are required, and use Requires over
//                bean structure, where ctor with least length of args is required and setters are supported
//                but optional?
//        defaultValue (pre-filled value, e.g. list of selected items for option)
//        valueName (for iri templates only)
//        readonlyValue (true for final public field or absence of setter, send fixed value like hidden field?)
//        (/) multipleValues
//        (/) valueMinLength
//        (/) valueMaxLength
//        (/) valuePattern
//        minValue (DateTime support)
//        maxValue (DateTime support)
//        (/) stepValue
        final Map<String, Object> inputConstraints = actionInputParameter.getInputConstraints();

        if (!inputConstraints.isEmpty()) {
            final List<String> keysToAppendValue = Arrays.asList(ActionInputParameter.MAX, ActionInputParameter.MIN,
                    ActionInputParameter.STEP);
            for (String keyToAppendValue : keysToAppendValue) {
                // TODO support min, max for date, datetime, time: using long or String, using minProvider/maxProvider?
                final Object constraint = inputConstraints.get(keyToAppendValue);
                if (constraint != null) {
                    jgen.writeFieldName(keyToAppendValue + "Value");
                    jgen.writeNumber(constraint
                            .toString());
                }
            }


            final List<String> keysToPrependValue = Arrays.asList(ActionInputParameter.MAX_LENGTH,
                    ActionInputParameter.MIN_LENGTH);
            for (String keyToPrependValue : keysToPrependValue) {
                final Object constraint = inputConstraints.get(keyToPrependValue);
                if (constraint != null) {
                    jgen.writeFieldName("value" + StringUtils.capitalize(keyToPrependValue));
                    jgen.writeNumber(constraint
                            .toString());
                }
            }
        }

        for (Object possibleValue : possiblePropertyValues) {
            // TODO: apply "hydra:option" : { "@type": "@vocab"} to context for enums
            writePossibleValue(jgen, possibleValue, property.getType());
        }
        jgen.writeEndArray();
    }


    private void writePossibleValue(JsonGenerator jgen, Object possibleValue,
                                    ActionInputParameter actionInputParameter) throws IOException {
        if (actionInputParameter.isNumber()) {
            jgen.writeNumber(possibleValue.toString());
        } else if (actionInputParameter.isBoolean()) {
            jgen.writeBoolean((Boolean) possibleValue);
        } else {
            jgen.writeString(possibleValue.toString());
        }
    }

    private void writePossibleValue(JsonGenerator jgen, Object possibleValue,
                                    Class<?> valueType) throws IOException {
        if (Number.class.isAssignableFrom(valueType)) {
            jgen.writeNumber(possibleValue.toString());
        } else if (Boolean.class.isAssignableFrom(valueType)) {
            jgen.writeBoolean((Boolean) possibleValue);
        } else if (Enum.class.isAssignableFrom(valueType)) {
            jgen.writeString(((Enum) possibleValue).name());
        } else {
            jgen.writeString(possibleValue.toString());
        }
    }

    private boolean isSelected(Object possibleValue, ActionInputParameter actionInputParameter) {
        boolean ret;
        if (actionInputParameter.isArrayOrCollection()) {
            ret = ArrayUtils.contains(actionInputParameter.getCallValues(), possibleValue);
        } else {
            final Object callValue = actionInputParameter.getCallValue();
            ret = (callValue == null ? false :
                    callValue.equals(possibleValue));
        }
        return ret;
    }

    private void writePropertyValueSpecifications(JsonGenerator jgen,
                                                  ActionDescriptor actionDescriptor) throws IOException {
        // TODO use input constraints
        for (String pathVariableName : actionDescriptor.getPathVariableNames()) {
            jgen.writeStringField(pathVariableName + "-input", "required");
        }
        for (String requestParamName : actionDescriptor.getRequestParamNames()) {
            // TODO could be a list -> tell so using select options, but what about a list
            // of free length, such as ids?
            jgen.writeStringField(requestParamName + "-input", "required");
        }
    }

    private void writeSimpleTarget(JsonGenerator jgen, Link action, Affordance affordance) throws IOException {
        jgen.writeStringField("target", action.getHref());
    }

    private void writeEntryPointTarget(JsonGenerator jgen, Link action, Affordance affordance) throws IOException {
        jgen.writeObjectFieldStart("target");
        jgen.writeStringField("@type", "EntryPoint");
        jgen.writeStringField("urlTemplate", action.getHref());
        jgen.writeStringField("httpMethod", affordance.getHttpMethod()
                .name());
        // TODO encodingType, contentType, application
        jgen.writeEndObject();
    }


    private void writeHydraVariableMapping(JsonGenerator jgen, @Nullable ActionDescriptor actionDescriptor,
                                           Collection<String> variableNames) throws IOException {
        for (String requestParamName : variableNames) {
            jgen.writeStartObject();
            jgen.writeStringField("@type", "hydra:IriTemplateMapping");
            jgen.writeStringField("hydra:variable", requestParamName);
            if (actionDescriptor != null) {
                jgen.writeBooleanField("hydra:required",
                        actionDescriptor.getActionInputParameter(requestParamName)
                                .isRequired());
                jgen.writeStringField("hydra:property",
                        getExposedPropertyOrParamName(actionDescriptor.getActionInputParameter(requestParamName)));
            }
            jgen.writeEndObject();
        }
    }

    /**
     * Gets exposed property or parameter name.
     *
     * @param inputParameter for exposure
     * @return property name
     */
    private String getExposedPropertyOrParamName(ActionInputParameter inputParameter) {
        final Expose expose = inputParameter.getAnnotation(Expose.class);
        String property;
        if (expose != null) {
            property = expose.value();
        } else {
            property = inputParameter.getParameterName();
        }
        return property;
    }

    /**
     * Gets exposed property or parameter name for properties with an appropriate setter (=write) method.
     *
     * @param inputParameter for exposure
     * @return property name
     */
    private String getWritableExposedPropertyOrPropertyName(PropertyDescriptor inputParameter) {

        final Method writeMethod = inputParameter.getWriteMethod();
        final Expose expose = writeMethod
                .getAnnotation(Expose.class);
        String propertyName;
        if (expose != null) {
            propertyName = expose.value();
        } else {
            propertyName = inputParameter.getName();
        }
        return propertyName;
    }

    @Override
    public boolean isUnwrappingSerializer() {
        return true;
    }
}
