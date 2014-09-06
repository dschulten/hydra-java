package de.escalon.hypermedia.hydra;

import com.github.jsonldjava.core.JsonLdError;
import com.github.jsonldjava.core.JsonLdOptions;
import com.github.jsonldjava.core.JsonLdProcessor;
import com.github.jsonldjava.utils.JsonUtils;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * Unit test for simple App.
 */
public class JsonLdTestUtils {

    private JsonLdTestUtils() {
        // prevent instantiation
    }

    public static String applyContext(String jsonLd) throws IOException, JsonLdError {
        Object jsonObject = JsonUtils.fromString(jsonLd);

        Map context = new HashMap();
        JsonLdOptions options = new JsonLdOptions();
        Map<String, Object> compact = JsonLdProcessor.compact(jsonObject, context, options);

        return JsonUtils.toPrettyString(compact);
    }

    @Test
    public void testMovie() throws IOException, JsonLdError {
        InputStream inputStream = this.getClass().getResourceAsStream("/Movie.json");
        // Read the file into an Object (The type of this object will be a List, Map, String, Boolean,
        // Number or null depending on the root object in the file).
        Object jsonObject = JsonUtils.fromInputStream(inputStream);
        // Create a context JSON map containing prefixes and definitions
        Map context = new HashMap();
        // Customise context...
        // Create an instance of JsonLdOptions with the standard JSON-LD options
        JsonLdOptions options = new JsonLdOptions();
        //        options.
        // Customise options...
        // Call whichever JSONLD function you want! (e.g. compact)
        Map<String, Object> compact = JsonLdProcessor.compact(jsonObject, context, options);

        // Print out the result (or don't, it's your call!)
        System.out.println(JsonUtils.toPrettyString(compact));
    }
}
