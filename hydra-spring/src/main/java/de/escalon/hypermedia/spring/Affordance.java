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
import org.springframework.hateoas.Link;
import org.springframework.http.HttpMethod;
import org.springframework.util.Assert;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Represents an http affordance for purposes of a ReST service as described by
 * <a href="http://tools.ietf.org/html/rfc5988">Web Linking rfc5988</a>. Additionally includes hints for the http method
 * and expected request body.
 * <p/>
 * Created by dschulten on 07.09.2014.
 */
public class Affordance extends Link {

    private final ActionDescriptor actionDescriptor;
    private MultiValueMap<String, String> linkParams = new LinkedMultiValueMap<String, String>();
    private HttpMethod httpMethod;
    // TODO: ideally the Resource content can be used to describe the supported properties, their type etc.
    // the event resource would define which properties are editable and we could derive the template from it
    // maybe affordance could hold the bean for which it is an affordance?

    public Affordance(String uriTemplate, String... rels) {
        super(uriTemplate);
        Assert.noNullElements(rels, "null rels are not allowed");
        for (String rel : rels) {
            addRel(rel);
        }
        this.actionDescriptor = null;
    }

    public Affordance(String uriTemplate, ActionDescriptor actionDescriptor, String... rels) {
        super(uriTemplate);
        Assert.notNull(actionDescriptor);
        Assert.noNullElements(rels, "null rels are not allowed");
        for (String rel : rels) {
            addRel(rel);
        }
        this.actionDescriptor = actionDescriptor;
    }

    private Affordance(String uriTemplate, MultiValueMap<String, String> linkParams, HttpMethod httpMethod,
                       ActionDescriptor actionDescriptor) {
        super(uriTemplate);
        this.linkParams = linkParams;
        this.httpMethod = httpMethod;
        this.actionDescriptor = actionDescriptor;
    }


    public Affordance(String href) {
        super(href);
        this.actionDescriptor = null;
    }

    public HttpMethod getHttpMethod() {
        return httpMethod;
    }

    public void setHttpMethod(HttpMethod httpMethod) {
        this.httpMethod = httpMethod;
    }

    public ActionDescriptor getActionDescriptor() {
        return actionDescriptor;
    }
    /**
     * The relation type of the link.
     *
     * @param rel IANA-registered type or extension relation type.
     */
    public void addRel(String rel) {
        Assert.hasLength(rel);
        linkParams.add("rel", rel);
    }

    /**
     * The "type" parameter, when present, is a hint indicating what the
     * media type of the result of dereferencing the link should be.  Note
     * that this is only a hint; for example, it does not override the
     * Content-Type header of a HTTP response obtained by actually following
     * the link.  There MUST NOT be more than one type parameter in a link-
     * value.
     *
     * @param mediaType to set
     */
    public void setType(String mediaType) {
        if (mediaType != null)
            linkParams.set("type", mediaType);
        else
            linkParams.remove("type");
    }

    /**
     * The "hreflang" parameter, when present, is a hint indicating what the
     * language of the result of dereferencing the link should be.  Note
     * that this is only a hint; for example, it does not override the
     * Content-Language header of a HTTP response obtained by actually
     * following the link.  Multiple "hreflang" parameters on a single link-
     * value indicate that multiple languages are available from the
     * indicated resource.
     *
     * @param hreflang to add
     */
    public void addHreflang(String hreflang) {
        Assert.hasLength(hreflang);
        linkParams.add("hreflang", hreflang);
    }

    /**
     * The "title" parameter, when present, is used to label the destination
     * of a link such that it can be used as a human-readable identifier
     * (e.g., a menu entry) in the language indicated by the Content-
     * Language header (if present).  The "title" parameter MUST NOT appear
     * more than once in a given link-value; occurrences after the first
     * MUST be ignored by parsers.
     *
     * @param title to set
     */
    public void setTitle(String title) {
        if (title != null)
            linkParams.set("title", title);
        else {
            linkParams.remove("title");
        }
    }

    /**
     * The "title*" parameter can be used to encode this label in a
     * different character set, and/or contain language information as per
     * [RFC5987].  The "title*" parameter MUST NOT appear more than once in
     * a given link-value; occurrences after the first MUST be ignored by
     * parsers.  If the parameter does not contain language information, its
     * language is indicated by the Content-Language header (when present).
     *
     * @param titleStar to set
     */
    public void setTitleStar(String titleStar) {
        if (titleStar != null)
            linkParams.set("title*", titleStar);
        else
            linkParams.remove("title*");
    }

    /**
     * The "media" parameter, when present, is used to indicate intended
     * destination medium or media for style information (see
     * [W3C.REC-html401-19991224], Section 6.13).  Note that this may be
     * updated by [W3C.CR-css3-mediaqueries-20090915]).  Its value MUST be
     * quoted if it contains a semicolon (";") or comma (","), and there
     * MUST NOT be more than one "media" parameter in a link-value.
     *
     * @param mediaDesc to set
     */
    public void setMedia(String mediaDesc) {
        if (mediaDesc != null)
            linkParams.set("media", mediaDesc);
        else
            linkParams.remove("media");
    }

    /**
     * The "rev" parameter has been used in the past to indicate that the
     * semantics of the relationship are in the reverse direction.  That is,
     * a link from A to B with REL="X" expresses the same relationship as a
     * link from B to A with REV="X". "rev" is deprecated by this
     * specification because it often confuses authors and readers; in most
     * cases, using a separate relation type is preferable.
     *
     * @param rev to set
     */
    public void setRev(String rev) {
        if (rev != null)
            linkParams.set("rev", rev);
        else
            linkParams.remove("rev");
    }

    /**
     * By default, the context of a link conveyed in the Link header field
     * is the IRI of the requested resource.
     * When present, the anchor parameter overrides this with another URI,
     * such as a fragment of this resource, or a third resource (i.e., when
     * the anchor value is an absolute URI).  If the anchor parameter's
     * value is a relative URI, parsers MUST resolve it as per [RFC3986],
     * Section 5.  Note that any base URI from the body's content is not
     * applied.
     *
     * @param anchor base uri to define
     */
    public void setAnchor(String anchor) {
        if (anchor != null)
            linkParams.set("anchor", anchor);
        else
            linkParams.remove("anchor");
    }

    /**
     * Adds link-extension params.
     *
     * @param paramName of link-extension
     * @param values    one or more values to add
     */
    public void addLinkParam(String paramName, String... values) {
        Assert.notEmpty(values);
        for (String value : values) {
            Assert.hasLength(value);
            linkParams.add(paramName, value);
        }
    }

    public String toLinkHeader() {
        // TODO do not render templated as linkHeader, have flag isWebLink() which tells if the link can be rendered
        // as link header, throw exception otherwise
        String linkHeader = "Link: <" + getHref() + ">; ";
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, List<String>> linkParamEntry : linkParams.entrySet()) {
            if (sb.length() != 0) {
                sb.append("; ");
            }
            if ("rel".equals(linkParamEntry.getKey())) {
                sb.append(linkParamEntry.getKey())
                        .append("=");
                sb.append("\"")
                        .append(StringUtils.collectionToDelimitedString(linkParamEntry.getValue(), " "))
                        .append("\"");
            } else {
                StringBuilder linkParams = new StringBuilder();
                for (String value : linkParamEntry.getValue()) {
                    if (linkParams.length() != 0) {
                        linkParams.append("; ");
                    }
                    linkParams.append(linkParamEntry.getKey())
                            .append("=");
                    linkParams.append("\"")
                            .append(value)
                            .append("\"");
                }
                sb.append(linkParams);

            }

        }
        return sb.insert(0, linkHeader)
                .toString();
    }

    @Override
    public String toString() {
        return toLinkHeader();
    }

    @Override
    public Affordance withRel(String rel) {
        linkParams.set("rel", rel);
        return new Affordance(this.getHref(), linkParams, httpMethod, actionDescriptor);
    }

    @Override
    public Affordance withSelfRel() {
        if (!linkParams.get("rel")
                .contains(Link.REL_SELF)) {
            linkParams.add("rel", Link.REL_SELF);
        }
        return new Affordance(this.getHref(), linkParams, httpMethod, actionDescriptor);
    }

    @Override
    public Affordance expand(Object... arguments) {
        return new Affordance(super.expand(arguments).getHref(), linkParams, httpMethod, actionDescriptor);
    }

    @Override
    public Affordance expand(Map<String, ? extends Object> arguments) {
        return new Affordance(super.expand(arguments).getHref(), linkParams, httpMethod, actionDescriptor);
    }

    public List<String> getRels() {
        final List<String> rels = linkParams.get("rel");
        return rels == null ? Collections.<String>emptyList() : rels;
    }

    @Override
    public String getRel() {
        return linkParams.getFirst("rel");
    }


}
