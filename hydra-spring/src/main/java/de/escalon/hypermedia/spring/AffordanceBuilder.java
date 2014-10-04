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

import de.escalon.hypermedia.spring.action.ActionDescriptor;
import org.springframework.hateoas.Identifiable;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.core.AnnotationMappingDiscoverer;
import org.springframework.hateoas.core.DummyInvocationUtils;
import org.springframework.hateoas.core.LinkBuilderSupport;
import org.springframework.hateoas.core.MappingDiscoverer;
import org.springframework.util.Assert;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.web.util.UriTemplate;

import javax.servlet.http.HttpServletRequest;
import java.lang.reflect.Method;
import java.net.URI;
import java.util.List;
import java.util.Map;

/**
 * Builder for hypermedia affordances, usable as rfc-5988 web links and optionally holding information about request body requirements.
 * Created by dschulten on 07.09.2014.
 */
public class AffordanceBuilder extends LinkBuilderSupport<AffordanceBuilder> {

    private static final MappingDiscoverer DISCOVERER = new AnnotationMappingDiscoverer(RequestMapping.class);
    private static final AffordanceBuilderFactory FACTORY = new AffordanceBuilderFactory();
    private final ActionDescriptor actionDescriptor;

    private MultiValueMap<String, String> linkParams = new LinkedMultiValueMap<String, String>();

    /**
     * Creates a new {@link AffordanceBuilder} with a base of the mapping annotated to the given controller class.
     *
     * @param controller the class to discover the annotation on, must not be {@literal null}.
     * @return
     */
    public static AffordanceBuilder linkTo(Class<?> controller) {
        return linkTo(controller, new Object[0]);
    }

    /**
     * Creates a new {@link AffordanceBuilder} with a base of the mapping annotated to the given controller class. The
     * additional parameters are used to fill up potentially available path variables in the class scop request mapping.
     *
     * @param controller the class to discover the annotation on, must not be {@literal null}.
     * @param parameters additional parameters to bind to the URI template declared in the annotation, must not be
     *                   {@literal null}.
     * @return
     */
    public static AffordanceBuilder linkTo(Class<?> controller, Object... parameters) {

        Assert.notNull(controller);

        AffordanceBuilder builder = new AffordanceBuilder(getBuilder());
        String mapping = DISCOVERER.getMapping(controller);

        UriComponents uriComponents = UriComponentsBuilder.fromUriString(mapping == null ? "/" : mapping)
                .build();
        UriComponents expandedComponents = uriComponents.expand(parameters);

        return builder.slash(expandedComponents);
    }

    /*
     * @see org.springframework.hateoas.MethodLinkBuilderFactory#linkTo(Method, Object...)
     */
    public static AffordanceBuilder linkTo(Method method, Object... parameters) {
        return linkTo(method.getDeclaringClass(), method);
    }

    /*
     * @see org.springframework.hateoas.MethodLinkBuilderFactory#linkTo(Class<?>, Method, Object...)
     */
    public static AffordanceBuilder linkTo(Class<?> controller, Method method, Object... parameters) {

        Assert.notNull(controller, "Controller type must not be null!");
        Assert.notNull(method, "Method must not be null!");

        UriTemplate template = new UriTemplate(DISCOVERER.getMapping(controller, method));
        URI uri = template.expand(parameters);

        return new AffordanceBuilder(getBuilder()).slash(uri);
    }


    /**
     * Creates a new {@link AffordanceBuilder} using the given {@link UriComponentsBuilder}.
     *
     * @param builder must not be {@literal null}.
     */
    AffordanceBuilder(UriComponentsBuilder builder) {
        super(builder);
        this.actionDescriptor = null;
    }

    /**
     * Creates a new {@link AffordanceBuilder} using the given {@link UriComponentsBuilder}.
     *
     * @param builder          must not be {@literal null}
     * @param actionDescriptor must not be {@literal null}
     */
    public AffordanceBuilder(UriComponentsBuilder builder, ActionDescriptor actionDescriptor) {
        super(builder);
        Assert.notNull(actionDescriptor);
        this.actionDescriptor = actionDescriptor;
    }

    public static AffordanceBuilder linkTo(Object methodInvocation) {
        return FACTORY.linkTo(methodInvocation);
    }

    public static <T> T methodOn(Class<T> clazz, Object... parameters) {
        return DummyInvocationUtils.methodOn(clazz, parameters);
    }


    public Affordance build(String... rels) {
        Assert.notEmpty(rels);
        final Affordance affordance = new Affordance(this.toString(), actionDescriptor, rels);
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

    @Override
    protected AffordanceBuilder getThis() {
        return this;
    }

    @Override
    protected AffordanceBuilder createNewInstance(UriComponentsBuilder builder) {
        return new AffordanceBuilder(builder);
    }
//    TODO/**
//     * Creates the {@link Affordance} built by the current builder instance with the default self rel.
//     *
//     * @return link
//     */
//    public Affordance withSelfRel() {
//        final Link link = AffordanceBuilder.withSelfRel();
//        return new Affordance(link.getHref(), link.getRel());
//    }


    @Override
    public AffordanceBuilder slash(Object object) {
        return super.slash(object);
    }

    @Override
    public AffordanceBuilder slash(Identifiable<?> identifyable) {
        return super.slash(identifyable);
    }

    @Override
    public URI toUri() {
        return super.toUri();
    }

    @Override
    public Affordance withRel(String rel) {
        return build(rel);
    }

    @Override
    public Affordance withSelfRel() {
        return build(Link.REL_SELF);
    }

    @Override
    public String toString() {
        return super.toString();
    }

    /**
     * Returns a {@link UriComponentsBuilder} obtained from the current servlet mapping with the host tweaked in case the
     * request contains an {@code X-Forwarded-Host} header and the scheme tweaked in case the request contains an
     * {@code X-Forwarded-Ssl} header
     *
     * @return
     */
    static UriComponentsBuilder getBuilder() {

        HttpServletRequest request = getCurrentRequest();
        ServletUriComponentsBuilder builder = ServletUriComponentsBuilder.fromServletMapping(request);

        String forwardedSsl = request.getHeader("X-Forwarded-Ssl");

        if (StringUtils.hasText(forwardedSsl) && forwardedSsl.equalsIgnoreCase("on")) {
            builder.scheme("https");
        }

        String host = request.getHeader("X-Forwarded-Host");

        if (!StringUtils.hasText(host)) {
            return builder;
        }

        String[] hosts = StringUtils.commaDelimitedListToStringArray(host);
        String hostToUse = hosts[0];

        if (hostToUse.contains(":")) {

            String[] hostAndPort = StringUtils.split(hostToUse, ":");

            builder.host(hostAndPort[0]);
            builder.port(Integer.parseInt(hostAndPort[1]));

        } else {
            builder.host(hostToUse);
            builder.port(-1); // reset port if it was forwarded from default port
        }

        String port = request.getHeader("X-Forwarded-Port");

        if (StringUtils.hasText(port)) {
            builder.port(Integer.parseInt(port));
        }

        return builder;
    }

    /**
     * Copy of {@link ServletUriComponentsBuilder#getCurrentRequest()} until SPR-10110 gets fixed.
     *
     * @return
     */
    private static HttpServletRequest getCurrentRequest() {

        RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
        Assert.state(requestAttributes != null, "Could not find current request via RequestContextHolder");
        Assert.isInstanceOf(ServletRequestAttributes.class, requestAttributes);
        HttpServletRequest servletRequest = ((ServletRequestAttributes) requestAttributes).getRequest();
        Assert.state(servletRequest != null, "Could not find current HttpServletRequest");
        return servletRequest;
    }

}
