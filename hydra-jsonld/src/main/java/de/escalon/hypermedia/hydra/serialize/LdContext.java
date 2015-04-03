package de.escalon.hypermedia.hydra.serialize;

import java.util.Collections;
import java.util.Map;

/**
 * Holds Jsonld Context with context inheritance from parent contexts.
 * Created by Dietrich on 02.04.2015.
 */
public class LdContext {

    private LdContext parentContext;
    public final String vocab;
    public final Map<String, Object> terms;

    public LdContext(LdContext parentContext, String vocab, Map<String, Object> terms) {
        this.parentContext = parentContext;
        this.vocab = vocab;
        this.terms = Collections.unmodifiableMap(terms);
    }

    public boolean contains(LdContext otherContext) {
        final boolean ret;
        if (!hasSameVocabAs(otherContext)) {
            ret = false;
        } else {
            ret = this.hasTermsOf(otherContext);
        }
        return ret;
    }

    private boolean hasSameVocabAs(LdContext otherContext) {
        boolean ret;
        if (this.vocab == null) {
            if (parentContext == null) {
                ret = false;
            } else {
                ret = parentContext.hasSameVocabAs(otherContext);
            }
        } else {
            ret = this.vocab.equals(otherContext.vocab);
        }
        return ret;
    }

    private boolean hasTermsOf(LdContext otherContext) {
        boolean ret = true;
        for (String otherTerm : otherContext.terms.keySet()) {
            ret = hasEqualTerm(otherTerm, otherContext.terms.get(otherTerm));
            if (ret == false) {
                break;
            }
        }
        return ret;
    }

    private boolean hasEqualTerm(String term, Object value) {
        final boolean ret;
        if (this.terms.containsKey(term)) {
            ret = this.terms.get(term)
                    .equals(value);
        } else {
            if(parentContext == null) {
                ret = false;
            } else {
                ret = parentContext.hasEqualTerm(term, value);
            }
        }
        return ret;
    }

}