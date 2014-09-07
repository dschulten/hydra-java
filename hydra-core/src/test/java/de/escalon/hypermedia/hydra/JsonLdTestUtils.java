/*
 * Copyright (c) 2014. Escalon System-Entwicklung, Dietrich Schulten
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */
package de.escalon.hypermedia.hydra;

import com.github.jsonldjava.core.JsonLdError;
import com.github.jsonldjava.core.JsonLdOptions;
import com.github.jsonldjava.core.JsonLdProcessor;
import com.github.jsonldjava.utils.JsonUtils;

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

    public static void main(String[] args) throws IOException, JsonLdError {
        InputStream inputStream = JsonLdTestUtils.class.getResourceAsStream("/valueExpansion.jsonld");
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
