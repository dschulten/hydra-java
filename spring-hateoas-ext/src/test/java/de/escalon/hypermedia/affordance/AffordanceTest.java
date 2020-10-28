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

package de.escalon.hypermedia.affordance;

import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.hateoas.LinkRelation;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;

public class AffordanceTest {

    @Test
    public void testConstructorWithoutRels() {
        final Affordance affordance = new Affordance("http://localhost/things/{id}");
        assertEquals("http://localhost/things/{id}", affordance.getHref());
        Assert.assertNull("rel must be null", affordance.getRel());
        assertEquals(0, affordance.getRels()
                .size());
        Assert.assertThat(affordance.getRels(), Matchers.is(Matchers.empty()));
    }

    @Test
    public void testConstructorWithSingleRel() {
        final Affordance affordance = new Affordance("http://localhost/things/{id}", "thing");
        assertEquals("http://localhost/things/{id}", affordance.getHref());
        assertEquals(LinkRelation.of("thing"), affordance.getRel());
        Assert.assertThat(affordance.getRels(), Matchers.contains("thing"));
    }

    @Test
    public void testConstructorWithRels() {
        final Affordance affordance = new Affordance("http://localhost/things/{id}",
                "start", "http://example.net/relation/other");
        assertEquals("http://localhost/things/{id}", affordance.getHref());
        assertEquals(LinkRelation.of("start"), affordance.getRel());
        Assert.assertThat(affordance.getRels(), Matchers.contains("start", "http://example.net/relation/other"));
    }

    @Test
    public void testAffordanceAsHeader() {
        final Affordance affordance = new Affordance("http://localhost/things/{id}", "thing", "http://example.net/relation/other");
        assertEquals("<http://localhost/things/{id}>; rel=\"thing http://example.net/relation/other\"", affordance.asHeader());
    }

    @Test
    public void testIsTemplated() {
        final Affordance affordance = new Affordance("http://localhost/things/{id}", "thing", "http://example.net/relation/other");
        assertEquals("http://localhost/things/{id}", affordance.getHref());
        Assert.assertTrue("must recognize template", affordance.isTemplated());
    }

    @Test
    public void testGetVariables() {
        final Affordance affordance = new Affordance("http://localhost/things/{id}", "thing");
        Assert.assertThat(affordance.getVariableNames(), Matchers.contains("id"));
    }

    @Test
    public void testLinkExtensionParams() {
        final Affordance affordance = new Affordance("http://example.com");
        affordance.addLinkParam("name", "name-to-distinguish-links-with-same-rel");
        affordance.addLinkParam("deprecation", "http://example.com/why/this/is/deprecated");
        affordance.addLinkParam("type", "application/json");
        Affordance.DynaBean linkExtensions = affordance.getLinkExtensions();
        assertEquals("application/json", affordance.getType());
        assertEquals("must only contain link extension params",
                "{name=name-to-distinguish-links-with-same-rel, " +
                "deprecation=http://example.com/why/this/is/deprecated}", linkExtensions.toString());
    }

    @Test
    public void testExpand() {
        final Affordance affordance = new Affordance("http://localhost/things{/id}", "thing");
        assertEquals("http://localhost/things/100", affordance.expand(100)
                .getHref());
    }

    @Test
    public void testExpandWithArgumentsMap() {
        final Affordance affordance = new Affordance("http://localhost/things{?id}", "thing");

        Map<String, Object> arguments = new HashMap<String, Object>();
        arguments.put("id", 101);

        assertEquals("http://localhost/things?id=101", affordance.expand(101)
                .getHref());
    }

    @Test
    public void preservesSimpleStringVariables() {
        final Affordance affordance = new Affordance("/protected/res/documents/index.html?focus={contractId}&caller=BLUE&referrer=/protected/res/my_contracts/index.html&fragment=/contractDetails/{ref}", "thing");

        assertEquals("/protected/res/documents/index.html?focus={contractId}&caller=BLUE&referrer=/protected/res/my_contracts/index.html&fragment=/contractDetails/{ref}", affordance.getHref());

    }

    @Test
    public void expandsSimpleStringVariablesPartially() {
        final Affordance affordance = new Affordance("/protected/res/documents/index.html?focus={contractId}&caller=BLUE&referrer=/protected/res/my_contracts/index.html&fragment=/contractDetails/{ref}", "thing");

        Map<String, Object> args = new HashMap<String, Object>();
        args.put("ref", 1234567890);

        assertEquals("/protected/res/documents/index.html?focus={contractId}&caller=BLUE&referrer=/protected/res/my_contracts/index.html&fragment=/contractDetails/1234567890", affordance.expandPartially(args).getHref());

    }

}