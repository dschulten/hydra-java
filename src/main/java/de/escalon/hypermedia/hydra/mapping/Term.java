package de.escalon.hypermedia.hydra.mapping;

import java.lang.annotation.*;

/**
 * Defines a single term. If you need to define several terms, use @Terms.
 */
@Documented
@Target({ElementType.PACKAGE, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface Term {
    /**
     * Term, e.g. a shorthand for a vocabulary to be used in compact uris, like foaf,
     * or a manual term definition such as 'fullName'.
     *
     * @return term
     */
    String define();

    /**
     * What the term stands for, e.g. a vocabulary uri like http://xmlns.com/foaf/0.1/ could stand for foaf.
     * an uri for a property like http://xmlns.com/foaf/0.1/name could stand for fullName.
     *
     * @return definition of term
     */
    String as();
}
