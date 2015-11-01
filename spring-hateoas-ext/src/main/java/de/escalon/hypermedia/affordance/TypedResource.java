package de.escalon.hypermedia.affordance;

import org.springframework.util.Assert;

/**
 * Resource of a certain semantic type which may or may not be identifiable.
 */
public class TypedResource {

    private String typeUri;
    private String identifyingUri;

    /**
     * Creates a resource whose semantic type is known, but which cannot be identified as an individual.
     *
     * @param typeUri
     *         semantic type of the resource as string, either as Uri or Curie
     * @see <a href="http://www.w3.org/TR/curie/">Curie</a>
     */
    public TypedResource(String typeUri) {
        Assert.notNull(typeUri, "typeUri must be given");
        this.typeUri = typeUri;
    }

    /**
     * Creates identified resource of a semantic type.
     *
     * @param typeUri
     *         semantic type of the resource as string, either as Uri or Curie
     * @param identifyingUri
     *         identifying an individual of the typed resource
     * @see <a href="http://www.w3.org/TR/curie/">Curie</a>
     */
    public TypedResource(String typeUri, String identifyingUri) {
        Assert.notNull(typeUri, "typeUri must be given");
        Assert.notNull(identifyingUri, "identifyingUri must be given");
        this.typeUri = typeUri;
        this.identifyingUri = identifyingUri;
    }

    public String getTypeUri() {
        return typeUri;
    }

    public String getIdentifyingUri() {
        return identifyingUri;
    }
}
