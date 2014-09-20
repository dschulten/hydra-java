/*
 * Copyright (c) 2014. Escalon System-Entwicklung, Dietrich Schulten
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package de.escalon.hypermedia.spring;

import org.springframework.hateoas.Link;
import org.springframework.hateoas.mvc.ControllerLinkBuilder;
import org.springframework.util.Assert;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.util.List;
import java.util.Map;

/**
 * Builder for rfc5988 web links.
 * Created by dschulten on 07.09.2014.
 */
public class AffordanceBuilder {

    private final ControllerLinkBuilder controllerLinkBuilder;

    private MultiValueMap<String, String> linkParams = new LinkedMultiValueMap<String, String>();

    private AffordanceBuilder(ControllerLinkBuilder controllerLinkBuilder) {
        this.controllerLinkBuilder = controllerLinkBuilder;
    }

    public static AffordanceBuilder linkTo(Object methodInvocation) {
        final ControllerLinkBuilder controllerLinkBuilder = ControllerLinkBuilder.linkTo(methodInvocation);
        return new AffordanceBuilder(controllerLinkBuilder);
    }

    public static <T> T methodOn(Class<T> clazz, Object... parameters) {
        return ControllerLinkBuilder.methodOn(clazz, parameters);
    }


    public Affordance build(String... rels) {
        Assert.notEmpty(rels);
        final String link = controllerLinkBuilder.toString();
        final Affordance affordance = new Affordance(link, rels);
        for (Map.Entry<String, List<String>> linkParamEntry : linkParams.entrySet()) {
            final List<String> values = linkParamEntry.getValue();
            for (String value : values) {
                affordance.addLinkParam(linkParamEntry.getKey(), value);
            }
        }

        return affordance;
    }


    public AffordanceBuilder withTitle(String title) {
        this.linkParams.set("title", title);
        return this;
    }

    public AffordanceBuilder withTitleStar(String titleStar) {
        this.linkParams.set("title*", titleStar);
        return this;
    }

    public AffordanceBuilder withLinkParam(String name, String value) {
        this.linkParams.add(name, value);
        return this;
    }

    public AffordanceBuilder withAnchor(String anchor) {
        this.linkParams.set("anchor", anchor);
        return this;
    }

    public AffordanceBuilder withHreflang(String hreflang) {
        this.linkParams.add("hreflang", hreflang);
        return this;
    }

    public AffordanceBuilder withMedia(String media) {
        this.linkParams.set("media", media);
        return this;
    }


    public AffordanceBuilder withType(String type) {
        this.linkParams.set("type", type);
        return this;
    }
//    TODO/**
//     * Creates the {@link Affordance} built by the current builder instance with the default self rel.
//     *
//     * @return link
//     */
//    public Affordance withSelfRel() {
//        final Link link = controllerLinkBuilder.withSelfRel();
//        return new Affordance(link.getHref(), link.getRel());
//    }

}
