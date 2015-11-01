package de.escalon.hypermedia.spring.hydra;

import com.fasterxml.jackson.annotation.JsonProperty;
import de.escalon.hypermedia.affordance.TypedResource;
import de.escalon.hypermedia.hydra.serialize.JsonLdKeywords;

/**
 * Renders typed resource as Json-LD typed resource.
 * Created by Dietrich on 01.11.2015.
 */
public class TypedResourceMixin {

    @JsonProperty(JsonLdKeywords.AT_TYPE)
    public String getTypeUri() {
        throw new UnsupportedOperationException("calling mixin method is not supported");
    }

    @JsonProperty(JsonLdKeywords.AT_ID)
    public String getIdentifyingUri() {
        throw new UnsupportedOperationException("calling mixin method is not supported");
    }
}
