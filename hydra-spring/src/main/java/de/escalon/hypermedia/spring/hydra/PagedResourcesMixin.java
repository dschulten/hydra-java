package de.escalon.hypermedia.spring.hydra;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import de.escalon.hypermedia.hydra.mapping.ContextProvider;
import de.escalon.hypermedia.hydra.mapping.Expose;
import de.escalon.hypermedia.hydra.mapping.Term;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.PagedResources;

import java.util.Collection;
import java.util.List;

/**
 * Mixin for json-ld serialization of PagedResources.
 */
@JsonSerialize(include = JsonSerialize.Inclusion.NON_EMPTY)
@Term(define = "hydra", as = "http://www.w3.org/ns/hydra/core#")
@Expose("hydra:Collection")
public abstract class PagedResourcesMixin<T> extends PagedResources<T> {
    @Override
    @JsonProperty("hydra:member")
    @ContextProvider
    public Collection<T> getContent() {
        return super.getContent();
    }

    @Override
    @JsonSerialize(using = LinkListSerializer.class)
    @JsonUnwrapped
    public List<Link> getLinks() {
        return super.getLinks();
    }

    @Override
    @JsonIgnore // used by PagedResourcesSerializer instead
    public PageMetadata getMetadata() {
        return super.getMetadata();
    }
}
