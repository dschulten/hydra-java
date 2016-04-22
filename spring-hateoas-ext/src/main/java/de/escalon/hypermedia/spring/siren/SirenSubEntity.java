package de.escalon.hypermedia.spring.siren;

import java.util.List;
import java.util.Map;

/**
 * Created by Dietrich on 17.04.2016.
 */
public abstract class SirenSubEntity extends AbstractSirenEntity {

    public SirenSubEntity(List<String> sirenClasses, Map<String, Object> properties,
                          List<SirenSubEntity> entities, List<SirenAction> actions, List<SirenLink>
                                  links, List<String> rels) {
        super(sirenClasses, properties, entities, actions, links, rels);
    }

    public SirenSubEntity(List<String> sirenClasses, List<String> rels, String href) {
        super(sirenClasses, rels, href);
    }


    public Map<String, Object> getProperties() {
        return super.properties;
    }

    public List<SirenSubEntity> getEntities() {
        @SuppressWarnings("unchecked")
        List<SirenSubEntity> ret = (List<SirenSubEntity>) entities;
        return ret;
    }


    public List<String> getRel() {
        return rel;
    }

    public String getHref() {
        return href;
    }
}
