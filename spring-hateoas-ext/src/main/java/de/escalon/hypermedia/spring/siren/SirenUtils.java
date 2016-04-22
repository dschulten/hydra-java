package de.escalon.hypermedia.spring.siren;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import de.escalon.hypermedia.PropertyUtils;
import de.escalon.hypermedia.affordance.ActionDescriptor;
import de.escalon.hypermedia.affordance.Affordance;
import de.escalon.hypermedia.affordance.AnnotatedParameter;
import de.escalon.hypermedia.affordance.DataType;
import de.escalon.hypermedia.spring.ActionInputParameter;
import org.springframework.core.MethodParameter;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.hateoas.*;
import org.springframework.hateoas.core.Relation;
import org.springframework.util.Assert;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.*;

/**
 * Created by Dietrich on 17.04.2016.
 */
public class SirenUtils {

    static final Set<String> FILTER_RESOURCE_SUPPORT = new HashSet<String>(Arrays.asList("class", "links", "id"));

    static Set<String> NAVIGATIONAL_RELS = new HashSet<String>(Arrays.asList("self", "next", "previous", "prev"));

    public static void setNavigationalRels(Set<String> navigationalRels) {
        SirenUtils.NAVIGATIONAL_RELS = navigationalRels;
    }

    public static void toSirenEntity(AbstractSirenEntity objectNode, Object object, RelProvider relProvider) {
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
//                    SirenEntity sirenEntity = new SirenEntity()
//                    objectNode.addData(itemNode);
//                    toUberData(itemNode, item);

//                    toUberData(objectNode, item);
                }
                return;
            }
            if (object instanceof Map) {
                Map<?, ?> map = (Map<?, ?>) object;
                for (Map.Entry<?, ?> entry : map.entrySet()) {
                    String key = entry.getKey()
                            .toString();
                    Object content = entry.getValue();
                    Object value = getContentAsScalarValue(content);
                    // TODO how create map item?
//                    SirenEntity entryNode = new SirenEntity();
//                    objectNode.addData(entryNode);
//                    entryNode.setName(key);
//                    if (value != null) {
//                        entryNode.setValue(value);
//                    } else {
//                        toUberData(entryNode, content);
//                    }
                }
            } else { // bean or ResourceSupport
                // TODO fields
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
            if (!NAVIGATIONAL_RELS.contains(link.getRel())) {
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
            if (NAVIGATIONAL_RELS.contains(link.getRel())) {
                ret.add(link);
            }
        }
        return ret;
    }

    private static List<Link> getActions(List<Link> links) {
        List<Link> ret = new ArrayList<Link>();
        for (Link link : links) {
            if (!NAVIGATIONAL_RELS.contains(link.getRel())) {
                if (link instanceof Affordance) {
                    Affordance affordance = (Affordance) link;
                    List<ActionDescriptor> actionDescriptors = affordance.getActionDescriptors();
                    for (ActionDescriptor actionDescriptor : actionDescriptors) {
                        // non-GET and templated links are actions
                        if (!("GET".equals(actionDescriptor.getHttpMethod())) || affordance.isTemplated()) {
                            ret.add(link);
                        }
                    }
                } else {
                    if (link.isTemplated()) {
                        ret.add(link);
                    }
                }
            }
        }
        return ret;
    }


    private static void recurseEntities(AbstractSirenEntity objectNode, Map<String, Object> propertiesNode,
                                        Object object, RelProvider relProvider) throws InvocationTargetException, IllegalAccessException {
        PropertyDescriptor[] propertyDescriptors = getPropertyDescriptors(object);
        for (PropertyDescriptor propertyDescriptor : propertyDescriptors) {
            String name = propertyDescriptor.getName();
            if (FILTER_RESOURCE_SUPPORT.contains(name)) {
                continue;
            }


            Object content = propertyDescriptor.getReadMethod()
                    .invoke(object);
            Object value = getContentAsScalarValue(content);

            if (value == NULL_VALUE) {
                continue;
            } else if (value != null) {
                // for each scalar property of a simple bean, add valuepair
                propertiesNode.put(name, value);
            } else {
                if (content instanceof ResourceSupport) {
                    traverseSubEntity(objectNode, content, relProvider);
                } else if (content instanceof Collection) {
                    Collection<?> collection = (Collection<?>) content;
                    for (Object item : collection) {
                        traverseSubEntity(objectNode, item, relProvider);
                    }
                } else {
                    Map<String, Object> nested = new HashMap<String, Object>();
                    propertiesNode.put(name, nested);
                    recurseEntities(objectNode, nested, content, relProvider);
                }
            }

        }
    }

    private static void traverseSubEntity(AbstractSirenEntity objectNode, Object content, RelProvider relProvider) throws InvocationTargetException, IllegalAccessException {
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

        String rel = relProvider.getItemResourceRelFor(bean.getClass());

        Map<String, Object> properties = new HashMap<String, Object>();
        SirenEmbeddedRepresentation subEntity = new SirenEmbeddedRepresentation(
                Collections.<String>emptyList(), properties, null, toSirenActions(getActions(links)),
                toSirenLinks(getNavigationalLinks(links)), Arrays.asList(rel));
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

                    SirenAction sirenAction = new SirenAction(null, null, actionDescriptor.getHttpMethod(),
                            affordance.getHref(), null, fields);

                    // simple parameters or request body attributes

                    ret.add(sirenAction);
                }
            } else if (link.isTemplated()) {
                List<SirenField> fields = new ArrayList<SirenField>();
                List<TemplateVariable> variables = link.getVariables();
                for (TemplateVariable variable : variables) {
                    fields.add(new SirenField(variable.getName(), "text", null, variable.getDescription()));
                }
                SirenAction sirenAction = new SirenAction(null, null, "GET",
                        link.getHref(), null, fields);
            }
        }
        return ret;
    }

    private static List<SirenField> toSirenFields(ActionDescriptor actionDescriptor) {
        List<SirenField> ret = new ArrayList<SirenField>();
        if (actionDescriptor.hasRequestBody()) {
            recurseBeanProperties(ret, actionDescriptor.getRequestBody()
                    .getParameterType(), actionDescriptor, actionDescriptor.getRequestBody(), actionDescriptor
                    .getRequestBody()
                    .getCallValue(), "");
        } else {
            Collection<AnnotatedParameter> inputParameters = actionDescriptor.getInputParameters();
            for (AnnotatedParameter inputParameter : inputParameters) {
                ret.add(new SirenField(inputParameter.getParameterName(),
                        inputParameter.getHtmlInputFieldType()
                                .name()
                                .toLowerCase(), inputParameter.getCallValueFormatted(), null));
            }
        }
        return ret;
    }

    /**
     * Renders input fields for bean properties of bean to add or update or patch.
     *
     * @param fields
     *         to add to
     * @param beanType
     *         to render
     * @param actionDescriptor
     *         which describes the method
     * @param actionInputParameter
     *         which requires the bean
     * @param currentCallValue
     *         sample call value
     * @throws IOException
     */
    private static void recurseBeanProperties(List<SirenField> fields, Class<?> beanType,
                                              ActionDescriptor actionDescriptor,
                                              AnnotatedParameter actionInputParameter, Object currentCallValue,
                                              String parentParamName) {
        // TODO support Option provider by other method args?
        final BeanInfo beanInfo = getBeanInfo(beanType);
        final PropertyDescriptor[] propertyDescriptors = beanInfo.getPropertyDescriptors();
        // TODO collection and map

        // TODO: do not add two inputs for setter and ctor

        // TODO almost duplicate of HtmlResourceMessageConverter.recursivelyCreateObject
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
            // TODO Constructor parameters, yet to add field/property support
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

                            // TODO duplicate below for PropertyDescriptors and in appendForm
                            if (DataType.isSingleValueType(parameterType)) {

                                if (actionInputParameter.isIncluded(paramName)) {

                                    Object propertyValue = PropertyUtils.getPropertyOrFieldValue(currentCallValue,
                                            paramName);

                                    ActionInputParameter constructorParamInputParameter = new ActionInputParameter
                                            (new MethodParameter(constructor, paramIndex), propertyValue);

                                    final Object[] possibleValues =
                                            actionInputParameter.getPossibleValues(
                                                    constructor, paramIndex, actionDescriptor);

                                    String propertyValueAsString = propertyValue == null ? null : propertyValue
                                            .toString();
                                    fields.add(new SirenField(parentParamName + paramName,
                                            constructorParamInputParameter.getHtmlInputFieldType()
                                                    .name()
                                                    .toLowerCase(),
                                            propertyValueAsString, null));

                                }
                            } else if (DataType.isArrayOrCollection(parameterType)) {
                                // not supported by Siren
                            } else {
//                                beginDiv();
//                                write(paramName + ":");
                                // TODO consider to concatenate parent and child param name
                                Object propertyValue = PropertyUtils.getPropertyOrFieldValue(currentCallValue,
                                        paramName);
                                recurseBeanProperties(fields, parameterType, actionDescriptor, actionInputParameter,
                                        propertyValue, paramName + "_");
//                                endDiv();
                            }
                            paramIndex++; // increase for each @JsonProperty
                        }
                    }
                }
                Assert.isTrue(parameters.length == paramIndex,
                        "not all constructor arguments of @JsonCreator " + constructor.getName() +
                                " are annotated with @JsonProperty");
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to write input fields for constructor", e);
        }

//            // TODO non-writable properties and public fields: make sure the inputs are part of a form
//            // write input field for every setter
//            for (PropertyDescriptor propertyDescriptor : propertyDescriptors) {
//                final Method writeMethod = propertyDescriptor.getWriteMethod();
//                if (writeMethod == null) {
//                    continue;
//                }
//                final Class<?> propertyType = propertyDescriptor.getPropertyType();
//
//                String propertyName = propertyDescriptor.getName();
//
//                if (DataType.isSingleValueType(propertyType)) {
//
//                    final Property property = new Property(beanType, propertyDescriptor.getReadMethod(),
//                            propertyDescriptor.getWriteMethod(), propertyDescriptor.getName());
//
//                    Object propertyValue = PropertyUtils.getPropertyOrFieldValue(currentCallValue, propertyName);
//                    MethodParameter methodParameter = new MethodParameter(propertyDescriptor.getWriteMethod(), 0);
//                    ActionInputParameter propertySetterInputParameter = new ActionInputParameter(methodParameter,
//                            propertyValue);
//                    final Object[] possibleValues = actionInputParameter.getPossibleValues(propertyDescriptor
//                                    .getWriteMethod(), 0,
//                            actionDescriptor);
//                    appendInputOrSelect(actionInputParameter, propertyName, propertySetterInputParameter,
// possibleValues);
//                } else if (actionInputParameter.isArrayOrCollection()) {
//                    Object[] callValues = actionInputParameter.getCallValues();
//                    int items = callValues.length;
//                    for (int i = 0; i < items; i++) {
//                        Object value;
//                        if (i < callValues.length) {
//                            value = callValues[i];
//                        } else {
//                            value = null;
//                        }
//                        recurseBeanProperties(actionInputParameter.getParameterType(), actionDescriptor,
//                                actionInputParameter, value);
//                    }
//                } else {
//                    beginDiv();
//                    write(propertyName + ":");
//                    Object propertyValue = PropertyUtils.getPropertyValue(currentCallValue, propertyDescriptor);
//                    recurseBeanProperties(propertyType, actionDescriptor, actionInputParameter, propertyValue);
//                    endDiv();
//                }
//            }
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
                ret.add(new SirenLink(((Affordance) link).getRels(), link.getHref()));
            } else {
                ret.add(new SirenLink(Arrays.asList(link.getRel()), link.getHref()));
            }
        }
        return ret;
    }

    private static List<SirenEmbeddedLink> toSirenEmbeddedLinks(List<Link> links) {
        List<SirenEmbeddedLink> ret = new ArrayList<SirenEmbeddedLink>();
        for (Link link : links) {
            if (link instanceof Affordance) {
                // TODO: how to determine classes? type of target resource? collection/item?
                ret.add(new SirenEmbeddedLink(Collections.<String>emptyList(), ((Affordance) link).getRels(), link
                        .getHref()));
            } else {
                ret.add(new SirenEmbeddedLink(Collections.<String>emptyList(), Arrays.asList(link.getRel()), link
                        .getHref()));
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

    private static PropertyDescriptor[] getPropertyDescriptors(Object bean) {
        try {
            return Introspector.getBeanInfo(bean.getClass())
                    .getPropertyDescriptors();
        } catch (IntrospectionException e) {
            throw new RuntimeException("failed to get property descriptors of bean " + bean, e);
        }
    }

}
