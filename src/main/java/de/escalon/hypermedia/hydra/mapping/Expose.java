package de.escalon.hypermedia.hydra.mapping;

import java.lang.annotation.*;

/**
 * The Expose annotation defines whether a field or class is exposed by the
 * serializer (or set by the deserializer). If a field or class is being exposed,
 * it is possible to define it's short name with the "as" parameter and it's
 * IRI (fragment) with the "value" parameter. If the value of "iri" contains
 * a colon, it is assumed to represent the full, absolute IRI, otherwise it
 * is interpreted as IRI fragment which is appended to the vocabulary's IRI.
 * The three parameters "required", "readonly" and "writeonly" are only used for fields.
 * <p/>
 * Created by dschulten on 10.08.2014.
 */
@Documented
@Target({ElementType.TYPE, ElementType.FIELD, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface Expose {

//    /**
//     * Alias for annotated field or type.
//     *
//     * @return alias
//     */
//    String as() default "";

    /**
     * Iri describing the attribute or type e.g. http://schema.org/name or http://schema.org/Person. Can also be
     * a compact iri like foaf:name or a term from the current vocab (e.g. just name if @vocab is schema.org).
     *
     * @return iri
     */
    String value();

    String[] contexts() default {};

}
