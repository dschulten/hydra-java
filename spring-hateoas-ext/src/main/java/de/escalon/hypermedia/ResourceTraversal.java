package de.escalon.hypermedia;

import de.escalon.hypermedia.affordance.DataType;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.RepresentationModel;
import org.springframework.hateoas.CollectionModel;

import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;
import java.util.*;

/**
 * Created by Dietrich on 07.05.2016.
 */
public class ResourceTraversal {

    static final Set<String> FILTER_RESOURCE_SUPPORT = new HashSet<String>(Arrays.asList("class", "links", "id"));

    static class NullValue {

    }
    public static final NullValue NULL_VALUE = new NullValue();


    public void traverseResource(ResourceSupportVisitor visitor, Object object) {
        Set<String> filtered = FILTER_RESOURCE_SUPPORT;
        if (object == null) {
            return;
        }

        try {
            // TODO: move all returns to else branch of property descriptor handling
            if (object instanceof EntityModel) {
                EntityModel<?> resource = (EntityModel<?>) object;

                if(!visitor.visitLinks(resource.getLinks())) {
                    return;
                }
                traverseResource(visitor, resource.getContent());
                return;
            } else if (object instanceof CollectionModel) {
                CollectionModel<?> resources = (CollectionModel<?>) object;
                if(!visitor.visitLinks(resources.getLinks())) {
                    return;
                }
                traverseResource(visitor, resources.getContent());
                return;
            } else if (object instanceof RepresentationModel) {
                RepresentationModel resource = (RepresentationModel) object;
                if(!visitor.visitLinks(resource.getLinks())) {
                    return;
                }
                // wrap object attributes below to avoid endless loop
            } else if (object instanceof Collection) {
                Collection<?> collection = (Collection<?>) object;
                if(!visitor.visitEnterCollection(collection)) {
                    return;
                }
                for (Object item : collection) {
                    traverseResource(visitor, item);
                }
                if(!visitor.visitLeaveCollection(collection)) {
                    return;
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
//                    UberNode entryNode = new UberNode();
//                    objectNode.addData(entryNode);
//                    entryNode.setName(key);
//                    if (value != null) {
//                        entryNode.setValue(value);
//                    } else {
//                        traverseResource(visitor, content);
//                    }

                    Class<?> type = content != null ? content.getClass() : null;
                    if(!visitor.visitEnterProperty(key, type, value)) {
                        return;
                    }
                    if (value != null) {
                        // for each scalar property of a simple bean, add valuepair nodes to data
                        if(!visitor.visitProperty(key, type, value)) {
                            return;
                        }
                    } else {
                        traverseResource(visitor, content);
                    }
                    if(!visitor.visitLeaveProperty(key, type, value)) {
                        return;
                    }
                }
            } else {
                Map<String, PropertyDescriptor> propertyDescriptors = PropertyUtils.getPropertyDescriptors(object);
                for (PropertyDescriptor propertyDescriptor : propertyDescriptors.values()) {
                    String name = propertyDescriptor.getName();
                    if (filtered.contains(name)) {
                        continue;
                    }

//                    UberNode propertyNode = new UberNode();
                    Object content = propertyDescriptor.getReadMethod()
                            .invoke(object);
//
//                    if (isEmptyCollectionOrMap(content, propertyDescriptor.getPropertyType())) {
//                        continue;
//                    }
//
                    Object value = getContentAsScalarValue(content);
//                    propertyNode.setName(name);
//                    objectNode.addData(propertyNode);
                    Class<?> propertyType = propertyDescriptor.getPropertyType();
                    if(!visitor.visitEnterProperty(name, propertyType, value)) {
                        return;
                    }
                    if (value != null) {
                        // for each scalar property of a simple bean, add valuepair nodes to data
                        if(!visitor.visitProperty(name, propertyType, value)) {
                            return;
                        }
                    } else {
                        traverseResource(visitor, content);
                    }
                    if(!visitor.visitLeaveProperty(name, propertyType, value)) {
                        return;
                    }
                }

                Field[] fields = object.getClass()
                        .getFields();
                for (Field field : fields) {
                    String name = field.getName();
                    if (!propertyDescriptors.containsKey(name)) {
                        Object content = field.get(object);
                        Class<?> type = field.getType();
//                        if (isEmptyCollectionOrMap(content, type)) {
//                            continue;
//                        }
//                        UberNode propertyNode = new UberNode();
//
                        Object value = getContentAsScalarValue(content);
//                        propertyNode.setName(name);
//                        objectNode.addData(propertyNode);
//                        if (value != null) {
//                            // for each scalar property of a simple bean, add valuepair nodes to data
//                            propertyNode.setValue(value);
//                        } else {
//                            toUberData(propertyNode, content);
//                        }

                        if(!visitor.visitEnterProperty(name, type, value)) {
                            return;
                        }
                        if (value != null) {
                            // for each scalar property of a simple bean, add valuepair nodes to data
                            if(!visitor.visitProperty(name, type, value)) {
                                return;
                            }
                        } else {
                            traverseResource(visitor, content);
                        }
                        if(!visitor.visitLeaveProperty(name, type, value)) {
                            return;
                        }

                    }
                }
            }
        } catch (Exception ex) {
            throw new RuntimeException("failed to transform object " + object, ex);
        }
    }

    private static Object getContentAsScalarValue(Object content) {
        final Object value;
        if (content == null) {
            value = ResourceTraversal.NULL_VALUE;
        } else if (DataType.isSingleValueType(content.getClass())) {
            value = content.toString();
        } else {
            value = null;
        }
        return value;
    }
}
