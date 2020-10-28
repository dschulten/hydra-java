package de.escalon.hypermedia;

import java.util.Collection;

import org.springframework.hateoas.Links;

/**
 * Created by Dietrich on 07.05.2016.
 */
public interface ResourceSupportVisitor {
    boolean visitLinks(Links links);

    boolean visitEnterCollection(Collection<?> collection);

    boolean visitLeaveCollection(Collection<?> collection);

    boolean visitEnterProperty(String name, Class<?> propertyType, Object value);

    boolean visitProperty(String name, Object value, Object o);

    boolean visitLeaveProperty(String name, Class<?> propertyType, Object value);
}
