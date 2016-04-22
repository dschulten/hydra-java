package de.escalon.hypermedia.spring.siren;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created by Dietrich on 17.04.2016.
 */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
@JsonPropertyOrder({"class", "rel", "properties", "entities", "actions", "links"})
public abstract class AbstractSirenEntity {
    @JsonProperty("class")
    private List<String> sirenClasses;

    // content may have nested attributes, see https://groups.google.com/forum/#!msg/api-craft/GYNZBED1a-8/ekpeDZXpeF0J
    protected Map<String, Object> properties;

    protected List<? super SirenSubEntity> entities;

    protected List<SirenAction> actions;

    // Navigational GET Affordances: self, next, prev
    protected List<SirenLink> links;

    protected List<String> rel;

    protected String href;

    protected String title;

    protected AbstractSirenEntity() {

    }

    /**
     * Siren link
     */
    public AbstractSirenEntity(List<String> rels, String href) {
        this.rel = rels;
        this.href = href;
    }

    /**
     * Siren entity.
     */
    public AbstractSirenEntity(List<String> sirenClasses, Map<String, Object> properties, List<SirenSubEntity> entities,
                               List<SirenAction> actions, List<SirenLink> links) {

        this.sirenClasses = sirenClasses;
        this.properties = properties;
        this.entities = entities;
        this.actions = actions;
        this.links = links;
    }

    /**
     * Siren embedded representation.
     * @param sirenClasses classes
     * @param properties object
     * @param entities sub-entities
     * @param actions actions
     * @param links navigational links
     * @param rels rels
     */
    public AbstractSirenEntity(List<String> sirenClasses, Map<String, Object> properties, List<SirenSubEntity> entities,
                               List<SirenAction> actions, List<SirenLink> links, List<String> rels) {

        this.sirenClasses = sirenClasses;
        this.rel = rels;
        this.properties = properties;
        this.entities = entities;
        this.actions = actions;
        this.links = links;
    }

    /**
     * Siren embedded link.
     */
    public AbstractSirenEntity(List<String> sirenClasses, List<String> rels, String href) {

        this.sirenClasses = sirenClasses;
        this.rel = rels;
        this.href = href;
    }

    // TODO remove links from ctors?
    public void setLinks(List<SirenLink> links) {
        this.links = links;
    }

    public void setProperties(Map<String,Object> properties) {
        this.properties = properties;
    }

    public void addSubEntity(SirenSubEntity sirenSubEntity) {
        if(this.entities == null) {
            this.entities = new ArrayList<SirenSubEntity>();
        }
        entities.add(sirenSubEntity);
    }

    public void setEmbeddedLinks(List<SirenEmbeddedLink> embeddedLinks) {
        if(this.entities == null) {
            this.entities = new ArrayList<SirenSubEntity>();
        }
        this.entities.addAll(embeddedLinks);
    }

    public void setActions(List<SirenAction> actions) {
        this.actions = actions;
    }
}