/*
 * Copyright (c) 2014. Escalon System-Entwicklung, Dietrich Schulten
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package de.escalon.hypermedia.spring.hydra;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import de.escalon.hypermedia.hydra.serialize.ProxyUnwrapper;
import de.escalon.hypermedia.spring.HypermediaTypes;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;

import java.util.Arrays;

/**
 * Created by dschulten on 04.10.2014.
 */
public class HydraMessageConverter extends MappingJackson2HttpMessageConverter {


    public HydraMessageConverter() {
        this(null);
    }

    /**
     * Creates new HydraMessageConverter with proxyUnwrapper.
     * @param proxyUnwrapper capable of unwrapping proxified Java beans during message conversion.
     */
    public HydraMessageConverter(ProxyUnwrapper proxyUnwrapper, Module... additionalModules) {
        ObjectMapper objectMapper = new ObjectMapper();
        // see https://github.com/json-ld/json-ld.org/issues/76
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        objectMapper.registerModules(additionalModules);
        objectMapper.registerModule(new JacksonHydraModule(proxyUnwrapper));
        this.setObjectMapper(objectMapper);
        this.setSupportedMediaTypes(
                Arrays.asList(HypermediaTypes.APPLICATION_JSONLD));
    }
}
