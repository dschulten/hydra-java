/*
 * Copyright (c) 2014. Escalon System-Entwicklung, Dietrich Schulten
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package de.escalon.hypermedia.spring.de.escalon.hypermedia.spring.jackson;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import org.springframework.hateoas.IanaRels;
import org.springframework.hateoas.Link;

import java.io.IOException;
import java.util.List;

/**
 * Serializer to convert Link to json-ld representation.
 * Created by dschulten on 19.09.2014.
 */
public class LinkListSerializer extends StdSerializer<List<Link>> {


    private static final String IANA_REL_PREFIX = "urn:iana:link-relations:";

    public LinkListSerializer() {
        super(List.class, false);
    }

    @Override
    public void serialize(List<Link> links, JsonGenerator jgen,
                          SerializerProvider provider) throws IOException {

        for (Link link : links) {
            final String rel = link.getRel();


            if (Link.REL_SELF.equals(rel)) {
                jgen.writeStringField("@id", link.getHref());
            } else {
                String linkFieldName = IanaRels.isIanaRel(rel) ? IANA_REL_PREFIX + rel : rel;
                jgen.writeFieldName(linkFieldName);
                jgen.writeStartObject();
                jgen.writeStringField("@id", link.getHref());
                jgen.writeStringField("@type", "@id");
                jgen.writeEndObject();
            }
        }

    }

    @Override
    public boolean isUnwrappingSerializer() {
        return true;
    }
}
