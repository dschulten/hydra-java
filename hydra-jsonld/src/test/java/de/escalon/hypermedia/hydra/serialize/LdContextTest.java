package de.escalon.hypermedia.hydra.serialize;

import junit.framework.TestCase;
import org.junit.Before;
import org.junit.Test;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Created by Dietrich on 03.04.2015.
 */
public class LdContextTest {

    private Map<String, Object> allTerms = new LinkedHashMap<String, Object>();
    private Map<String, Object> someTerms = new LinkedHashMap<String, Object>();

    LdContext contextWithAllTerms = new LdContext(null, "http://schema.org", allTerms);
    LdContext contextWithSomeTerms = new LdContext(null, "http://schema.org", someTerms);

    @Before
    public void setUp() {
        allTerms.put("hydra", "http://www.w3.org/ns/hydra/core#");
        allTerms.put("rdfs", "http://www.w3.org/2000/01/rdf-schema#");

        someTerms.put("hydra", "http://www.w3.org/ns/hydra/core#");
    }

    @Test
    public void allTermsContainsSomeTerms() throws Exception {
        assertTrue(contextWithAllTerms.contains(contextWithSomeTerms));
    }

    @Test
    public void someTermsDoesNotContainAllTerms() throws Exception {
        assertFalse(contextWithSomeTerms.contains(contextWithAllTerms));
    }

    @Test
    public void differentVocabMeansDifferenContext() throws Exception {
        LdContext contextWithDifferentVocab = new LdContext(null, "http://purl.org/goodrelations/v1#", allTerms);

        assertFalse(contextWithDifferentVocab.contains(contextWithAllTerms));
        assertFalse(contextWithAllTerms.contains(contextWithDifferentVocab));
    }

    @Test
    public void considersVocabFromParent() {
        LdContext childContext = new LdContext(contextWithAllTerms, null, allTerms);

        assertTrue(childContext.contains(contextWithSomeTerms));
    }

    @Test
    public void considersTermsFromParent() {
        LdContext childContext = new LdContext(contextWithAllTerms, null, Collections.<String, Object>emptyMap());

        assertTrue(childContext.contains(contextWithSomeTerms));
    }
}