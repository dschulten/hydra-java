package de.escalon.hypermedia;

import java.beans.PropertyDescriptor;

/**
 * Created by Dietrich on 11.03.2015.
 */
public class PropertyUtil {

    private PropertyUtil() {

    }

    public static Object getPropertyValue(Object currentCallValue, PropertyDescriptor propertyDescriptor) {
        Object propertyValue = null;
        if (currentCallValue != null && propertyDescriptor.getReadMethod() != null) {
            try {
                propertyValue = propertyDescriptor.getReadMethod()
                        .invoke(currentCallValue);
            } catch (Exception e) {
                throw new RuntimeException("failed to read property from call value", e);
            }
        }
        return propertyValue;
    }

}
