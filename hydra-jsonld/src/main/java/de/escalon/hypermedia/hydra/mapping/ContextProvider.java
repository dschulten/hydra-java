package de.escalon.hypermedia.hydra.mapping;

import java.lang.annotation.*;

/**
 * Allows to identify a method with generic return type and zero arguments which returns objects having a jsonld
 * context. Sometimes a serialized class is a generic type with a type argument that has a jsonld context defined with
 * mapping annotations such as {@link Term}, {@link Expose}  or {@link Vocab}. Annotating a method that returns objects
 * of the generic type allows to determine the jsonld context of the generic type. Created by Dietrich on 03.04.2015.
 */
@Documented
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface ContextProvider {
}
