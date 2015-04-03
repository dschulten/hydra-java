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

import de.escalon.hypermedia.action.ActionDescriptor;
import org.springframework.hateoas.Identifiable;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.LinkBuilder;
import org.springframework.hateoas.core.AnnotationMappingDiscoverer;
import org.springframework.hateoas.core.DummyInvocationUtils;
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
import org.springframework.web.util.UriComponentsBuilder;

import javax.servlet.http.HttpServletRequest;
import java.lang.reflect.Method;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Builder for hypermedia affordances, usable as rfc-5988 web links and optionally holding information about request body requirements.
 * Created by dschulten on 07.09.2014.
 */
public class AffordanceBuilder implements LinkBuilder {

    private static final MappingDiscoverer DISCOVERER = new AnnotationMappingDiscoverer(RequestMapping.class);
    private static final AffordanceBuilderFactory FACTORY = new AffordanceBuilderFactory();

    private UriTemplateComponents uriTemplateComponents;
    private List<ActionDescriptor> actionDescriptors = new ArrayList<ActionDescriptor>();

    private MultiValueMap<String, String> linkParams = new LinkedMultiValueMap<String, String>();

    /**
     * Creates a new {@link AffordanceBuilder} with a base of the mapping annotated to the given controller class.
     *
     * @param controller the class to discover the annotation on, must not be {@literal null}.
     * @return builder
     */
    public static AffordanceBuilder linkTo(Class<?> controller) {
        return linkTo(controller, new Object[0]);
    }

    /**
     * Creates a new {@link AffordanceBuilder} with a base of the mapping annotated to the given controller class. The
     * additional parameters are used to fill up potentially available path variables in the class scope request mapping.
     *
     * @param controller the class to discover the annotation on, must not be {@literal null}.
     * @param parameters additional parameters to bind to the URI template declared in the annotation, must not be
     *                   {@literal null}.
     * @return builder
     */
    public static AffordanceBuilder linkTo(Class<?> controller, Object... parameters) {
        return FACTORY.linkTo(controller, parameters);
    }

    /*
     * @see org.springframework.hateoas.MethodLinkBuilderFactory#linkTo(Method, Object...)
     */
    public static AffordanceBuilder linkTo(Method method, Object... parameters) {
        return linkTo(method.getDeclaringClass(), method, parameters);
    }

    /*
     * @see org.springframework.hateoas.MethodLinkBuilderFactory#linkTo(Class<?>, Method, Object...)
     */
    public static AffordanceBuilder linkTo(Class<?> controller, Method method, Object... parameters) {
        return FACTORY.linkTo(controller, method, parameters);
    }


    /**
     * Creates a new {@link AffordanceBuilder} pointing to this server, but without ActionDescriptor.
     */
    AffordanceBuilder() {
        this(new PartialUriTemplate(getBuilder().build()
                        .toString()).expand(Collections.<String, Object>emptyMap()),
                Collections.<ActionDescriptor>emptyList());

    }

    /**
     * Creates a new {@link AffordanceBuilder} using the given {@link ActionDescriptor}.
     *
     * @param uriTemplateComponents must not be {@literal null}
     * @param actionDescriptors     must not be {@literal null}
     */
    public AffordanceBuilder(UriTemplateComponents uriTemplateComponents, List<ActionDescriptor> actionDescriptors) {

        Assert.notNull(uriTemplateComponents);
        Assert.notNull(actionDescriptors);

        this.uriTemplateComponents = uriTemplateComponents;

        for (ActionDescriptor actionDescriptor : actionDescriptors) {
            this.actionDescriptors.add(actionDescriptor);
        }
    }

    public static AffordanceBuilder linkTo(Object methodInvocation) {
        return FACTORY.linkTo(methodInvocation);
    }

    public static <T> T methodOn(Class<T> clazz, Object... parameters) {
        return DummyInvocationUtils.methodOn(clazz, parameters);
    }


    /**
     * Builds affordance with multiple rels. According to rfc-5988, a link can have multiple link relation types.
     *
     * &quot;Note that link-values can convey multiple links between the same
     * target and context IRIs; for example:
     * <pre>
     * Link: &lt;http://example.org/&gt;
     *       rel="start http://example.net/relation/other"
     * </pre>
     * Here, the link to 'http://example.org/' has the registered relation
     * type 'start' and the extension relation type
     * 'http://example.net/relation/other'.&quot;
     *
     * @param rels link relation types
     * @return affordance
     * @see <a href="https://tools.ietf.org/html/rfc5988#section-5.5">Web Linking Examples</a>
     */
    public Affordance build(String... rels) {
        Assert.notEmpty(rels);
        final Affordance affordance;
        affordance = new Affordance(new PartialUriTemplate(this.toString()), actionDescriptors, rels);
        for (Map.Entry<String, List<String>> linkParamEntry : linkParams.entrySet()) {
            final List<String> values = linkParamEntry.getValue();
            for (String value : values) {
                affordance.addLinkParam(linkParamEntry.getKey(), value);
            }
        }
        //affordance.setActionDescriptors(actionDescriptors);
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

    /**
     * Allows to define link header params (not UriTemplate variables).
     *
     * @param name  of the link header param
     * @param value of the link header param
     * @return builder
     */
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
    public AffordanceBuilder slash(Object object) {

        if (object == null) {
            return this;
        }

        if (object instanceof Identifiable) {
            return slash((Identifiable<?>) object);
        }

        String urlPart = object.toString();

        // make sure one cannot delete the fragment
        if (urlPart.endsWith("#")) {
            urlPart = urlPart.substring(0, urlPart.length() - 1);
        }

        if (!StringUtils.hasText(urlPart)) {
            return this;
        }

        final UriTemplateComponents urlPartComponents = new PartialUriTemplate(urlPart).expand(Collections.<String, Object>emptyMap());
        final UriTemplateComponents affordanceComponents = uriTemplateComponents;

        final String path = !affordanceComponents.getBaseUri()
                .endsWith("/") && !urlPartComponents.getBaseUri()
                .startsWith("/") ?
                affordanceComponents.getBaseUri() + "/" + urlPartComponents.getBaseUri() :
                affordanceComponents.getBaseUri() + urlPartComponents.getBaseUri();
        final String queryHead = affordanceComponents.getQueryHead() +
                (StringUtils.hasText(urlPartComponents.getQueryHead()) ?
                        "&" + urlPartComponents.getQueryHead()
                                .substring(1) :
                        "");
        final String queryTail = affordanceComponents.getQueryTail() +
                (StringUtils.hasText(urlPartComponents.getQueryTail()) ?
                        "," + urlPartComponents.getQueryTail() :
                        "");
        final String fragmentIdentifier = StringUtils.hasText(urlPartComponents.getFragmentIdentifier()) ?
                urlPartComponents.getFragmentIdentifier() :
                affordanceComponents.getFragmentIdentifier();

        final UriTemplateComponents mergedUriComponents =
                new UriTemplateComponents(path, queryHead, queryTail, fragmentIdentifier);

        return new AffordanceBuilder(mergedUriComponents, actionDescriptors);

    }

    @Override
    public AffordanceBuilder slash(Identifiable<?> identifiable) {
        if (identifiable == null) {
            return this;
        }

        return slash(identifiable.getId());
    }

    @Override
    public URI toUri() {
        final String actionLink = uriTemplateComponents.toString();
        if (actionLink == null || actionLink.contains("{")) {
            throw new IllegalStateException("cannot convert template to URI");
        }
        return UriComponentsBuilder.fromUriString(actionLink
                .toString())
                .build()
                .toUri();
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
        return uriTemplateComponents.toString();
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

    public AffordanceBuilder and(AffordanceBuilder affordanceBuilder) {
        for (ActionDescriptor actionDescriptor : affordanceBuilder.actionDescriptors) {
            this.actionDescriptors.add(actionDescriptor);
        }
        return this;
    }


}
