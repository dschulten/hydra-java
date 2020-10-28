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

import de.escalon.hypermedia.PropertyUtils;
import de.escalon.hypermedia.action.Cardinality;
import de.escalon.hypermedia.action.Input;
import de.escalon.hypermedia.affordance.ActionDescriptor;
import de.escalon.hypermedia.affordance.ActionInputParameter;
import de.escalon.hypermedia.affordance.Affordance;
import de.escalon.hypermedia.affordance.DataType;
import de.escalon.hypermedia.affordance.PartialUriTemplateComponents;
import de.escalon.hypermedia.affordance.TypedResource;
import de.escalon.hypermedia.hydra.mapping.Expose;
import de.escalon.hypermedia.hydra.serialize.JacksonHydraSerializer;
import de.escalon.hypermedia.hydra.serialize.JsonLdKeywords;
import de.escalon.hypermedia.hydra.serialize.LdContext;
import de.escalon.hypermedia.hydra.serialize.LdContextFactory;
import de.escalon.hypermedia.spring.SpringActionInputParameter;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.MethodParameter;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.hateoas.IanaLinkRelations;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.LinkRelation;
import org.springframework.hateoas.Links;
import org.springframework.util.Assert;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

/**
 * Serializer to convert Link to json-ld representation. Created by dschulten on 19.09.2014.
 */
public class LinkListSerializer extends StdSerializer<Links> {

    Logger LOG = LoggerFactory.getLogger(LinkListSerializer.class);

    private static final String IANA_REL_PREFIX = "urn:iana:link-relations:";


    public LinkListSerializer() {
        super(List.class, false);
    }

    @Override
    public void serialize(Links links, JsonGenerator jgen,
                          SerializerProvider serializerProvider) throws IOException {

        try {
            Collection<Link> simpleLinks = new ArrayList<Link>();
            Collection<Affordance> affordances = new ArrayList<Affordance>();
            Collection<Link> templatedLinks = new ArrayList<Link>();
            Collection<Affordance> collectionAffordances = new ArrayList<Affordance>();
            Link selfRel = null;
            for (Link link : links) {
                if (link instanceof Affordance) {
                    final Affordance affordance = (Affordance) link;
                    final List<ActionDescriptor> actionDescriptors = affordance.getActionDescriptors();
                    if (!actionDescriptors.isEmpty()) {
                        // TODO: consider to use Link href for template even if it is not compatible
                        if (affordance.getUriTemplateComponents()
                                .hasVariables()) {
                            // TODO resolve rel against context
                            if ("hydra:search".equals(affordance.getRel().value())
                                    || Cardinality.SINGLE == affordance
                                    .getCardinality()) {
                                templatedLinks.add(affordance);
                            } else {
                                collectionAffordances.add(affordance);
                            }
                        } else {
                            // if all required variables are satisfied, the url can be used as identifier
                            // by stripping optional variables
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
                if ("self".equals(link.getRel().value())) {
                    selfRel = link;
                }
            }

            for (Link templatedLink : templatedLinks) {
                // templated affordance might turn out to have all variables satisfied or
                // only optional unsatisfied variables
                ActionDescriptor actionDescriptorForHttpGet = getActionDescriptorForHttpGet(templatedLink);
                // TODO handle rev here
                LinkRelation rel = templatedLink.getRel();
                writeIriTemplate(rel.value(), templatedLink.getHref(), templatedLink.getVariableNames(),
                        actionDescriptorForHttpGet, jgen);
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
                    PartialUriTemplateComponents templateComponents =
                            collectionAffordance.getUriTemplateComponents();
                    if (!collectionAffordance.isBaseUriTemplated() &&
                            !collectionAffordance.hasUnsatisfiedRequiredVariables()) {
                        String collectionUri = templateComponents.getBaseUri()
                                + templateComponents.getQueryHead();
                        jgen.writeStringField(JsonLdKeywords.AT_ID, collectionUri);
                    }
                    if (templateComponents.hasVariables()) {
                        ActionDescriptor actionDescriptorForHttpGet = getActionDescriptorForHttpGet
                                (collectionAffordance);
                        writeIriTemplate("hydra:search", templateComponents.toString(),
                                templateComponents.getVariableNames(), actionDescriptorForHttpGet,
                                jgen);
                    }
                    jgen.writeObjectFieldStart("hydra:manages");
                    // do we have a collection holder which is not owner of the affordance?
                    TypedResource collectionHolder = collectionAffordance.getCollectionHolder();
                    if (collectionAffordance.getRev() != null) {
                        jgen.writeStringField("hydra:property", collectionAffordance.getRev());
                        if (collectionHolder != null) {
                            // can't use writeObjectField, it won't inherit the context stack
                            writeCollectionHolder("hydra:object", collectionHolder, jgen);
                        } else if (selfRel != null) {
                            jgen.writeStringField("hydra:object", selfRel.getHref());
                        }
                    } else if (collectionAffordance.getRel() != null) {
                        jgen.writeStringField("hydra:property", collectionAffordance.getRel().value());
                        if (collectionHolder != null) {
                            // can't use writeObjectField, it won't inherit the context stack
                            writeCollectionHolder("hydra:subject", collectionHolder, jgen);
                        } else if (selfRel != null) {
                            jgen.writeStringField("hydra:subject", selfRel.getHref());
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
                final LinkRelation rel = affordance.getRel();
                List<ActionDescriptor> actionDescriptors = affordance.getActionDescriptors();

                if (!actionDescriptors.isEmpty()) {
                    if (!IanaLinkRelations.SELF.equals(rel)) {
                        jgen.writeObjectFieldStart(rel.value()); // begin rel
                    }
                    jgen.writeStringField(JsonLdKeywords.AT_ID, affordance.getHref());
                    jgen.writeArrayFieldStart("hydra:operation");
                }


                writeActionDescriptors(jgen, currentVocab, actionDescriptors);

                if (!actionDescriptors.isEmpty()) {
                    jgen.writeEndArray(); // end hydra:operation

                    if (!IanaLinkRelations.SELF.equals(rel)) {
                        jgen.writeEndObject(); // end rel
                    }
                }
            }

            for (Link simpleLink : simpleLinks) {
                final LinkRelation rel = simpleLink.getRel();
                if (IanaLinkRelations.SELF.isSameAs(rel)) {
                    jgen.writeStringField("@id", simpleLink.getHref());
                } else {
                    String linkAttributeName = IanaLinkRelations.isIanaRel(rel) ? IANA_REL_PREFIX + rel.value() : rel.value();
                    jgen.writeObjectFieldStart(linkAttributeName);
                    jgen.writeStringField("@id", simpleLink.getHref());
                    jgen.writeEndObject();
                }
            }
        } catch (IntrospectionException e) {
            throw new RuntimeException(e);
        }
    }

    private void writeIriTemplate(String rel, String href, List<String> variableNames, ActionDescriptor
            actionDescriptorForHttpGet,
                                  JsonGenerator jgen) throws IOException {
        jgen.writeObjectFieldStart(rel);

        jgen.writeStringField("@type", "hydra:IriTemplate");
        jgen.writeStringField("hydra:template", href);
        jgen.writeArrayFieldStart("hydra:mapping");
        writeHydraVariableMapping(jgen, actionDescriptorForHttpGet, variableNames);
        jgen.writeEndArray();

        jgen.writeEndObject();
    }

    private void writeCollectionHolder(String fieldName, TypedResource collectionHolder, JsonGenerator jgen) throws
            IOException {
        jgen.writeObjectFieldStart(fieldName);
        String identifyingUri = collectionHolder.getIdentifyingUri();
        if (identifyingUri != null) {
            jgen.writeStringField(JsonLdKeywords.AT_ID, identifyingUri);
        }
        jgen.writeStringField(JsonLdKeywords.AT_TYPE, collectionHolder.getSemanticType());
        jgen.writeEndObject();
    }

    @Nullable
    private ActionDescriptor getActionDescriptorForHttpGet(Link templatedAffordance) {
        if (!(templatedAffordance instanceof Affordance)) {
            return null;
        }
        final List<ActionDescriptor> actionDescriptors = ((Affordance) templatedAffordance).getActionDescriptors();
        ActionDescriptor actionDescriptorGet = null;
        for (ActionDescriptor actionDescriptor : actionDescriptors) {
            String httpMethod = actionDescriptor.getHttpMethod();
            if ("GET".equalsIgnoreCase(httpMethod)) {
                actionDescriptorGet = actionDescriptor;
            }
        }
        return actionDescriptorGet;
    }


    private void writeActionDescriptors(JsonGenerator jgen, String currentVocab, List<ActionDescriptor>
            actionDescriptors) throws IOException, IntrospectionException {
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
                // TODO check need for allRootParameters and requestBodyInputParameter here:
                recurseSupportedProperties(jgen, currentVocab, clazz, actionDescriptor,
                        requestBodyInputParameter, requestBodyInputParameter.getValue(), "");
                jgen.writeEndArray(); // end hydra:supportedProperty

                jgen.writeEndObject(); // end hydra:expects
            }

            jgen.writeEndObject(); // end hydra:Operation
        }
    }

    /**
     * Writes bean description recursively.
     *
     * @param jgen              to write to
     * @param currentVocab      in context
     * @param valueType         class of value
     * @param allRootParameters of the method that receives the request body
     * @param rootParameter     the request body
     * @param currentCallValue  the value at the current recursion level
     * @param propertyPath      of the current recursion level
     * @throws IntrospectionException
     * @throws IOException
     */
    private void recurseSupportedProperties(JsonGenerator jgen, String currentVocab, Class<?>
            valueType, ActionDescriptor allRootParameters,
                                            ActionInputParameter rootParameter, Object currentCallValue,
                                            String propertyPath)
            throws IntrospectionException,
            IOException {


        Map<String, ActionInputParameter> properties = new HashMap<String, ActionInputParameter>();

        // collect supported properties from ctor
        Constructor[] constructors = valueType.getConstructors();
        // find default ctor
        Constructor constructor = PropertyUtils.findDefaultCtor(constructors);
        // find ctor with JsonCreator ann
        if (constructor == null) {
            constructor = PropertyUtils.findJsonCreator(constructors, JsonCreator.class);
        }
        if (constructor == null) {
            // TODO this can be a generic collection, find a way to describe it
            LOG.warn("can't describe supported properties, no default constructor or JsonCreator found for type " + valueType
                    .getName());
            return;
        }

        int parameterCount = constructor.getParameterTypes().length;
        if (parameterCount > 0) {
            Annotation[][] annotationsOnParameters = constructor.getParameterAnnotations();

            Class[] parameters = constructor.getParameterTypes();
            int paramIndex = 0;
            for (Annotation[] annotationsOnParameter : annotationsOnParameters) {
                for (Annotation annotation : annotationsOnParameter) {
                    if (JsonProperty.class == annotation.annotationType()) {
                        JsonProperty jsonProperty = (JsonProperty) annotation;
                        // TODO use required attribute of JsonProperty
                        String paramName = jsonProperty.value();

                        Object propertyValue = PropertyUtils.getPropertyOrFieldValue(currentCallValue, paramName);

                        ActionInputParameter constructorParamInputParameter =
                                new SpringActionInputParameter(
                                        new MethodParameter(constructor, paramIndex), propertyValue);

                        // TODO collect ctor params, setter params and process
                        // TODO then handle single, collection and bean for both
                        properties.put(paramName, constructorParamInputParameter);
                        paramIndex++; // increase for each @JsonProperty
                    }
                }
            }
            Assert.isTrue(parameters.length == paramIndex,
                    "not all constructor arguments of @JsonCreator " + constructor.getName() +
                            " are annotated with @JsonProperty");
        }

        // collect supported properties from setters

        // TODO support Option provider by other method args?
        final BeanInfo beanInfo = Introspector.getBeanInfo(valueType);
        final PropertyDescriptor[] propertyDescriptors = beanInfo.getPropertyDescriptors();
        // TODO collection and map
        // TODO distinguish which properties should be printed as supported - now just setters
        for (PropertyDescriptor propertyDescriptor : propertyDescriptors) {
            final Method writeMethod = propertyDescriptor.getWriteMethod();
            if (writeMethod == null) {
                continue;
            }
            // TODO: the property name must be a valid URI - need to check context for terms?
            String propertyName = getWritableExposedPropertyOrPropertyName(propertyDescriptor);

            Object propertyValue = PropertyUtils.getPropertyOrFieldValue(currentCallValue, propertyDescriptor
                    .getName());

            MethodParameter methodParameter = new MethodParameter(propertyDescriptor.getWriteMethod(), 0);
            ActionInputParameter propertySetterInputParameter = new SpringActionInputParameter(
                    methodParameter, propertyValue);

            properties.put(propertyName, propertySetterInputParameter);
        }

        // write all supported properties
        // TODO we are using the annotatedParameter.parameterName but should use the key of properties here:
        for (ActionInputParameter annotatedParameter : properties.values()) {
            String nextPropertyPathLevel = propertyPath.isEmpty() ? annotatedParameter.getParameterName() :
                    propertyPath + '.' + annotatedParameter.getParameterName();
            Class<?> parameterType = annotatedParameter.getParameterType();
            if (DataType.isSingleValueType(parameterType)) {

                final Object[] possiblePropertyValues = rootParameter.getPossibleValues(allRootParameters);

                if (rootParameter.isIncluded(nextPropertyPathLevel) && !rootParameter.isExcluded
                        (nextPropertyPathLevel)) {
                    writeSupportedProperty(jgen, currentVocab, annotatedParameter,
                            annotatedParameter.getParameterName(), possiblePropertyValues);
                }
                // TODO collections?
                //                        } else if (DataType.isArrayOrCollection(parameterType)) {
                //                            Object[] callValues = rootParameter.getValues();
                //                            int items = callValues.length;
                //                            for (int i = 0; i < items; i++) {
                //                                Object value;
                //                                if (i < callValues.length) {
                //                                    value = callValues[i];
                //                                } else {
                //                                    value = null;
                //                                }
                //                                recurseSupportedProperties(jgen, currentVocab, rootParameter
                // .getParameterType(),
                //                                        allRootParameters, rootParameter, value);
                //                            }
            } else {
                jgen.writeStartObject();
                jgen.writeStringField("hydra:property", annotatedParameter.getParameterName());
                // TODO: is the property required -> for bean props we need the Access annotation to express that

                Expose expose = AnnotationUtils.getAnnotation(parameterType, Expose.class);
                String subClass = null;
                if (expose != null) {
                    subClass = expose.value();
                } else {
                    if (List.class.isAssignableFrom(parameterType)) {
                        Type genericParameterType = annotatedParameter.getGenericParameterType();
                        if (genericParameterType instanceof ParameterizedType) {
                            ParameterizedType parameterizedType = (ParameterizedType) genericParameterType;
                            Type[] actualTypeArguments = parameterizedType.getActualTypeArguments();
                            if (actualTypeArguments.length == 1) {
                                Type actualTypeArgument = actualTypeArguments[0];
                                if (actualTypeArgument instanceof Class) {
                                    parameterType = (Class<?>) actualTypeArgument;
                                    subClass = parameterType.getSimpleName();
                                } else if (actualTypeArgument instanceof ParameterizedType) {
                                    ParameterizedType genericItemType = (ParameterizedType) actualTypeArgument;
                                    Type rawType = genericItemType.getRawType();
                                    if (rawType instanceof Class) {
                                        parameterType = (Class<?>) rawType;
                                        subClass = parameterType.getSimpleName();
                                    }
                                }
                            }
                        }
                        if (subClass != null) {
                            String multipleValueProp = getPropertyOrClassNameInVocab(currentVocab,
                                    "multipleValues",
                                    LdContextFactory.HTTP_SCHEMA_ORG,
                                    "schema:");
                            jgen.writeBooleanField(multipleValueProp, true);
                        }
                    }
                }
                if (subClass == null) {
                    subClass = parameterType.getSimpleName();
                }
                jgen.writeObjectFieldStart(getPropertyOrClassNameInVocab(currentVocab, "rangeIncludes",
                        LdContextFactory.HTTP_SCHEMA_ORG, "schema:"));

                jgen.writeStringField(getPropertyOrClassNameInVocab(currentVocab,
                        "subClassOf",
                        "http://www.w3.org/2000/01/rdf-schema#",
                        "rdfs:"), subClass);

                jgen.writeArrayFieldStart("hydra:supportedProperty");
                // TODO let defaultValue be an filled list, if needed
                Object propertyValue = PropertyUtils.getPropertyOrFieldValue(currentCallValue, annotatedParameter
                        .getParameterName());

                recurseSupportedProperties(jgen, currentVocab, parameterType,
                        allRootParameters,
                        rootParameter, propertyValue, nextPropertyPathLevel);
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
     * @param vocabularyPrefixWithColon to use if the current vocab does not match the given vocabulary to which the name belongs, should end
     *                                  with colon
     * @return property name or class name in the currenct context
     */

    private String getPropertyOrClassNameInVocab(@Nullable String currentVocab, String propertyOrClassName, String
            vocabulary, String vocabularyPrefixWithColon) {
        Assert.notNull(vocabulary, "Vocabulary should be not null");
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
                                        String propertyName, @SuppressWarnings("unused") Object[]
                                                possiblePropertyValues)
            throws IOException {

        jgen.writeStartObject();

        if (actionInputParameter.hasValue() || actionInputParameter.hasInputConstraints()) {
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
//                writeScalarValue(jgen, possibleValue, rootParameter.getParameterType());
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
        //          annotation alongside requestBody to tell which attributes are required or writable, and use
        // Requires over
        //          bean structure, where ctor with least length of args is required and setters are supported
        //          but optional? The bean structure does say what is writable for updates, but not what is required
        // for creation. Right now setters are supportedProperties. For creation we would have to add constructor
        // arguments as supportedProperties.
        //  (/) defaultValue (pre-filled value, e.g. list of selected items for option)
        //  valueName (for iri templates only)
        //  (/) readonlyValue (true for final public field or absence of setter, send fixed value like hidden field?)
        // -> use hydra:readable, hydra:writable
        //  (/) multipleValues
        //  (/) valueMinLength
        //  (/) valueMaxLength
        //  (/) valuePattern
        //  minValue (DateTime support)
        //  maxValue (DateTime support)
        //  (/) stepValue
        final Map<String, Object> inputConstraints = actionInputParameter.getInputConstraints();

        if (actionInputParameter.hasValue()) {
            if (actionInputParameter.isArrayOrCollection()) {
                Object[] callValues = actionInputParameter.getValues();
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

                writeScalarValue(jgen, actionInputParameter.getValue(), actionInputParameter
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

//    private boolean isSelected(Object possibleValue, ActionInputParameter rootParameter) {
//        boolean ret;
//        if (rootParameter.isArrayOrCollection()) {
//            ret = ArrayUtils.contains(rootParameter.getValues(), possibleValue);
//        } else {
//            final Object callValue = rootParameter.getValue();
//            ret = (callValue == null ? false :
//                    callValue.equals(possibleValue));
//        }
//        return ret;
//    }

//    private void writePropertyValueSpecifications(JsonGenerator jgen,
//                                                  ActionDescriptor allRootParameters) throws IOException {
//        // TODO use input constraints
//        for (String pathVariableName : allRootParameters.getPathVariableNames()) {
//            jgen.writeStringField(pathVariableName + "-input", "required");
//        }
//        for (String requestParamName : allRootParameters.getRequestParamNames()) {
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


    private void writeHydraVariableMapping(JsonGenerator jgen, @Nullable ActionDescriptor annotatedParameters,
                                           Collection<String> variableNames) throws IOException {
        if (annotatedParameters != null) {
            for (String variableName : variableNames) {
                // TODO: find also @Input
                ActionInputParameter annotatedParameter = annotatedParameters.getActionInputParameter(variableName);
                // TODO access @Input parameter, too
                // only unsatisfied parameters become hydra variables
                if (annotatedParameter != null && annotatedParameter.getValue() == null) {
                    jgen.writeStartObject();
                    jgen.writeStringField("@type", "hydra:IriTemplateMapping");
                    jgen.writeStringField("hydra:variable", annotatedParameter.getParameterName());
                    jgen.writeBooleanField("hydra:required",
                            annotatedParameter
                                    .isRequired());
                    jgen.writeStringField("hydra:property",
                            getExposedPropertyOrParamName(annotatedParameter));
                    jgen.writeEndObject();
                }
            }
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
