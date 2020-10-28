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

package de.escalon.hypermedia.spring;

import de.escalon.hypermedia.action.Input;
import de.escalon.hypermedia.affordance.Affordance;
import org.junit.Before;
import org.junit.Test;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.CollectionModel;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockServletContext;
import org.springframework.stereotype.Controller;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.util.MultiValueMap;
import org.springframework.util.ReflectionUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class AffordanceBuilderFactoryTest {

    AffordanceBuilderFactory factory = new AffordanceBuilderFactory();

    private MockHttpServletRequest request;


    enum EventStatus {
        CANCELLED, SCHEDULED
    }

    /**
     * Sample controller. Created by dschulten on 11.09.2014.
     */
    @Controller
    @RequestMapping("/events")
    static class EventControllerSample {

        static class EventQbe {
            List<String> description = Collections.singletonList("concert");
            List<EventStatus> status = Collections.singletonList(EventStatus.SCHEDULED);

            public void setStatus(List<EventStatus> status) {
                this.status = status;
            }

            public List<EventStatus> getStatus() {
                return status;
            }

            public void setDescription(List<String> description) {
                this.description = description;
            }

            public List<String> getDescription() {
                return description;
            }

        }


        @RequestMapping(value = "/{eventId}", method = RequestMethod.GET)
        public
        @ResponseBody
        EntityModel<Object> getEvent(@PathVariable String eventId) {
            return null;
        }

        @RequestMapping(value = "/query", method = RequestMethod.GET)
        public
        @ResponseBody
        CollectionModel<Object> queryEvent(@Input EventQbe query) {
            return null;
        }

        @RequestMapping(value = "/simplequery", method = RequestMethod.GET)
        public
        @ResponseBody
        CollectionModel<Object> simpleQueryEvent(
            @RequestParam("q") String query,
            @RequestParam(value = "offset", defaultValue = "0") Long offset) {
            return null;
        }

        @RequestMapping(value = "/queryexcludes", method = RequestMethod.GET)
        public
        @ResponseBody
        CollectionModel<Object> queryEventWithExcludes(@Input(exclude = "status") EventQbe query) {
            return null;
        }

        @RequestMapping(value = "/queryincludes", method = RequestMethod.GET)
        public
        @ResponseBody
        CollectionModel<Object> queryEventWithIncludes(@Input(include = "status") EventQbe query) {
            return null;
        }


        @RequestMapping(value = "/querymap", method = RequestMethod.GET)
        public
        @ResponseBody
        CollectionModel<Object> queryEventByMap(@Input(include = "description", hidden = "status", readOnly = "donttouch")
                                          MultiValueMap<String, String> query) {
            return null;
        }


        @RequestMapping(value = "/wrongqueryinclude", method = RequestMethod.GET)
        public
        @ResponseBody
        CollectionModel<Object> queryEventWithWrongInclude(@Input(include = "foo") EventQbe query) {
            return null;
        }

        @RequestMapping(value = "/wrongqueryexclude", method = RequestMethod.GET)
        public
        @ResponseBody
        CollectionModel<Object> queryEventWithWrongExclude(@Input(exclude = "foo") EventQbe query) {
            return null;
        }
    }

    @Before
    public void setUp() {
        request = MockMvcRequestBuilders.get("http://example.com/")
                .buildRequest(new MockServletContext());
        final RequestAttributes requestAttributes = new ServletRequestAttributes(request);
        RequestContextHolder.setRequestAttributes(requestAttributes);
    }

    @Test
    public void testLinkToMethod() throws Exception {
        final Method getEventMethod = ReflectionUtils.findMethod(EventControllerSample.class, "getEvent", String.class);
        final Affordance affordance = factory.linkTo(getEventMethod, new Object[0])
                .rel("foo")
                .build();
        assertEquals("http://example.com/events/{eventId}", affordance.getHref());
    }

    @Test
    public void testLinkToMethodWithInputBean() throws Exception {
        final Method getEventMethod = ReflectionUtils.findMethod(EventControllerSample.class, "queryEvent",
                EventControllerSample.EventQbe.class);
        final Affordance affordance = factory.linkTo(getEventMethod, new Object[0])
                .rel("foo")
                .build();
        assertEquals("http://example.com/events/query{?description,status}", affordance.getHref());
    }

    @Test
    public void testLinkToMethodInvocation() throws Exception {
        final Affordance affordance = factory.linkTo(
            AffordanceBuilder.methodOn(EventControllerSample.class)
                .getEvent(null))
                .rel("foo")
                .build();
        assertEquals("http://example.com/events/{eventId}", affordance.getHref());
    }

    @Test
    public void testLinkToControllerClass() throws Exception {
        final Affordance affordance = factory.linkTo(EventControllerSample.class, new Object[0])
                .rel("foo")
                .build();
        assertEquals("http://example.com/events", affordance.getHref());
    }

    @Test
    public void testLinkToMethodNoArgsBuild() throws Exception {
        final Method getEventMethod = ReflectionUtils.findMethod(EventControllerSample.class, "getEvent", String.class);
        assert getEventMethod != null;
        final Affordance affordance = factory.linkTo(getEventMethod, new Object[0])
                .rel("foo")
                .build();
        assertEquals("http://example.com/events/{eventId}", affordance.getHref());
        assertEquals("foo", affordance.getRel().value());
    }

    @Test
    public void testLinkToMethodInvocationNoArgsBuild() throws Exception {

        final Affordance affordance = factory.linkTo(AffordanceBuilder.methodOn(EventControllerSample.class)
                .getEvent((String) null))
                .rel("foo")
                .build();
        assertEquals("http://example.com/events/{eventId}", affordance.getHref());
        assertEquals("foo", affordance.getRel().value());
    }

    @Test
    public void testLinkToMethodInvocationBeanInput() throws Exception {

        final Affordance affordance = factory.linkTo(AffordanceBuilder.methodOn(EventControllerSample.class)
                .queryEvent(null))
                .rel("foo")
                .build();
        assertEquals("http://example.com/events/query{?description,status}", affordance.getHref());
        assertEquals("foo", affordance.getRel().value());
    }

    @Test
    public void testLinkToMethodInvocationNamedRequestParam() throws Exception {

        final Affordance affordance = factory.linkTo(AffordanceBuilder.methodOn(EventControllerSample.class)
                .simpleQueryEvent(null, null))
                .rel("foo")
                .build();
        // href strips optional variables
        assertEquals("http://example.com/events/simplequery{?q}", affordance.getHref());
        // full uritemplate
        assertEquals("http://example.com/events/simplequery{?q,offset}",
                affordance.getUriTemplateComponents().toString());
        assertEquals("foo", affordance.getRel().value());
    }

    @Test
    public void testLinkToMethodInvocationNamedRequestParamWithValue() throws Exception {

        final Affordance affordance = factory.linkTo(AffordanceBuilder.methodOn(EventControllerSample.class)
                .simpleQueryEvent("foo", null))
                .rel("foo")
                .build();
        // href strips optional variables
        assertEquals("http://example.com/events/simplequery?q=foo", affordance.getHref());
        // full uritemplate
        assertEquals("http://example.com/events/simplequery?q=foo{&offset}",
                affordance.getUriTemplateComponents().toString());
        assertEquals("foo", affordance.getRel().value());
    }

    @Test
    public void testLinkToMethodInvocationNamedRequestParamWithAllValues() throws Exception {

        final Affordance affordance = factory.linkTo(AffordanceBuilder.methodOn(EventControllerSample.class)
                .simpleQueryEvent("foo", 2L))
                .rel("foo")
                .build();
        // href must not strip variables with values
        assertEquals("http://example.com/events/simplequery?q=foo&offset=2", affordance.getHref());
        // full uritemplate with all values
        assertEquals("http://example.com/events/simplequery?q=foo&offset=2",
                affordance.getUriTemplateComponents().toString());
        assertEquals("foo", affordance.getRel().value());
    }


    @Test
    public void testLinkToMethodInvocationBeanInputWithExcludes() throws Exception {

        final Affordance affordance = factory.linkTo(AffordanceBuilder.methodOn(EventControllerSample.class)
                .queryEventWithExcludes(null))
                .rel("foo")
                .build();
        assertEquals("http://example.com/events/queryexcludes{?description}", affordance.getHref());
        assertEquals("foo", affordance.getRel().value());
    }

    @Test
    public void testLinkToMethodInvocationBeanInputWithIncludes() throws Exception {

        final Affordance affordance = factory.linkTo(AffordanceBuilder.methodOn(EventControllerSample.class)
                .queryEventWithIncludes(null))
                .rel("foo")
                .build();
        assertEquals("http://example.com/events/queryincludes{?status}", affordance.getHref());
        assertEquals("foo", affordance.getRel().value());
    }

    @Test
    public void testLinkToMethodInvocationMapInput() throws Exception {

        final Affordance affordance = factory.linkTo(AffordanceBuilder.methodOn(EventControllerSample.class)
                .queryEventByMap(null))
                .rel("foo")
                .build();
        assertEquals("http://example.com/events/querymap{?description,status,donttouch}", affordance.getHref());
        assertEquals("foo", affordance.getRel().value());
    }

    @Test(expected = IllegalStateException.class)
    public void testLinkToMethodInvocationWrongBeanInclude() throws Exception {

        factory.linkTo(AffordanceBuilder.methodOn(EventControllerSample.class)
                .queryEventWithWrongInclude(null))
                .rel("bar")
                .build();

    }

    @Test(expected = IllegalStateException.class)
    public void testLinkToMethodInvocationWrongBeanExclude() throws Exception {

        factory.linkTo(AffordanceBuilder.methodOn(EventControllerSample.class)
                .queryEventWithWrongExclude(null))
                .rel("bar")
                .build();

    }

    @Test
    public void testLinkToControllerClassNoArgsBuild() throws Exception {
        final Affordance affordance = factory.linkTo(EventControllerSample.class, new Object[0])
                .rel("foo")
                .build();
        assertEquals("http://example.com/events", affordance.getHref());
        assertEquals("foo", affordance.getRel().value());
    }

    @Test
    public void testLinkToMethodInvocationReverseRel() throws Exception {

        final Affordance affordance = factory.linkTo(AffordanceBuilder.methodOn(EventControllerSample.class)
                .getEvent((String) null))
                .reverseRel("schema:parent", "ex:children")
                .build();
        assertEquals("http://example.com/events/{eventId}", affordance.getHref());
        assertEquals("schema:parent", affordance.getRev());
        assertEquals("ex:children", affordance.getRel().value());
    }
}