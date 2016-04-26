package de.escalon.hypermedia.spring.siren;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import de.escalon.hypermedia.PropertyUtils;
import de.escalon.hypermedia.affordance.*;
import de.escalon.hypermedia.spring.ActionInputParameter;
import org.jetbrains.annotations.NotNull;
import org.springframework.core.MethodParameter;
import org.springframework.hateoas.*;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;

/**
 * Maps spring-hateoas response data to siren data.
 *
 * Created by Dietrich on 17.04.2016.
 */
public class SirenUtils {

    static final Set<String> FILTER_RESOURCE_SUPPORT = new HashSet<String>(Arrays.asList("class", "links", "id"));

    static Set<String> navigationalRels = new HashSet<String>(Arrays.asList("self", "next", "previous", "prev"));

    public static void setNavigationalRels(Set<String> navigationalRels) {
        SirenUtils.navigationalRels = navigationalRels;
    }

    public static void toSirenEntity(SirenEntityContainer objectNode, Object object, RelProvider relProvider) {
        if (object == null) {
            return;
        }
        try {
            // TODO: move all returns to else branch of property descriptor handling
            if (object instanceof Resource) {
                Resource<?> resource = (Resource<?>) object;
                objectNode.setLinks(SirenUtils.toSirenLinks(
                        getNavigationalLinks(resource.getLinks())));
                objectNode.setEmbeddedLinks(SirenUtils.toSirenEmbeddedLinks(
                        getEmbeddedLinks(resource.getLinks())));
                objectNode.setActions(SirenUtils.toSirenActions(getActions(resource.getLinks())));
                toSirenEntity(objectNode, resource.getContent(), relProvider);
                return;
            } else if (object instanceof Resources) {
                Resources<?> resources = (Resources<?>) object;

                // TODO set name using EVO see HypermediaSupportBeanDefinitionRegistrar
                objectNode.setLinks(SirenUtils.toSirenLinks(getNavigationalLinks(resources.getLinks())));
                Collection<?> content = resources.getContent();
                toSirenEntity(objectNode, content, relProvider);
                objectNode.setActions(SirenUtils.toSirenActions(getActions(resources.getLinks())));
                return;
            } else if (object instanceof ResourceSupport) {
                ResourceSupport resource = (ResourceSupport) object;
                objectNode.setLinks(SirenUtils.toSirenLinks(
                        getNavigationalLinks(resource.getLinks())));
                objectNode.setEmbeddedLinks(SirenUtils.toSirenEmbeddedLinks(
                        getEmbeddedLinks(resource.getLinks())));
                objectNode.setActions(SirenUtils.toSirenActions(
                        getActions(resource.getLinks())));

                // wrap object attributes below to avoid endless loop

            } else if (object instanceof Collection) {
                Collection<?> collection = (Collection<?>) object;
                for (Object item : collection) {
                    // TODO name must be repeated for each collection item
                    // TODO: how create collection item?
                    SirenEmbeddedRepresentation child = new SirenEmbeddedRepresentation();
                    toSirenEntity(child, item, relProvider);
                    objectNode.addSubEntity(child);
                }
                return;
            }
            if (object instanceof Map) {
//                Map<?, ?> map = (Map<?, ?>) object;
//                for (Map.Entry<?, ?> entry : map.entrySet()) {
//                    String key = entry.getKey()
//                            .toString();
//                    Object content = entry.getValue();
//                    Object value = getContentAsScalarValue(content);
//                    // TODO how create map item?
//                    SirenEntity entryNode = new SirenEntity();
//                    objectNode.addData(entryNode);
//                    entryNode.setName(key);
//                    if (value != null) {
//                        entryNode.setValue(value);
//                    } else {
//                        toUberData(entryNode, content);
//                    }
//                }
            } else { // bean or ResourceSupport
                String sirenClass = relProvider.getItemResourceRelFor(object.getClass());
                objectNode.setSirenClasses(Collections.singletonList(sirenClass));
                Map<String, Object> propertiesNode = new HashMap<String, Object>();
                recurseEntities(objectNode, propertiesNode, object, relProvider);
                objectNode.setProperties(propertiesNode);
            }
        } catch (Exception ex) {
            throw new RuntimeException("failed to transform object " + object, ex);
        }
    }

    private static List<Link> getEmbeddedLinks(List<Link> links) {
        List<Link> ret = new ArrayList<Link>();
        for (Link link : links) {
            if (!navigationalRels.contains(link.getRel())) {
                if (link instanceof Affordance) {
                    Affordance affordance = (Affordance) link;
                    List<ActionDescriptor> actionDescriptors = affordance.getActionDescriptors();
                    for (ActionDescriptor actionDescriptor : actionDescriptors) {
                        if ("GET".equals(actionDescriptor.getHttpMethod()) && !affordance.isTemplated()) {
                            ret.add(link);
                        }
                    }
                } else {
                    ret.add(link);
                }
            }
        }
        return ret;
    }

    private static List<Link> getNavigationalLinks(List<Link> links) {
        List<Link> ret = new ArrayList<Link>();
        for (Link link : links) {
            if (navigationalRels.contains(link.getRel())) {
                ret.add(link);
            }
        }
        return ret;
    }

    // TODO: BUG: hydra:search not a template for events collection
    private static List<Link> getActions(List<Link> links) {
        List<Link> ret = new ArrayList<Link>();
        for (Link link : links) {
            if (link instanceof Affordance) {
                Affordance affordance = (Affordance) link;

                List<ActionDescriptor> actionDescriptors = affordance.getActionDescriptors();
                for (ActionDescriptor actionDescriptor : actionDescriptors) {
                    // non-self GET non-GET and templated links are actions
                    if (!("GET".equals(actionDescriptor.getHttpMethod())) || affordance.isTemplated()) {
                        ret.add(link);
                        // add just once for eligible link
                        break;
                    }
                }
            } else {
                if (!navigationalRels.contains(link.getRel()) && link.isTemplated()) {
                    ret.add(link);
                }
            }
        }
        return ret;
    }


    private static void recurseEntities(SirenEntityContainer objectNode, Map<String, Object> propertiesNode,
                                        Object object, RelProvider relProvider) throws InvocationTargetException,
            IllegalAccessException {
        Map<String, PropertyDescriptor> propertyDescriptors = PropertyUtils.getPropertyDescriptors(object);
        for (PropertyDescriptor propertyDescriptor : propertyDescriptors.values()) {
            String name = propertyDescriptor.getName();
            if (FILTER_RESOURCE_SUPPORT.contains(name)) {
                continue;
            }


            Object content = propertyDescriptor.getReadMethod()
                    .invoke(object);
            traverseAttribute(objectNode, propertiesNode, relProvider, name, content);

        }

        Field[] fields = object.getClass()
                .getFields();
        for (Field field : fields) {
            String name = field.getName();
            if (!propertyDescriptors.containsKey(name)) {
                Object content = field.get(object);
                // String docUrl = documentationProvider.getDocumentationUrl(field, content);
                //<a href="http://schema.org/review">http://schema.org/performer</a>
                traverseAttribute(objectNode, propertiesNode, relProvider, name, content);
            }
        }
    }

    private static void traverseAttribute(SirenEntityContainer objectNode, Map<String, Object> propertiesNode,
                                          RelProvider relProvider, String name, Object content) throws
            InvocationTargetException, IllegalAccessException {
        Object value = getContentAsScalarValue(content);

        if (value == NULL_VALUE) {
            return;
        } else if (value != null) {
            // for each scalar property of a simple bean, add valuepair
            propertiesNode.put(name, value);
        } else {
            if (content instanceof ResourceSupport) {
                traverseSubEntity(objectNode, content, relProvider, name);
            } else if (content instanceof Collection) {
                Collection<?> collection = (Collection<?>) content;
                for (Object item : collection) {
                    traverseSubEntity(objectNode, item, relProvider, name);
                }
            } else {
                Map<String, Object> nestedProperties = new HashMap<String, Object>();
                propertiesNode.put(name, nestedProperties);
                recurseEntities(objectNode, nestedProperties, content, relProvider);
            }
        }
    }

    private static void traverseSubEntity(SirenEntityContainer objectNode, Object content, RelProvider relProvider,
                                          String name)
            throws InvocationTargetException, IllegalAccessException {
        Object bean;
        List<Link> links;
        if (content instanceof Resource) {
            bean = ((Resource) content).getContent();
            links = ((Resource) content).getLinks();
        } else if (content instanceof Resources) {
            throw new UnsupportedOperationException("Resources not supported yet");
        } else {
            bean = content;
            links = ((ResourceSupport) content).getLinks();
        }

        String sirenClass = relProvider.getItemResourceRelFor(bean.getClass());

        Map<String, Object> properties = new HashMap<String, Object>();
        SirenEmbeddedRepresentation subEntity = new SirenEmbeddedRepresentation(
                Collections.singletonList(sirenClass), properties, null, toSirenActions(getActions(links)),
                toSirenLinks(getNavigationalLinks(links)), Arrays.asList(name), null);
        //subEntity.setProperties(properties);
        objectNode.addSubEntity(subEntity);
        List<SirenEmbeddedLink> sirenEmbeddedLinks = toSirenEmbeddedLinks(getEmbeddedLinks(links));
        for (SirenEmbeddedLink sirenEmbeddedLink : sirenEmbeddedLinks) {
            subEntity.addSubEntity(sirenEmbeddedLink);
        }
        recurseEntities(subEntity, properties, bean, relProvider);
    }

    private static List<SirenAction> toSirenActions(List<Link> links) {
        List<SirenAction> ret = new ArrayList<SirenAction>();
        for (Link link : links) {
            if (link instanceof Affordance) {
                Affordance affordance = (Affordance) link;
                List<ActionDescriptor> actionDescriptors = affordance.getActionDescriptors();
                for (ActionDescriptor actionDescriptor : actionDescriptors) {
                    List<SirenField> fields = toSirenFields(actionDescriptor);
                    // TODO integration getActions and this method so we do not need this check:
                    // only templated affordances or non-get affordances are actions
                    if(!"GET".equals(actionDescriptor.getHttpMethod()) || affordance.isTemplated()) {
                        String href;
                        if (affordance.isTemplated()) {
                            href = affordance.getUriTemplateComponents()
                                    .getBaseUri();
                        } else {
                            href = affordance.getHref();
                        }

                        SirenAction sirenAction = new SirenAction(null, actionDescriptor.getActionName(), null,
                                actionDescriptor.getHttpMethod(), href, null, fields);
                        ret.add(sirenAction);
                    }
                }
            } else if (link.isTemplated()) {
                List<SirenField> fields = new ArrayList<SirenField>();
                List<TemplateVariable> variables = link.getVariables();
                for (TemplateVariable variable : variables) {
                    fields.add(new SirenField(variable.getName(), "text", (String) null, variable.getDescription(),
                            null));
                }
                String baseUri = new UriTemplate(link.getHref()).expand()
                        .toASCIIString();
                SirenAction sirenAction = new SirenAction(null, null, null, "GET",
                        baseUri, null, fields);
            }
        }
        return ret;
    }

    private static List<SirenField> toSirenFields(ActionDescriptor actionDescriptor) {
        List<SirenField> ret = new ArrayList<SirenField>();
        if (actionDescriptor.hasRequestBody()) {
            recurseBeanCreationParams(ret, actionDescriptor.getRequestBody()
                    .getParameterType(), actionDescriptor, actionDescriptor.getRequestBody(), actionDescriptor
                    .getRequestBody()
                    .getCallValue(), "", Collections.<String>emptySet());
        } else {
            Collection<String> paramNames = actionDescriptor.getRequestParamNames();
            for (String paramName : paramNames) {
                AnnotatedParameter inputParameter = actionDescriptor.getAnnotatedParameter(paramName);
                Object[] possibleValues = inputParameter.getPossibleValues(actionDescriptor);

                ret.add(createSirenField(paramName, inputParameter.getCallValueFormatted(), inputParameter,
                        possibleValues));
            }
        }
        return ret;
    }

    /**
     * Renders input fields for bean properties of bean to add or update or patch.
     *
     * @param sirenFields
     *         to add to
     * @param beanType
     *         to render
     * @param annotatedParameters
     *         which describes the method
     * @param annotatedParameter
     *         which requires the bean
     * @param currentCallValue
     *         sample call value
     * @throws IOException
     */
    private static void recurseBeanCreationParams(List<SirenField> sirenFields, Class<?> beanType,
                                                  AnnotatedParameters annotatedParameters,
                                                  AnnotatedParameter annotatedParameter, Object currentCallValue,
                                                  String parentParamName, Set<String> knownFields) {
        // TODO collection and map
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

                            // TODO use required attribute of JsonProperty
                            String paramName = jsonProperty.value();
                            Class parameterType = parameters[paramIndex];
                            Object propertyValue = PropertyUtils.getPropertyOrFieldValue(currentCallValue,
                                    paramName);
                            MethodParameter methodParameter = new MethodParameter(constructor, paramIndex);

                            addSirenFieldsForMethodParameter(sirenFields, methodParameter, annotatedParameter,
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

            Set<String> knownConstructorFields = new HashSet<String>(sirenFields.size());
            for (SirenField sirenField : sirenFields) {
                knownConstructorFields.add(sirenField.getName());
            }

            // TODO support Option provider by other method args?
            final BeanInfo beanInfo = getBeanInfo(beanType);
            final PropertyDescriptor[] propertyDescriptors = beanInfo.getPropertyDescriptors();

            // add input field for every setter
            for (PropertyDescriptor propertyDescriptor : propertyDescriptors) {
                final Method writeMethod = propertyDescriptor.getWriteMethod();
                String propertyName = propertyDescriptor.getName();

                if (writeMethod == null || knownFields.contains(parentParamName + propertyName)) {
                    continue;
                }
                final Class<?> propertyType = propertyDescriptor.getPropertyType();

                Object propertyValue = PropertyUtils.getPropertyOrFieldValue(currentCallValue, propertyName);
                MethodParameter methodParameter = new MethodParameter(propertyDescriptor.getWriteMethod(), 0);

                addSirenFieldsForMethodParameter(sirenFields, methodParameter, annotatedParameter,
                        annotatedParameters,
                        parentParamName, propertyName, propertyType, propertyValue, knownConstructorFields);

            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to write input fields for constructor", e);
        }

    }

    private static void addSirenFieldsForMethodParameter(List<SirenField> sirenFields, MethodParameter
            methodParameter, AnnotatedParameter annotatedParameter, AnnotatedParameters annotatedParameters, String
                                                                 parentParamName, String paramName, Class
                                                                 parameterType, Object propertyValue, Set<String>
                                                                 knownFields) {
        if (DataType.isSingleValueType(parameterType)
                || DataType.isArrayOrCollection(parameterType)) {

            if (annotatedParameter.isIncluded(paramName) && !knownFields.contains(parentParamName + paramName)) {

                ActionInputParameter constructorParamInputParameter = new ActionInputParameter
                        (methodParameter, propertyValue);

                final Object[] possibleValues =
                        annotatedParameter.getPossibleValues(methodParameter, annotatedParameters);

                // dot-separated property path as field name
                SirenField sirenField = createSirenField(parentParamName + paramName,
                        propertyValue, constructorParamInputParameter, possibleValues);
                sirenFields.add(sirenField);
            }
        } else {
            recurseBeanCreationParams(sirenFields, parameterType, annotatedParameters,
                    annotatedParameter,
                    propertyValue, paramName + ".", knownFields);
        }
    }

    @NotNull
    private static SirenField createSirenField(String paramName, Object propertyValue,
                                               AnnotatedParameter inputParameter, Object[] possibleValues) {
        SirenField sirenField;
        if (possibleValues.length == 0) {
            String propertyValueAsString = propertyValue == null ? null : propertyValue
                    .toString();
            String type = inputParameter.getHtmlInputFieldType()
                    .name()
                    .toLowerCase();
            sirenField = new SirenField(paramName,
                    type,
                    propertyValueAsString, null, null);
        } else {
            List<SirenFieldValue> sirenPossibleValues = new ArrayList<SirenFieldValue>();
            String type;
            if (inputParameter.isArrayOrCollection()) {
                type = "checkbox";
                for (Object possibleValue : possibleValues) {
                    boolean selected = ObjectUtils.containsElement(
                            inputParameter.getCallValues(),
                            possibleValue);
                    // TODO have more useful value title
                    sirenPossibleValues.add(new SirenFieldValue(possibleValue.toString(), possibleValue, selected));
                }
            } else {
                type = "radio";
                for (Object possibleValue : possibleValues) {
                    boolean selected = possibleValue.equals(propertyValue);
                    sirenPossibleValues.add(new SirenFieldValue(possibleValue.toString(), possibleValue, selected));
                }
            }
            sirenField = new SirenField(paramName,
                    type,
                    sirenPossibleValues, null, null);
        }
        return sirenField;
    }

    private static BeanInfo getBeanInfo(Class<?> beanType) {
        try {
            return Introspector.getBeanInfo(beanType);
        } catch (IntrospectionException e) {
            throw new RuntimeException(e);
        }
    }

    private static List<SirenLink> toSirenLinks(List<Link> links) {
        List<SirenLink> ret = new ArrayList<SirenLink>();
        for (Link link : links) {
            if (link instanceof Affordance) {
                ret.add(new SirenLink(null, ((Affordance) link).getRels(), link.getHref(), null, null));
            } else {
                ret.add(new SirenLink(null, Collections.singletonList(link.getRel()), link.getHref(), null, null));
            }
        }
        return ret;
    }

    private static List<SirenEmbeddedLink> toSirenEmbeddedLinks(List<Link> links) {
        List<SirenEmbeddedLink> ret = new ArrayList<SirenEmbeddedLink>();
        for (Link link : links) {
            if (link instanceof Affordance) {
                // TODO: how to determine classes? type of target resource? collection/item?
                ret.add(new SirenEmbeddedLink(null, ((Affordance) link).getRels(), link
                        .getHref(), null, null));
            } else {
                ret.add(new SirenEmbeddedLink(null, Collections.singletonList(link.getRel()), link
                        .getHref(), null, null));
            }
        }
        return ret;
    }


    static class NullValue {

    }

    public static final NullValue NULL_VALUE = new NullValue();

    private static Object getContentAsScalarValue(Object content) {
        Object value = null;

        if (content == null) {
            value = NULL_VALUE;
        } else if (DataType.isSingleValueType(content.getClass())) {
            value = DataType.asScalarValue(content);
        }
        return value;
    }

}
