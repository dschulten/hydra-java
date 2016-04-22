package de.escalon.hypermedia.spring.siren;

import java.util.List;

/**
 * Created by Dietrich on 17.04.2016.
 */
public class SirenEmbeddedLink extends SirenSubEntity {


    /**
     * Siren embedded link.
     *
     * @param sirenClasses
     *         classes
     * @param rels
     *         relation names
     * @param href
     *         url
     */
    public SirenEmbeddedLink(List<String> sirenClasses, List<String> rels, String href) {
        super(sirenClasses, rels, href);
    }



}
