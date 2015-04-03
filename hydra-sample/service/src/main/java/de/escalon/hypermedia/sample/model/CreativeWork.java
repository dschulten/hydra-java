package de.escalon.hypermedia.sample.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import de.escalon.hypermedia.hydra.mapping.Term;

/**
 * Created by dschulten on 08.12.2014.
 */
@Term(define="rdfs", as="http://www.w3.org/2000/01/rdf-schema#")
public class CreativeWork {

    public final String name;

    @JsonCreator
    public CreativeWork(@JsonProperty("name") String name) {
        this.name = name;
    }


}

