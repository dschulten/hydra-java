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
import org.springframework.hateoas.CollectionModel;

import java.io.IOException;

/**
 * Serializer for CollectionModel. Created by dschulten on 15.09.2014.
 */
@SuppressWarnings("unused")
public class ResourcesSerializer extends StdSerializer<CollectionModel> {

    @SuppressWarnings("unused")
    public ResourcesSerializer() {
        super(CollectionModel.class);
    }

    @Override
    public void serialize(CollectionModel value, JsonGenerator jgen, SerializerProvider provider) throws IOException {

        final SerializationConfig config = provider.getConfig();
        JavaType javaType = config.constructType(value.getClass());

        JsonSerializer<Object> serializer = BeanSerializerFactory.instance.createSerializer(provider, javaType);

        jgen.writeStartObject();
        serializer.serialize(value, jgen, provider);
        jgen.writeEndObject();

    }

}

