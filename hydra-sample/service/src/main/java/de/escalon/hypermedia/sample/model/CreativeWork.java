package de.escalon.hypermedia.sample.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Created by dschulten on 08.12.2014.
 */
public class CreativeWork {

    public final String name;

    @JsonCreator
    public CreativeWork(@JsonProperty("name") String name) {
        this.name = name;
    }


}

