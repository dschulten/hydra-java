package de.escalon.hypermedia;

import org.springframework.hateoas.Link;

import java.util.Collection;
import java.util.List;

/**
 * Created by Dietrich on 07.05.2016.
 */
public interface ResourceSupportVisitor {
    boolean visitLinks(List<Link> links);

    boolean visitEnterCollection(Collection<?> collection);

    boolean visitLeaveCollection(Collection<?> collection);

    boolean visitEnterProperty(String name, Class<?> propertyType, Object value);

    boolean visitProperty(String name, Object value, Object o);

    boolean visitLeaveProperty(String name, Class<?> propertyType, Object value);
}
