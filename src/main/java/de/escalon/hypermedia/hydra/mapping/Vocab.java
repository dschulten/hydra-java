package de.escalon.hypermedia.hydra.mapping;

import java.lang.annotation.*;

/**
 * Defines default vocab for a package, use inside <code></code>package-info.java</code>.
 */
@Documented
@Target({ElementType.PACKAGE, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface Vocab {
    String value() default "http://schema.org";
}
