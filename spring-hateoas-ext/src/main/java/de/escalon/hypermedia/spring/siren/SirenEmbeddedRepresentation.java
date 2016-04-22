package de.escalon.hypermedia.spring.siren;

import org.springframework.util.Assert;

import java.util.List;
import java.util.Map;

/**
 * Created by Dietrich on 17.04.2016.
 */
public class SirenEmbeddedRepresentation extends SirenSubEntity {
    /**
     * Siren embedded representation.
     *
     * @param sirenClasses
     *         classes
     * @param properties
     *         object
     * @param entities
     *         sub-entities
     * @param actions
     *         actions
     * @param links
     *         navigational links
     * @param rels
     *         rels
     */
    public SirenEmbeddedRepresentation(List<String> sirenClasses, Map<String, Object> properties,
                                       List<SirenSubEntity> entities,
                                       List<SirenAction> actions, List<SirenLink> links, List<String> rels) {
        super(sirenClasses, properties, entities, actions, links, rels);
        Assert.notEmpty(rels, "embedded representations must have a rel");
    }

    public List<SirenLink> getLinks() {
        return links;
    }

    public List<SirenAction> getActions() {
        return actions;
    }


}
