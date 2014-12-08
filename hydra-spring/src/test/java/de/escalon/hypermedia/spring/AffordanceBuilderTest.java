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
import de.escalon.hypermedia.spring.action.ActionInputParameter;
import de.escalon.hypermedia.spring.de.escalon.hypermedia.spring.sample.EventStatusType;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockServletContext;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

public class AffordanceBuilderTest {

    private MockHttpServletRequest request;

    @Before
    public void setUp() {
        request = MockMvcRequestBuilders.get("http://example.com/")
                .buildRequest(new MockServletContext());
        final RequestAttributes requestAttributes = new ServletRequestAttributes(request);
        RequestContextHolder.setRequestAttributes(requestAttributes);
    }

    public static class Thing {

    }

    public static class DummyController {

        @RequestMapping("/things")
        public ResponseEntity createThing(@RequestBody Thing thing) {
            return new ResponseEntity(HttpStatus.CREATED);
        }

        @RequestMapping(value = "/things/{id}/eventStatus", method = RequestMethod.PUT)
        public ResponseEntity updateThing(@PathVariable int id, @RequestParam EventStatusType eventStatus) {
            return new ResponseEntity(HttpStatus.OK);
        }

        @RequestMapping(value = "/things/{id}", method = RequestMethod.PUT)
        public ResponseEntity updateThing(@PathVariable int id, @RequestBody Thing thing) {
            return new ResponseEntity(HttpStatus.OK);
        }

    }

    @Test
    public void testWithSingleRel() throws Exception {
        final Affordance affordance = AffordanceBuilder.linkTo(AffordanceBuilder.methodOn(DummyController.class)
                .createThing(new Thing()))
                .build("next");
        assertEquals("Link: <http://example.com/things>; rel=\"next\"", affordance.toString());
    }

    @Test
    public void testWithTitle() {
        final Affordance affordance = AffordanceBuilder.linkTo(AffordanceBuilder.methodOn(DummyController.class)
                .createThing(new Thing()))
                .withTitle("my-title")
                .build("next");
        assertEquals("Link: <http://example.com/things>; rel=\"next\"; title=\"my-title\"",
                affordance.toString());
    }

    @Test
    public void testWithTitleStar() {
        final Affordance affordance = AffordanceBuilder.linkTo(AffordanceBuilder.methodOn(DummyController.class)
                .createThing(new Thing()))
                .withTitleStar("UTF-8'de'n%c3%a4chstes%20Kapitel")
                .build("next");
        assertEquals("Link: <http://example.com/things>; rel=\"next\"; title*=\"UTF-8'de'n%c3%a4chstes%20Kapitel\"",
                affordance.toString());
    }

    @Test
    public void testWithAnchor() {
        final Affordance affordance = AffordanceBuilder.linkTo(AffordanceBuilder.methodOn(DummyController.class)
                .createThing(new Thing()))
                .withAnchor("http://api.example.com/api")
                .build("next");
        assertEquals("Link: <http://example.com/things>; rel=\"next\"; anchor=\"http://api.example.com/api\"",
                affordance.toString());
    }

    @Test
    public void testWithType() {
        final Affordance affordance = AffordanceBuilder.linkTo(AffordanceBuilder.methodOn(DummyController.class)
                .createThing(new Thing()))
                .withType("application/pdf")
                .build("next");
        assertEquals("Link: <http://example.com/things>; rel=\"next\"; type=\"application/pdf\"",
                affordance.toString());
    }

    @Test
    public void testWithMedia() {
        final Affordance affordance = AffordanceBuilder.linkTo(AffordanceBuilder.methodOn(DummyController.class)
                .createThing(new Thing()))
                .withMedia("qhd")
                .build("next");
        assertEquals("Link: <http://example.com/things>; rel=\"next\"; media=\"qhd\"",
                affordance.toString());
    }

    @Test
    public void testWithHreflang() {
        final Affordance affordance = AffordanceBuilder.linkTo(AffordanceBuilder.methodOn(DummyController.class)
                .createThing(new Thing()))
                .withHreflang("en-us")
                .withHreflang("de")
                .build("next");
        assertEquals("Link: <http://example.com/things>; rel=\"next\"; hreflang=\"en-us\"; hreflang=\"de\"",
                affordance.toString());
    }

    @Test
    public void testWithLinkParam() {
        final Affordance affordance = AffordanceBuilder.linkTo(AffordanceBuilder.methodOn(DummyController.class)
                .createThing(new Thing()))
                .withLinkParam("param1", "foo")
                .withLinkParam("param1", "bar")
                .withLinkParam("param2", "baz")
                .build("next");
        assertEquals("Link: <http://example.com/things>; rel=\"next\"; param1=\"foo\"; param1=\"bar\"; param2=\"baz\"",
                affordance.toString());
    }

    @Test
    public void testActionDescriptorForRequestParams() {
        final Affordance affordance = AffordanceBuilder.linkTo(AffordanceBuilder.methodOn(DummyController.class)
                .updateThing(1, (EventStatusType)null))
                .build("eventStatus");
        assertEquals("Link-Template: <http://example.com/things/1/eventStatus{?eventStatus}>; rel=\"eventStatus\"",
                affordance.toString());
        final ActionDescriptor actionDescriptor = affordance.getActionDescriptor();
        assertThat((EventStatusType[]) actionDescriptor.getActionInputParameter("eventStatus")
                        .getPossibleValues(actionDescriptor),
                Matchers.arrayContainingInAnyOrder(
                        EventStatusType.EVENT_CANCELLED,
                        EventStatusType.EVENT_POSTPONED,
                        EventStatusType.EVENT_RESCHEDULED,
                        EventStatusType.EVENT_SCHEDULED));
        assertEquals("updateThing", actionDescriptor.getActionName());
    }

    @Test
    public void testActionDescriptorForRequestBody() {
        final Affordance affordance = AffordanceBuilder.linkTo(AffordanceBuilder.methodOn(DummyController.class)
                .updateThing(1, (Thing)null))
                .build("event");
        assertEquals("Link: <http://example.com/things/1>; rel=\"event\"",
                affordance.toString());
        final ActionDescriptor actionDescriptor = affordance.getActionDescriptor();
        final ActionInputParameter thingParameter = actionDescriptor.getRequestBody();
        assertEquals("Thing", ((Class)thingParameter.getGenericParameterType()).getSimpleName());
        assertThat(thingParameter.isRequestBody(), is(true));
        // TODO use rel as action name, remove action name from ActionDescriptor
        assertEquals("updateThing", actionDescriptor.getActionName());
    }


    @Test
    public void testBuild() throws Exception {
        final Affordance affordance = AffordanceBuilder.linkTo(AffordanceBuilder.methodOn(DummyController.class)
                .createThing(new Thing()))
                .build("next", "thing");
        assertEquals("Link: <http://example.com/things>; rel=\"next thing\"", affordance.toString());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testRejectsEmptyRel() throws Exception {
        final Affordance affordance = AffordanceBuilder.linkTo(AffordanceBuilder.methodOn(DummyController.class)
                .createThing(new Thing()))
                .build("");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testRejectsEmptyRels() throws Exception {
        final Affordance affordance = AffordanceBuilder.linkTo(AffordanceBuilder.methodOn(DummyController.class)
                .createThing(new Thing()))
                .build(new String[0]);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testRejectsMissingRel() throws Exception {
        final Affordance affordance = AffordanceBuilder.linkTo(AffordanceBuilder.methodOn(DummyController.class)
                .createThing(new Thing()))
                .build();
    }


    @Test(expected = IllegalArgumentException.class)
    public void testRejectsNullRel() throws Exception {
        final Affordance affordance = AffordanceBuilder.linkTo(AffordanceBuilder.methodOn(DummyController.class)
                .createThing(new Thing()))
                .build((String)null);
    }

}