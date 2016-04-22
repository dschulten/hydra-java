package de.escalon.hypermedia.spring.siren;

import java.util.List;

/**
 * Created by Dietrich on 17.04.2016.
 */
public class SirenLink extends AbstractSirenEntity {

    public SirenLink(List<String> rels, String href) {
        super(rels, href);
    }

    public List<String> getRel() {
        return super.rel;
    }

    public String getHref() {
        return href;
    }
}
