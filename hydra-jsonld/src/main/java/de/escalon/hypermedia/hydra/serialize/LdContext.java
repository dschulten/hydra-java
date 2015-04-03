package de.escalon.hypermedia.hydra.serialize;

import java.util.Collections;
import java.util.Map;

/**
 * Holds Jsonld Context
 * Created by Dietrich on 02.04.2015.
 */
public class LdContext {

    public final String vocab;
    public final Map<String, Object> terms;

    public LdContext(String vocab, Map<String, Object> terms) {
        this.vocab = vocab;
        this.terms = Collections.unmodifiableMap(terms);
    }

    public boolean contains(LdContext otherContext) {
        final boolean ret;
        if (this.vocab != null && !this.vocab.equals(otherContext.vocab)) {
            ret = false;
        } else {
            ret = hasTermsOf(otherContext);
        }
        return ret;
    }

    private boolean hasTermsOf(LdContext otherContext) {
        boolean ret = true;
        for (String otherTerm : otherContext.terms.keySet()) {
            if (this.terms.containsKey(otherTerm)) {
                if (!this.terms.get(otherTerm)
                        .equals(otherContext.terms.get(otherTerm))) {
                    ret = false;
                    break;
                }
            } else {
                ret = false;
                break;
            }
        }
        return ret;
    }
}