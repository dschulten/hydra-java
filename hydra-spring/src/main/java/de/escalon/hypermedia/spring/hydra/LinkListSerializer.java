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

package de.escalon.hypermedia.spring.hydra;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import de.escalon.hypermedia.action.Input;
import de.escalon.hypermedia.affordance.AnnotatedParameter;
import de.escalon.hypermedia.affordance.DataType;
import de.escalon.hypermedia.PropertyUtils;
import de.escalon.hypermedia.action.Cardinality;
import de.escalon.hypermedia.hydra.mapping.Expose;
import de.escalon.hypermedia.hydra.serialize.JacksonHydraSerializer;
import de.escalon.hypermedia.hydra.serialize.JsonLdKeywords;
import de.escalon.hypermedia.hydra.serialize.LdContext;
import de.escalon.hypermedia.hydra.serialize.LdContextFactory;
import de.escalon.hypermedia.affordance.Affordance;
import de.escalon.hypermedia.affordance.ActionDescriptor;
import de.escalon.hypermedia.spring.ActionInputParameter;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;
import org.springframework.core.MethodParameter;
import org.springframework.core.annotation.AnnotationUtils;
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
 * Serializer to convert Link to json-ld representation. Created by dschulten on 19.09.2014.
 */
public class LinkListSerializer extends StdSerializer<List<Link>> {

    private static final String IANA_REL_PREFIX = "urn:iana:link-relations:";


    public LinkListSerializer() {
        super(List.class, false);
    }

    @Override
    public void serialize(List<Link> links, JsonGenerator jgen,
                          SerializerProvider serializerProvider) throws IOException {

        try {
            Collection<Link> simpleLinks = new ArrayList<Link>();
            Collection<Affordance> affordances = new ArrayList<Affordance>();
            Collection<Link> templatedLinks = new ArrayList<Link>();
            Collection<Affordance> templatedAffordances = new ArrayList<Affordance>();
            Collection<Affordance> collectionAffordances = new ArrayList<Affordance>();
            Link selfRel = null;
            for (Link link : links) {
                if (link instanceof Affordance) {
                    final Affordance affordance = (Affordance) link;
                    final List<ActionDescriptor> actionDescriptors = affordance.getActionDescriptors();
                    if (!actionDescriptors.isEmpty()) {
                        if (affordance.isTemplated()) {
                            templatedAffordances.add(affordance);
                        } else {
                            if (!affordance.isSelfRel() && Cardinality.COLLECTION == affordance.getCardinality()) {
                                collectionAffordances.add(affordance);
                            } else {
                                affordances.add(affordance);
                            }
                        }
                    } else {
                        if (affordance.isTemplated()) {
                            templatedLinks.add(affordance);
                        } else {
                            simpleLinks.add(affordance);
                        }
                    }
                } else if (link.isTemplated()) {
                    templatedLinks.add(link);
                } else {
                    simpleLinks.add(link);
                }
                if ("self".equals(link.getRel())) {
                    selfRel = link;
                }
            }

            for (Affordance templatedAffordance : templatedAffordances) {
                jgen.writeObjectFieldStart(templatedAffordance.getRel());

                jgen.writeStringField("@type", "hydra:IriTemplate");
                jgen.writeStringField("hydra:template", templatedAffordance.getHref());
                final List<ActionDescriptor> actionDescriptors = templatedAffordance.getActionDescriptors();
                ActionDescriptor actionDescriptor = actionDescriptors.get(0);
                jgen.writeArrayFieldStart("hydra:mapping");
                writeHydraVariableMapping(jgen, actionDescriptor, actionDescriptor.getPathVariableNames());
                writeHydraVariableMapping(jgen, actionDescriptor, actionDescriptor.getRequestParamNames());
                jgen.writeEndArray();

                jgen.writeEndObject();
            }
            for (Link templatedLink : templatedLinks) {
                // we only have the template, no access to method params
                jgen.writeObjectFieldStart(templatedLink.getRel());

                jgen.writeStringField("@type", "hydra:IriTemplate");
                jgen.writeStringField("hydra:template", templatedLink.getHref());

                jgen.writeArrayFieldStart("hydra:mapping");
                writeHydraVariableMapping(jgen, null, templatedLink.getVariableNames());
                jgen.writeEndArray();

                jgen.writeEndObject();
            }

            @SuppressWarnings("unchecked")
            Deque<LdContext> contextStack = (Deque<LdContext>) serializerProvider.getAttribute(JacksonHydraSerializer
                    .KEY_LD_CONTEXT);
            String currentVocab = (contextStack != null && !contextStack.isEmpty()) ?
                    contextStack.peek().vocab : null;

            // related collections
            if (!collectionAffordances.isEmpty()) {
                jgen.writeArrayFieldStart("hydra:collection");

                for (Affordance collectionAffordance : collectionAffordances) {
                    jgen.writeStartObject();
                    jgen.writeStringField(JsonLdKeywords.AT_TYPE, "hydra:Collection");
                    jgen.writeStringField(JsonLdKeywords.AT_ID, collectionAffordance.getHref());
                    jgen.writeObjectFieldStart("hydra:manages");
                    jgen.writeStringField("hydra:property", collectionAffordance.getRel());
                    // a) in the case of </Alice> :knows /bob the subject must be /Alice
                    // b) in the case of <> orderedItem /latte-1 the subject is an anonymous resource of type Order
                    // c) in the case of </order/1> :seller </store> the object must be the store
                    // 1. where is the information *about* the subject or object: the type or @id
                    // 2. how to decide if the collection manages items for a subject or object?
                    // ad 1.)
                    // ad a) self rel of the Resource that has the collection affordance: looking through link list
                    //       at serialization time is possible
                    // ad b) a candidate is the class given as value of @ExposesResourceFor on the controller
                    //       that defines the method which leads us to believe we have a collection:
                    //       either a GET /orders handler having a collection return type or the POST /orders.
                    //       Works only if the GET or POST has no request mapping path of its own
                    //       an alternative is to use {} without type if @ExposesResourceFor is not present
                    // ad c) like a
                    // ad 2.)
                    // we know the affordance is a collection
                    // * we could pass information into build(rel), like .rel().reverseRel("schema:seller").build()
                    // * we could use build("myReversedTerm") and look at the @context to see if it is reversed,
                    //   if so, we know that the manages block must use object not subject. However weird if the
                    //   reverse term is never really used in the json-ld
                    // * we could have a registry which says if a property is meant to be reversed in a certain context
                    //   and use that to find out if we need subject or object, like :seller ->
                    if (selfRel != null) {
                        // prefer rev over rel, assuming that rev exists to be used by RDF serialization
                        if (collectionAffordance.getRev() != null) {
                            jgen.writeStringField("hydra:object", selfRel.getHref());
                        } else if (collectionAffordance.getRel() != null) {
                            jgen.writeStringField("hydra:subject", selfRel.getHref());
                        }
                    } else {
                        // prefer rev over rel, assuming that rev exists to be used by RDF serialization
                        if (collectionAffordance.getRev() != null) {
                            jgen.writeObjectFieldStart("hydra:object");
                            jgen.writeEndObject();
                        } else if (collectionAffordance.getRel() != null) {
                            jgen.writeObjectFieldStart("hydra:subject");
                            jgen.writeEndObject();
                        }
                    }
                    jgen.writeEndObject(); // end manages


                    List<ActionDescriptor> actionDescriptors = collectionAffordance.getActionDescriptors();
                    if (!actionDescriptors.isEmpty()) {
                        jgen.writeArrayFieldStart("hydra:operation");
                    }
                    writeActionDescriptors(jgen, currentVocab, actionDescriptors);
                    if (!actionDescriptors.isEmpty()) {
                        jgen.writeEndArray(); // end hydra:operation
                    }


                    jgen.writeEndObject(); // end collection
                }
                jgen.writeEndArray();
            }

            for (Affordance affordance : affordances) {
                final String rel = affordance.getRel();
                List<ActionDescriptor> actionDescriptors = affordance.getActionDescriptors();

                if (!actionDescriptors.isEmpty()) {
                    if (!Link.REL_SELF.equals(rel)) {
                        jgen.writeObjectFieldStart(rel); // begin rel
                    }
                    jgen.writeStringField(JsonLdKeywords.AT_ID, affordance.getHref());
                    jgen.writeArrayFieldStart("hydra:operation");
                }


                writeActionDescriptors(jgen, currentVocab, actionDescriptors);

                if (!actionDescriptors.isEmpty()) {
                    jgen.writeEndArray(); // end hydra:operation

                    if (!Link.REL_SELF.equals(rel)) {
                        jgen.writeEndObject(); // end rel
                    }
                }
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

    private void writeActionDescriptors(JsonGenerator jgen, String currentVocab, List<ActionDescriptor> actionDescriptors) throws IOException, IntrospectionException {
        for (ActionDescriptor actionDescriptor : actionDescriptors) {
            jgen.writeStartObject(); // begin a hydra:Operation

            final String semanticActionType = actionDescriptor.getSemanticActionType();
            if (semanticActionType != null) {
                jgen.writeStringField("@type", semanticActionType);
            }
            jgen.writeStringField("hydra:method", actionDescriptor.getHttpMethod());

            final ActionInputParameter requestBodyInputParameter = actionDescriptor.getRequestBody();
            if (requestBodyInputParameter != null) {

                jgen.writeObjectFieldStart("hydra:expects"); // begin hydra:expects

                final Class<?> clazz = requestBodyInputParameter.getParameterType();
                final Expose classExpose = clazz.getAnnotation(Expose.class);
                final String typeName;
                if (classExpose != null) {
                    typeName = classExpose.value();
                } else {
                    typeName = requestBodyInputParameter.getParameterType()
                            .getSimpleName();
                }
                jgen.writeStringField("@type", typeName);

                jgen.writeArrayFieldStart("hydra:supportedProperty"); // begin hydra:supportedProperty
                // TODO check need for actionDescriptor and requestBodyInputParameter here:
                recurseSupportedProperties(jgen, currentVocab, clazz, actionDescriptor,
                        requestBodyInputParameter, requestBodyInputParameter.getCallValue());
                jgen.writeEndArray(); // end hydra:supportedProperty

                jgen.writeEndObject(); // end hydra:expects
            }

            jgen.writeEndObject(); // end hydra:Operation
        }
    }

    private void recurseSupportedProperties(JsonGenerator jgen, String currentVocab, Class<?>
            beanType, ActionDescriptor actionDescriptor,
                                            ActionInputParameter actionInputParameter, Object currentCallValue)
            throws IntrospectionException,
            IOException {
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
            // TODO: the property name must be a valid URI - need to check context for terms?
            String propertyName = getWritableExposedPropertyOrPropertyName(propertyDescriptor);
            if (DataType.isSingleValueType(propertyType)) {

                final Property property = new Property(beanType,
                        propertyDescriptor.getReadMethod(),
                        propertyDescriptor.getWriteMethod(),
                        propertyDescriptor.getName());

                Object propertyValue = PropertyUtils.getPropertyValue(currentCallValue, propertyDescriptor);

                MethodParameter methodParameter = new MethodParameter(propertyDescriptor.getWriteMethod(), 0);
                ActionInputParameter propertySetterInputParameter = new ActionInputParameter(
                        methodParameter, propertyValue);
                final Object[] possiblePropertyValues =
                        actionInputParameter.getPossibleValues(propertyDescriptor.getWriteMethod(), 0, actionDescriptor);

                writeSupportedProperty(jgen, currentVocab, propertySetterInputParameter,
                        propertyName, property, possiblePropertyValues);
            } else {
                jgen.writeStartObject();
                jgen.writeStringField("hydra:property", propertyName);
                // TODO: is the property required -> for bean props we need the Access annotation to express that
                jgen.writeObjectFieldStart(getPropertyOrClassNameInVocab(currentVocab, "rangeIncludes",
                        LdContextFactory.HTTP_SCHEMA_ORG, "schema:"));
                Expose expose = AnnotationUtils.getAnnotation(propertyType, Expose.class);
                String subClass;
                if (expose != null) {
                    subClass = expose.value();
                } else {
                    subClass = propertyType.getSimpleName();
                }
                jgen.writeStringField(getPropertyOrClassNameInVocab(currentVocab, "subClassOf", "http://www.w3.org/2000/01/rdf-schema#", "rdfs:"), subClass);

                jgen.writeArrayFieldStart("hydra:supportedProperty");

                Object propertyValue = PropertyUtils.getPropertyValue(currentCallValue, propertyDescriptor);

                recurseSupportedProperties(jgen, currentVocab, propertyType, actionDescriptor,
                        actionInputParameter, propertyValue);
                jgen.writeEndArray();

                jgen.writeEndObject();
                jgen.writeEndObject();
            }
        }
    }

    /**
     * Gets property or class name in the current context, either without prefix if the current vocab is the given
     * vocabulary, or prefixed otherwise.
     *
     * @param currentVocab              to determine the current vocab
     * @param propertyOrClassName       name to contextualize
     * @param vocabulary                to which the given property belongs
     * @param vocabularyPrefixWithColon to use if the current vocab does not match the given vocabulary to which the
     *                                  name belongs, should end with colon
     * @return property name or class name in the currenct context
     */
    private String getPropertyOrClassNameInVocab(@Nullable String currentVocab, String propertyOrClassName, String
            vocabulary, String vocabularyPrefixWithColon) {
        Assert.notNull(vocabulary);
        String ret;
        if (vocabulary.equals(currentVocab)) {
            ret = propertyOrClassName;
        } else {
            ret = vocabularyPrefixWithColon + propertyOrClassName;
        }
        return ret;
    }


    private void writeSupportedProperty(JsonGenerator jgen, String currentVocab,
                                        ActionInputParameter actionInputParameter,
                                        String propertyName, Property property,
                                        @SuppressWarnings("unused") Object[] possiblePropertyValues)
            throws IOException {

        jgen.writeStartObject();

        if (actionInputParameter.hasCallValue() || actionInputParameter.hasInputConstraints()) {
            // jgen.writeArrayFieldStart("@type");
            // jgen.writeString("hydra:SupportedProperty");

            jgen.writeStringField(JsonLdKeywords.AT_TYPE, getPropertyOrClassNameInVocab(currentVocab,
                    "PropertyValueSpecification", LdContextFactory.HTTP_SCHEMA_ORG, "schema:"));

            //jgen.writeEndArray();
        }
        jgen.writeStringField("hydra:property", propertyName);

        writePossiblePropertyValues(jgen, currentVocab, actionInputParameter, possiblePropertyValues);


        jgen.writeEndObject();
    }

    private void writePossiblePropertyValues(JsonGenerator jgen, String currentVocab, ActionInputParameter
            actionInputParameter, @SuppressWarnings("unused") Object[] possiblePropertyValues) throws IOException {
        // Enable the following to list possible values.
        // Problem: how to express individuals only for certain hydra:options
        // not all hydra:options should be taken as uris, sometimes they might be just literals
        // how to make that clear to the client?
        // maybe we must write them out for options
//        if (possiblePropertyValues.length > 0) {
//            jgen.writeArrayFieldStart("hydra:option");
//
//            for (Object possibleValue : possiblePropertyValues) {
//                // TODO: apply "hydra:option" : { "@type": "@vocab"} to context for enums
//                writeScalarValue(jgen, possibleValue, actionInputParameter.getParameterType());
//            }
//            jgen.writeEndArray();
//        }

        if (actionInputParameter.isArrayOrCollection()) {
            jgen.writeBooleanField(getPropertyOrClassNameInVocab(currentVocab, "multipleValues",
                    LdContextFactory.HTTP_SCHEMA_ORG, "schema:"), true);
        }


        //  valueRequired (hard to say, using @Access on Event is for all update requests - or make
        //     specific request beans for different
        //     purposes rather than always passing an instance of e.g. Event?)
        //       -> update is a different use case than create - or maybe have an @Requires("eventStatus")
        //          annotation alongside requestBody to tell which attributes are required or writable, and use Requires over
        //          bean structure, where ctor with least length of args is required and setters are supported
        //          but optional? The bean structure does say what is writable for updates, but not what is required for creation. Right now setters are supportedProperties. For creation we would have to add constructor arguments as supportedProperties.
        //  (/) defaultValue (pre-filled value, e.g. list of selected items for option)
        //  valueName (for iri templates only)
        //  (/) readonlyValue (true for final public field or absence of setter, send fixed value like hidden field?) -> use hydra:readable, hydra:writable
        //  (/) multipleValues
        //  (/) valueMinLength
        //  (/) valueMaxLength
        //  (/) valuePattern
        //  minValue (DateTime support)
        //  maxValue (DateTime support)
        //  (/) stepValue
        final Map<String, Object> inputConstraints = actionInputParameter.getInputConstraints();

        if (actionInputParameter.hasCallValue()) {
            if (actionInputParameter.isArrayOrCollection()) {
                Object[] callValues = actionInputParameter.getCallValues();
                Class<?> componentType = callValues.getClass()
                        .getComponentType();
                // only write defaultValue for array of scalars
                if (DataType.isSingleValueType(componentType)) {
                    jgen.writeFieldName(getPropertyOrClassNameInVocab(currentVocab, "defaultValue",
                            LdContextFactory.HTTP_SCHEMA_ORG, "schema:"));
                    jgen.writeStartArray();
                    for (Object callValue : callValues) {
                        writeScalarValue(jgen, callValue, componentType);
                    }
                    jgen.writeEndArray();
                }
            } else {
                jgen.writeFieldName(getPropertyOrClassNameInVocab(currentVocab, "defaultValue",
                        LdContextFactory.HTTP_SCHEMA_ORG, "schema:"));

                writeScalarValue(jgen, actionInputParameter.getCallValue(), actionInputParameter
                        .getParameterType());
            }
        }

        if (!inputConstraints.isEmpty()) {
            final List<String> keysToAppendValue = Arrays.asList(Input.MAX, Input.MIN,
                    Input.STEP);
            for (String keyToAppendValue : keysToAppendValue) {
                final Object constraint = inputConstraints.get(keyToAppendValue);
                if (constraint != null) {
                    jgen.writeFieldName(getPropertyOrClassNameInVocab(currentVocab, keyToAppendValue + "Value",
                            LdContextFactory.HTTP_SCHEMA_ORG, "schema:"));
                    jgen.writeNumber(constraint
                            .toString());
                }
            }


            final List<String> keysToPrependValue = Arrays.asList(Input.MAX_LENGTH,
                    Input.MIN_LENGTH, Input.PATTERN);
            for (String keyToPrependValue : keysToPrependValue) {
                final Object constraint = inputConstraints.get(keyToPrependValue);
                if (constraint != null) {
                    jgen.writeFieldName(getPropertyOrClassNameInVocab(currentVocab, "value" + StringUtils.capitalize
                                    (keyToPrependValue),
                            LdContextFactory.HTTP_SCHEMA_ORG, "schema:"));
                    if (Input.PATTERN.equals(keyToPrependValue)) {
                        jgen.writeString(constraint.toString());
                    } else {
                        jgen.writeNumber(constraint
                                .toString());
                    }
                }
            }


        }


    }

    private void writeScalarValue(JsonGenerator jgen, Object possibleValue,
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

//    private boolean isSelected(Object possibleValue, ActionInputParameter actionInputParameter) {
//        boolean ret;
//        if (actionInputParameter.isArrayOrCollection()) {
//            ret = ArrayUtils.contains(actionInputParameter.getCallValues(), possibleValue);
//        } else {
//            final Object callValue = actionInputParameter.getCallValue();
//            ret = (callValue == null ? false :
//                    callValue.equals(possibleValue));
//        }
//        return ret;
//    }

//    private void writePropertyValueSpecifications(JsonGenerator jgen,
//                                                  ActionDescriptor actionDescriptor) throws IOException {
//        // TODO use input constraints
//        for (String pathVariableName : actionDescriptor.getPathVariableNames()) {
//            jgen.writeStringField(pathVariableName + "-input", "required");
//        }
//        for (String requestParamName : actionDescriptor.getRequestParamNames()) {
//            // TODO could be a list -> tell the client using select options, but what about a list
//            // of free length, such as ids?
//            jgen.writeStringField(requestParamName + "-input", "required");
//        }
//    }

//    private void writeSimpleTarget(JsonGenerator jgen, Link action, Affordance affordance) throws IOException {
//        jgen.writeStringField("target", action.getHref());
//    }
//
//    private void writeEntryPointTarget(JsonGenerator jgen, Link action, Affordance affordance) throws IOException {
//        jgen.writeObjectFieldStart("target");
//        jgen.writeStringField("@type", "EntryPoint");
//        jgen.writeStringField("urlTemplate", action.getHref());
//        List<ActionDescriptor> actionDescriptors = affordance.getActionDescriptors();
//        if (actionDescriptors != null && actionDescriptors.getHttpMethod() != null) {
//            jgen.writeStringField("httpMethod", actionDescriptors.getHttpMethod().name());
//        }
//        // TODO encodingType, contentType, application
//        jgen.writeEndObject();
//    }


    private void writeHydraVariableMapping(JsonGenerator jgen, @Nullable ActionDescriptor actionDescriptor,
                                           Collection<String> variableNames) throws IOException {
        for (String requestParamName : variableNames) {
            jgen.writeStartObject();
            jgen.writeStringField("@type", "hydra:IriTemplateMapping");
            jgen.writeStringField("hydra:variable", requestParamName);
            if (actionDescriptor != null) {
                jgen.writeBooleanField("hydra:required",
                        actionDescriptor.getAnnotatedParameter(requestParamName)
                                .isRequired());
                jgen.writeStringField("hydra:property",
                        getExposedPropertyOrParamName(actionDescriptor.getAnnotatedParameter(requestParamName)));
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
    private String getExposedPropertyOrParamName(AnnotatedParameter inputParameter) {
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
