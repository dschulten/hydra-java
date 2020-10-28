/*
 * Copyright (c) 2015. Escalon System-Entwicklung, Dietrich Schulten
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

package de.escalon.hypermedia.spring.uber;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.node.ObjectNode;
import de.escalon.hypermedia.PropertyUtils;
import de.escalon.hypermedia.action.Type;
import de.escalon.hypermedia.affordance.*;
import de.escalon.hypermedia.spring.SpringActionDescriptor;
import de.escalon.hypermedia.spring.SpringActionInputParameter;
import org.springframework.core.MethodParameter;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.RepresentationModel;
import org.springframework.hateoas.CollectionModel;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.RequestMethod;

import java.beans.PropertyDescriptor;
import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;
import java.util.Map.Entry;

public class UberUtils {

    private UberUtils() {

    }

    static final Set<String> FILTER_RESOURCE_SUPPORT = new HashSet<String>(Arrays.asList("class", "links", "id"));
    static final String MODEL_FORMAT = "%s={%s}";


    /**
     * Recursively converts object to nodes of uber data.
     *
     * @param objectNode
     *         to append to
     * @param object
     *         to convert
     */
    public static void toUberData(AbstractUberNode objectNode, Object object) {
        Set<String> filtered = FILTER_RESOURCE_SUPPORT;
        if (object == null) {
            return;
        }

        try {
            // TODO: move all returns to else branch of property descriptor handling
            if (object instanceof EntityModel) {
                EntityModel<?> resource = (EntityModel<?>) object;
                objectNode.addLinks(resource.getLinks());
                toUberData(objectNode, resource.getContent());
                return;
            } else if (object instanceof CollectionModel) {
                CollectionModel<?> resources = (CollectionModel<?>) object;

                // TODO set name using EVO see HypermediaSupportBeanDefinitionRegistrar

                objectNode.addLinks(resources.getLinks());

                Collection<?> content = resources.getContent();
                toUberData(objectNode, content);
                return;
            } else if (object instanceof RepresentationModel) {
                RepresentationModel resource = (RepresentationModel) object;

                objectNode.addLinks(resource.getLinks());

                // wrap object attributes below to avoid endless loop

            } else if (object instanceof Collection) {
                Collection<?> collection = (Collection<?>) object;
                for (Object item : collection) {
                    // TODO name must be repeated for each collection item
                    UberNode itemNode = new UberNode();
                    objectNode.addData(itemNode);
                    toUberData(itemNode, item);
                }
                return;
            }
            if (object instanceof Map) {
                Map<?, ?> map = (Map<?, ?>) object;
                for (Entry<?, ?> entry : map.entrySet()) {
                    String key = entry.getKey()
                            .toString();
                    Object content = entry.getValue();
                    Object value = getContentAsScalarValue(content);
                    UberNode entryNode = new UberNode();
                    objectNode.addData(entryNode);
                    entryNode.setName(key);
                    if (value != null) {
                        entryNode.setValue(value);
                    } else {
                        toUberData(entryNode, content);
                    }
                }
            } else {
                Map<String, PropertyDescriptor> propertyDescriptors = PropertyUtils.getPropertyDescriptors(object);
                for (PropertyDescriptor propertyDescriptor : propertyDescriptors.values()) {
                    String name = propertyDescriptor.getName();
                    if (filtered.contains(name)) {
                        continue;
                    }
                    UberNode propertyNode = new UberNode();
                    Object content = propertyDescriptor.getReadMethod()
                            .invoke(object);

                    if (isEmptyCollectionOrMap(content, propertyDescriptor.getPropertyType())) {
                        continue;
                    }

                    Object value = getContentAsScalarValue(content);
                    propertyNode.setName(name);
                    objectNode.addData(propertyNode);
                    if (value != null) {
                        // for each scalar property of a simple bean, add valuepair nodes to data
                        propertyNode.setValue(value);
                    } else {
                        toUberData(propertyNode, content);
                    }
                }

                Field[] fields = object.getClass()
                        .getFields();
                for (Field field : fields) {
                    String name = field.getName();
                    if (!propertyDescriptors.containsKey(name)) {
                        Object content = field.get(object);
                        Class<?> type = field.getType();
                        if (isEmptyCollectionOrMap(content, type)) {
                            continue;
                        }
                        UberNode propertyNode = new UberNode();

                        Object value = getContentAsScalarValue(content);
                        propertyNode.setName(name);
                        objectNode.addData(propertyNode);
                        if (value != null) {
                            // for each scalar property of a simple bean, add valuepair nodes to data
                            propertyNode.setValue(value);
                        } else {
                            toUberData(propertyNode, content);
                        }

                    }
                }
            }
        } catch (Exception ex) {
            throw new RuntimeException("failed to transform object " + object, ex);
        }
    }

    private static boolean isEmptyCollectionOrMap(Object content, Class<?> type) {
        if (Collection.class.isAssignableFrom(type)) {
            if (content == null) {
                return true;
            } else {
                if (((List) content).isEmpty()) {
                    return true;
                }
            }
        } else if (Map.class.isAssignableFrom(type)) {
            if (content == null) {
                return true;
            } else {
                if (((List) content).isEmpty()) {
                    return true;
                }
            }
        }
        return false;
    }


    private static Object getContentAsScalarValue(Object content) {
        final Object value;
        if (content == null) {
            value = UberNode.NULL_VALUE;
        } else if (DataType.isSingleValueType(content.getClass())) {
            value = content.toString();
        } else {
            value = null;
        }
        return value;
    }

    /**
     * Converts single link to uber node.
     *
     * @param href
     *         to use
     * @param actionDescriptor
     *         to use for action and model, never null
     * @param rels
     *         of the link
     * @return uber link
     */
    public static UberNode toUberLink(String href, ActionDescriptor actionDescriptor, String... rels) {
        return toUberLink(href, actionDescriptor, Arrays.asList(rels));
    }

    /**
     * Converts single link to uber node.
     *
     * @param href
     *         to use
     * @param actionDescriptor
     *         to use for action and model, never null
     * @param rels
     *         of the link
     * @return uber link
     */
    public static UberNode toUberLink(String href, ActionDescriptor actionDescriptor, List<String> rels) {
        Assert.notNull(actionDescriptor, "actionDescriptor must not be null");
        UberNode uberLink = new UberNode();
        uberLink.setRel(rels);
        PartialUriTemplateComponents partialUriTemplateComponents = new PartialUriTemplate(href).expand(Collections.emptyMap());
        uberLink.setUrl(partialUriTemplateComponents.toString());
        uberLink.setTemplated(partialUriTemplateComponents.hasVariables() ? Boolean.TRUE : null);
        uberLink.setModel(getModelProperty(href, actionDescriptor));
        RequestMethod requestMethod = RequestMethod.valueOf(actionDescriptor.getHttpMethod());
        uberLink.setAction(UberAction.forRequestMethod(requestMethod));
        return uberLink;
    }

    private static String getModelProperty(String href, ActionDescriptor actionDescriptor) {

        RequestMethod httpMethod = RequestMethod.valueOf(actionDescriptor.getHttpMethod());
        StringBuffer model = new StringBuffer();

        switch (httpMethod) {
            case POST:
            case PUT:
            case PATCH: {
                List<UberField> uberFields = new ArrayList<UberField>();
                recurseBeanCreationParams(uberFields, actionDescriptor.getRequestBody()
                        .getParameterType(), actionDescriptor, actionDescriptor.getRequestBody(), actionDescriptor
                        .getRequestBody()
                        .getValue(), "", Collections.<String>emptySet());
                for (UberField uberField : uberFields) {
                    if (model.length() > 0) {
                        model.append("&");
                    }
                    model.append(String.format(MODEL_FORMAT, uberField.getName(), uberField.getName()));
                }
                break;
            }
            default:

        }
        return model.length() == 0 ? null : model.toString();
    }


//    private List<SirenAction> toUberActions(List<Link> links) {
//        List<SirenAction> ret = new ArrayList<SirenAction>();
//        for (Link link : links) {
//            if (link instanceof Affordance) {
//                Affordance affordance = (Affordance) link;
//                List<ActionDescriptor> actionDescriptors = affordance.getActionDescriptors();
//                for (ActionDescriptor actionDescriptor : actionDescriptors) {
//                    List<SirenField> fields = toUberFields(actionDescriptor);
//                    // TODO integrate getActions and this method so we do not need this check:
//                    // only templated affordances or non-get affordances are actions
//                    if (!"GET".equals(actionDescriptor.getHttpMethod()) || affordance.isTemplated()) {
//                        String href;
//                        if (affordance.isTemplated()) {
//                            href = affordance.getUriTemplateComponents()
//                                    .getBaseUri();
//                        } else {
//                            href = affordance.getHref();
//                        }
//
//
//                        SirenAction sirenAction = new SirenAction(null, actionDescriptor.getActionName(), null,
//                                actionDescriptor.getHttpMethod(), href, requestMediaType, fields);
//                        ret.add(sirenAction);
//                    }
//                }
//            } else if (link.isTemplated()) {
//                List<SirenField> fields = new ArrayList<SirenField>();
//                List<TemplateVariable> variables = link.getVariables();
//                boolean queryOnly = false;
//                for (TemplateVariable variable : variables) {
//                    queryOnly = isQueryParam(variable);
//                    if (!queryOnly) {
//                        break;
//                    }
//                    fields.add(new SirenField(variable.getName(), "text", (String) null, variable.getDescription(),
//                            null));
//                }
//                // no support for non-query fields in siren
//                if (queryOnly) {
//                    String baseUri = new UriTemplate(link.getHref()).expand()
//                            .toASCIIString();
//                    SirenAction sirenAction = new SirenAction(null, null, null, "GET",
//                            baseUri, null, fields);
//                    ret.add(sirenAction);
//                }
//            }
//        }
//        return ret;
//    }

//    private List<SirenField> toUberFields(ActionDescriptor actionDescriptor) {
//        List<SirenField> ret = new ArrayList<SirenField>();
//        if (actionDescriptor.hasRequestBody()) {
//            recurseBeanCreationParams(ret, actionDescriptor.getRequestBody()
//                    .getParameterType(), actionDescriptor, actionDescriptor.getRequestBody(), actionDescriptor
//                    .getRequestBody()
//                    .getValue(), "", Collections.<String>emptySet());
//        }
////        } else {
////            Collection<String> paramNames = actionDescriptor.getRequestParamNames();
////            for (String paramName : paramNames) {
////                ActionInputParameter inputParameter = actionDescriptor.getActionInputParameter(paramName);
////                Object[] possibleValues = inputParameter.getPossibleValues(actionDescriptor);
////
////                ret.add(createSirenField(paramName, inputParameter.getValueFormatted(), inputParameter,
////                        possibleValues));
////            }
////        }
//        return ret;
//    }
//

    /**
     * Renders input fields for bean properties of bean to add or update or patch.
     *
     * @param uberFields
     *         to add to
     * @param beanType
     *         to render
     * @param annotatedParameters
     *         which describes the method
     * @param annotatedParameter
     *         which requires the bean
     * @param currentCallValue
     *         sample call value
     */
    private static void recurseBeanCreationParams(List<UberField> uberFields, Class<?> beanType,
                                                  ActionDescriptor annotatedParameters,
                                                  ActionInputParameter annotatedParameter, Object currentCallValue,
                                                  String parentParamName, Set<String> knownFields) {
        // TODO collection, map and object node creation are only describable by an annotation, not via type reflection
        if (ObjectNode.class.isAssignableFrom(beanType) || Map.class.isAssignableFrom(beanType)
                || Collection.class.isAssignableFrom(beanType) || beanType.isArray()) {
            return; // use @Input(include) to list parameter names, at least? Or mix with hdiv's form builder?
        }
        try {
            Constructor[] constructors = beanType.getConstructors();
            // find default ctor
            Constructor constructor = PropertyUtils.findDefaultCtor(constructors);
            // find ctor with JsonCreator ann
            if (constructor == null) {
                constructor = PropertyUtils.findJsonCreator(constructors, JsonCreator.class);
            }
            Assert.notNull(constructor, "no default constructor or JsonCreator found for type " + beanType
                    .getName());
            int parameterCount = constructor.getParameterTypes().length;

            if (parameterCount > 0) {
                Annotation[][] annotationsOnParameters = constructor.getParameterAnnotations();

                Class[] parameters = constructor.getParameterTypes();
                int paramIndex = 0;
                for (Annotation[] annotationsOnParameter : annotationsOnParameters) {
                    for (Annotation annotation : annotationsOnParameter) {
                        if (JsonProperty.class == annotation.annotationType()) {
                            JsonProperty jsonProperty = (JsonProperty) annotation;

                            // TODO use required attribute of JsonProperty for required fields
                            String paramName = jsonProperty.value();
                            Class parameterType = parameters[paramIndex];
                            Object propertyValue = PropertyUtils.getPropertyOrFieldValue(currentCallValue,
                                    paramName);
                            MethodParameter methodParameter = new MethodParameter(constructor, paramIndex);

                            addUberFieldsForMethodParameter(uberFields, methodParameter, annotatedParameter,
                                    annotatedParameters,
                                    parentParamName, paramName, parameterType, propertyValue,
                                    knownFields);
                            paramIndex++; // increase for each @JsonProperty
                        }
                    }
                }
                Assert.isTrue(parameters.length == paramIndex,
                        "not all constructor arguments of @JsonCreator " + constructor.getName() +
                                " are annotated with @JsonProperty");
            }

            Set<String> knownConstructorFields = new HashSet<String>(uberFields.size());
            for (UberField sirenField : uberFields) {
                knownConstructorFields.add(sirenField.getName());
            }

            // TODO support Option provider by other method args?
            Map<String, PropertyDescriptor> propertyDescriptors = PropertyUtils.getPropertyDescriptors(beanType);

            // add input field for every setter
            for (PropertyDescriptor propertyDescriptor : propertyDescriptors.values()) {
                final Method writeMethod = propertyDescriptor.getWriteMethod();
                String propertyName = propertyDescriptor.getName();

                if (writeMethod == null || knownFields.contains(parentParamName + propertyName)) {
                    continue;
                }
                final Class<?> propertyType = propertyDescriptor.getPropertyType();

                Object propertyValue = PropertyUtils.getPropertyOrFieldValue(currentCallValue, propertyName);
                MethodParameter methodParameter = new MethodParameter(propertyDescriptor.getWriteMethod(), 0);

                addUberFieldsForMethodParameter(uberFields, methodParameter, annotatedParameter,
                        annotatedParameters,
                        parentParamName, propertyName, propertyType, propertyValue, knownConstructorFields);
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to write input fields for constructor", e);
        }
    }

    public static List<ActionDescriptor> getActionDescriptors(Link link) {
        List<ActionDescriptor> actionDescriptors;
        if (link instanceof Affordance) {
            actionDescriptors = ((Affordance) link).getActionDescriptors();
        } else {
            SpringActionDescriptor actionDescriptor = new SpringActionDescriptor("get", RequestMethod.GET
                    .name());
            PartialUriTemplate partialUriTemplate = new PartialUriTemplate(link.getHref());
            PartialUriTemplateComponents parts = partialUriTemplate.asComponents();
            actionDescriptors = Arrays.asList((ActionDescriptor) actionDescriptor);
        }
        return actionDescriptors;
    }

    public static List<String> getRels(Link link) {
        List<String> rels;
        if (link instanceof Affordance) {
            rels = ((Affordance) link).getRels();
        } else {
            rels = Arrays.asList(link.getRel().value());
        }
        return rels;
    }

    private static void addUberFieldsForMethodParameter(List<UberField> fields, MethodParameter
            methodParameter, ActionInputParameter annotatedParameter, ActionDescriptor annotatedParameters, String
                                                                parentParamName, String paramName, Class
                                                                parameterType, Object propertyValue, Set<String>
                                                                knownFields) {
        if (DataType.isSingleValueType(parameterType)
                || DataType.isArrayOrCollection(parameterType)) {

            if (annotatedParameter.isIncluded(paramName) && !knownFields.contains(parentParamName + paramName)) {

                ActionInputParameter constructorParamInputParameter =
                        new SpringActionInputParameter(methodParameter, propertyValue);

                final Object[] possibleValues =
                        annotatedParameter.getPossibleValues(methodParameter, annotatedParameters);

                // dot-separated property path as field name
                UberField field = createUberField(parentParamName + paramName,
                        propertyValue, constructorParamInputParameter, possibleValues);
                fields.add(field);
            }
        } else {
            Object callValueBean;
            if (propertyValue instanceof EntityModel) {
                callValueBean = ((EntityModel) propertyValue).getContent();
            } else {
                callValueBean = propertyValue;
            }
            recurseBeanCreationParams(fields, parameterType, annotatedParameters,
                    annotatedParameter,
                    callValueBean, paramName + ".", knownFields);
        }
    }

    private static UberField createUberField(String paramName, Object propertyValue,
                                             ActionInputParameter inputParameter, Object[] possibleValues) {
        UberField field;
//        if (possibleValues.length == 0) {
        String propertyValueAsString = propertyValue == null ? null : propertyValue
                .toString();
        Type htmlInputFieldType = inputParameter.getHtmlInputFieldType();
        // TODO: null -> array or bean parameter without possible values
        String type = htmlInputFieldType == null ? "text" :
                htmlInputFieldType
                        .name()
                        .toLowerCase();
        field = new UberField(paramName,
                propertyValueAsString);
//        } else {
//            List<SirenFieldValue> sirenPossibleValues = new ArrayList<SirenFieldValue>();
//            String type;
//            if (inputParameter.isArrayOrCollection()) {
//                type = "checkbox";
//                for (Object possibleValue : possibleValues) {
//                    boolean selected = ObjectUtils.containsElement(
//                            inputParameter.getValues(),
//                            possibleValue);
//                    // TODO have more useful value title
//                    sirenPossibleValues.add(new SirenFieldValue(possibleValue.toString(), possibleValue, selected));
//                }
//            } else {
//                type = "radio";
//                for (Object possibleValue : possibleValues) {
//                    boolean selected = possibleValue.equals(propertyValue);
//                    sirenPossibleValues.add(new SirenFieldValue(possibleValue.toString(), possibleValue, selected));
//                }
//            }
//            field = new UberField(paramName,
//                    sirenPossibleValues);
//    }

        return field;
    }

}
