package de.escalon.hypermedia.affordance;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Bean related utitility methods Created by Dietrich on 05.12.2015.
 */
public class BeanUtil {

    private static Logger LOG = LoggerFactory.getLogger(BeanUtil.class);

    private BeanUtil() {
        // prevent instantiation
    }

    /**
     * Recursively build list of property paths for bean.
     *
     * @param clazz
     *         of bean
     * @return property paths
     */
    public static List<String> getPropertyPaths(Class<?> clazz) {
        return addClassPropertyPaths(new ArrayList<String>(), "", clazz);
    }

    private static List<String> addClassPropertyPaths(List<String> ret, String currentPath, Class<?> clazz) {
        PropertyDescriptor[] propertyDescriptors = getPropertyDescriptors(clazz);
        for (PropertyDescriptor propertyDescriptor : propertyDescriptors) {
            addPropertyPaths(ret, currentPath, propertyDescriptor);
        }
        return ret;
    }

    private static List<String> addPropertyPaths(List<String> ret, String currentPath, PropertyDescriptor
            propertyDescriptor) {
        String propertyName = propertyDescriptor.getName();
        if ("class".equals(propertyName)) {
            return Collections.emptyList();
        }
        Class<?> propertyType = propertyDescriptor.getPropertyType();
        if (DataType.isSingleValueType(propertyType)) {
            if (!currentPath.isEmpty()) {
                currentPath += ".";
            }
            ret.add(currentPath + propertyName);
        } else if (DataType.isArrayOrCollection(propertyType)) {
            LOG.warn((currentPath.isEmpty() ? currentPath : currentPath + ".")
                    + propertyName + ": property paths for collections not supported yet");
            // TODO support for collection properties
        } else {
            currentPath += propertyName;
            addClassPropertyPaths(ret, currentPath, propertyType);
        }
        return ret;
    }

    private static PropertyDescriptor[] getPropertyDescriptors(Class<?> clazz) {
        try {
            BeanInfo info = Introspector.getBeanInfo(clazz);
            return info.getPropertyDescriptors();
        } catch (IntrospectionException e) {
            throw new RuntimeException(e);
        }
    }
}
