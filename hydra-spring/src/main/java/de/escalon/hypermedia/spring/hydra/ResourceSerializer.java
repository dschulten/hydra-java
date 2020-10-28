/*
 * Copyright (c) 2014. Escalon System-Entwicklung, Dietrich Schulten
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for
 * the specific language governing permissions and limitations under the License.
 */

package de.escalon.hypermedia.spring.hydra;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializationConfig;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.BeanSerializerFactory;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import com.fasterxml.jackson.databind.util.NameTransformer;
import org.springframework.hateoas.EntityModel;

import java.io.IOException;

/**
 * Serializer for json-ld representation of EntityModel. Created by dschulten on 15.09.2014.
 */
public class ResourceSerializer extends StdSerializer<EntityModel> {

    public ResourceSerializer() {
        super(EntityModel.class);
    }

    @Override
    public void serialize(EntityModel value, JsonGenerator jgen, SerializerProvider provider) throws IOException {

        final SerializationConfig config = provider.getConfig();
        JavaType javaType = config.constructType(value.getClass());

        JsonSerializer<Object> serializer = BeanSerializerFactory.instance.createSerializer(provider, javaType);

        jgen.writeStartObject();
        serializer.unwrappingSerializer(NameTransformer.NOP)
                .serialize(value, jgen, provider);

        jgen.writeEndObject();

    }

}

