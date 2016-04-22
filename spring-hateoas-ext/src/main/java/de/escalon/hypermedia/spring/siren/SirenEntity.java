package de.escalon.hypermedia.spring.siren;

import java.util.List;
import java.util.Map;

/**
 * Created by Dietrich on 17.04.2016.
 */
public class SirenEntity extends AbstractSirenEntity {

    public SirenEntity() {
    }

    /**
     * Siren entity.
     */
    public SirenEntity(List<String> sirenClasses, Map<String, Object> properties, List<SirenSubEntity> entities,
                       List<SirenAction> actions, List<SirenLink> links) {

        super(sirenClasses, properties, entities, actions, links);
    }

    public List<SirenSubEntity> getEntities() {
        @SuppressWarnings("unchecked")
        List<SirenSubEntity> ret = (List<SirenSubEntity>) entities;
        return ret;
    }

    public Map<String, Object> getProperties() {
        return properties;
    }

    public List<SirenLink> getLinks() {
        return links;
    }

    public List<SirenAction> getActions() {
        return actions;
    }
}
