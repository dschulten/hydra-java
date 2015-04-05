package de.escalon.hypermedia;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;

/**
 * Workaround for missing Parameter class previous to Java 1.8.
 * Created by Dietrich on 05.04.2015.
 */
public interface AnnotatedParameter {
    <T extends Annotation> T getAnnotation(Class<T> annotation);
    String getParameterName();
    Class<?> getDeclaringClass();
}
