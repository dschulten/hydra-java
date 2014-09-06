package de.escalon.hypermedia.hydra.mapping;

import java.lang.annotation.*;

/**
 * Allows to define multiple <code>&#64;Term</code>s in a type or package.
 */
@Documented
@Target({ElementType.PACKAGE, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface Terms {

    Term[] value();
}
