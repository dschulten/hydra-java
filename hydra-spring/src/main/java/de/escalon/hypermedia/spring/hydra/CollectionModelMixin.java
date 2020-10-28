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

import de.escalon.hypermedia.hydra.mapping.ContextProvider;
import de.escalon.hypermedia.hydra.mapping.Expose;
import de.escalon.hypermedia.hydra.mapping.Term;

import java.util.Collection;

import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.Links;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

/**
 * Mixin for json-ld serialization of CollectionModel. Created by dschulten on 14.09.2014.
 */
@JsonSerialize(include = JsonSerialize.Inclusion.NON_EMPTY)
@Term(define = "hydra", as = "http://www.w3.org/ns/hydra/core#")
@Expose("hydra:Collection")
public abstract class CollectionModelMixin<T> extends CollectionModel<T> {

    @Override
    @JsonProperty("hydra:member")
    @ContextProvider
    public Collection<T> getContent() {
        return super.getContent();
    }

    @Override
    @JsonSerialize(using = LinkListSerializer.class)
    @JsonUnwrapped
    public Links getLinks() {
        return super.getLinks();
    }
}
