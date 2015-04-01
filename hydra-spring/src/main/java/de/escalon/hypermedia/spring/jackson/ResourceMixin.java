/*
 * Copyright (c) 2014. Escalon System-Entwicklung, Dietrich Schulten
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package de.escalon.hypermedia.spring.jackson;

import com.fasterxml.jackson.annotation.JsonUnwrapped;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.Resource;

import java.util.List;

/**
 * Mixin for json-ld serialization of Resource.
 * Created by dschulten on 14.09.2014.
 */
@JsonSerialize(include = JsonSerialize.Inclusion.NON_EMPTY)
public abstract class ResourceMixin<T> extends Resource<T> {

    @SuppressWarnings("unused")
    public ResourceMixin(T content, Link... links) {
        super(content, links);
    }

    @SuppressWarnings("unused")
    public ResourceMixin(T content, Iterable<Link> links) {
        super(content, links);
    }
    @Override
    @JsonSerialize(using = LinkListSerializer.class)
    @JsonUnwrapped
    public List<Link> getLinks() {
        return super.getLinks();
    }
}
