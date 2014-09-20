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

import org.hamcrest.Matchers;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.collection.IsIterableContainingInOrder.contains;
import static org.junit.Assert.*;

public class AffordanceTest {

    @Test
    public void testConstructorWithoutRels() {
        final Affordance affordance = new Affordance("http://localhost/things/{id}");
        assertEquals("http://localhost/things/{id}", affordance.getHref());
        assertNull("rel must be null", affordance.getRel());
        assertEquals(0, affordance.getRels()
                .size());
        assertThat(affordance.getRels(), is(empty()));
    }

    @Test
    public void testConstructorWithSingleRel() {
        final Affordance affordance = new Affordance("http://localhost/things/{id}", "thing");
        assertEquals("http://localhost/things/{id}", affordance.getHref());
        assertEquals("thing", affordance.getRel());
        assertThat(affordance.getRels(), contains("thing"));

    }

    @Test
    public void testConstructorWithRels() {
        final Affordance affordance = new Affordance("http://localhost/things/{id}",
                "start", "http://example.net/relation/other");
        assertEquals("http://localhost/things/{id}", affordance.getHref());
        assertEquals("start", affordance.getRel());
        assertThat(affordance.getRels(), contains("start", "http://example.net/relation/other"));
    }

    @Test
    public void testIsTemplated() {
        final Affordance affordance = new Affordance("http://localhost/things/{id}", "thing");
        assertEquals("http://localhost/things/{id}", affordance.getHref());
        assertTrue("must recognize template", affordance.isTemplated());

    }

    @Test
    public void testGetVariables() {
        final Affordance affordance = new Affordance("http://localhost/things/{id}", "thing");
        assertThat(affordance.getVariableNames(), contains("id"));

    }

    @Test
    public void testExpand() {
        final Affordance affordance = new Affordance("http://localhost/things{/id}", "thing");
        assertEquals("http://localhost/things/100", affordance.expand(100).getHref());

    }

    @Test
    public void testExpandWithArgumentsMap() {
        final Affordance affordance = new Affordance("http://localhost/things{?id}", "thing");

        Map<String, Object> arguments = new HashMap<String, Object>();
        arguments.put("id", 101);

        assertEquals("http://localhost/things?id=101", affordance.expand(101).getHref());
    }
}