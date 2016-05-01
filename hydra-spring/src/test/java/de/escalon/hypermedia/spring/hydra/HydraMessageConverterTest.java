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

import de.escalon.hypermedia.spring.HypermediaTypes;
import de.escalon.hypermedia.spring.sample.test.DummyEventControllerExposed;
import de.escalon.hypermedia.spring.sample.test.ReviewController;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.AnnotationConfigWebContextLoader;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.HandlerExceptionResolver;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;
import org.springframework.web.servlet.mvc.method.annotation.ExceptionHandlerExceptionResolver;

import java.util.List;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

/**
 * Tests Spring mvc message converter for hydra. Created by dschulten on 11.09.2014.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@WebAppConfiguration
@ContextConfiguration(loader = AnnotationConfigWebContextLoader.class)
public class HydraMessageConverterTest {

    public static final Logger LOG = LoggerFactory.getLogger(HydraMessageConverterTest.class);

    @Configuration
    @EnableWebMvc
    static class WebConfig extends WebMvcConfigurerAdapter {


        @Bean
        public ReviewController reviewController() {
            return new ReviewController();
        }

        @Bean
        public DummyEventControllerExposed eventController() {
            return new DummyEventControllerExposed();
        }

        @Override
        public void configureMessageConverters(List<HttpMessageConverter<?>> converters) {
            super.configureMessageConverters(converters);
            converters.add(new HydraMessageConverter());
        }

        @Override
        public void configureHandlerExceptionResolvers(List<HandlerExceptionResolver> exceptionResolvers) {
            final ExceptionHandlerExceptionResolver resolver = new ExceptionHandlerExceptionResolver();
            resolver.setWarnLogCategory(resolver.getClass()
                    .getName());
            exceptionResolvers.add(resolver);
        }

    }

    @Autowired
    private WebApplicationContext wac;

    private MockMvc mockMvc;

    @Before
    public void setUp() {
        this.mockMvc = webAppContextSetup(this.wac).build();
    }

    @Test
    public void convertsResource() throws Exception {
        final MvcResult result = this.mockMvc.perform(MockMvcRequestBuilders.get("/events/1")
                .accept(HypermediaTypes.APPLICATION_JSONLD))
                .andExpect(MockMvcResultMatchers.status()
                        .isOk())
                .andExpect(content().contentType("application/ld+json"))
                .andExpect(jsonPath("$.@type").value("Event"))
                .andExpect(jsonPath("$.performer").value("Cornelia Bielefeldt"))
                .andExpect(jsonPath("$.review.@id").value("http://localhost/reviews"))
                .andReturn();
        LOG.debug(result.getResponse()
                .getContentAsString());
    }

    @Test
    public void convertsQueryParamToObject() throws Exception {
        final MvcResult result = this.mockMvc.perform(MockMvcRequestBuilders.get
                ("/events/query?foo1=1&foo2=2&offset=5&size=20&bar.baz=4&strings=a&strings=b&bars[0].baz=42&bars[1]" +
                        ".baz=8")
                .accept(HypermediaTypes.APPLICATION_JSONLD))
                .andExpect(MockMvcResultMatchers.status()
                        .isOk())
//                .andExpect(jsonPath("$['hydra:member'][0].pageable.offset").value(5))
//                .andExpect(jsonPath("$['hydra:member'][0].pageable.size").value(20))
//                .andExpect(jsonPath("$.['hydra:member'][0]['hydra:collection'][0]['hydra:search']['hydra:template
// ']").value
//                        ("http://localhost/events/query{?offset,size,foo1,foo2}"))
                .andReturn();
        LOG.debug(result.getResponse()
                .getContentAsString());
    }

    @Test
    public void convertsResourceSupport() throws Exception {
        final MvcResult result = this.mockMvc.perform(MockMvcRequestBuilders.get("/events/resourcesupport/1")
                .accept(HypermediaTypes.APPLICATION_JSONLD))
                .andExpect(MockMvcResultMatchers.status()
                        .isOk())
                .andExpect(content().contentType("application/ld+json"))
                .andExpect(jsonPath("$.@type").value("Event"))
                .andExpect(jsonPath("$.performer").value("Cornelia Bielefeldt"))
                .andExpect(jsonPath("$.review.@id").value("http://localhost/reviews"))
                .andReturn();
        LOG.debug(result.getResponse()
                .getContentAsString());
    }

    @Test
    public void convertsListOfResourceOfEvent() throws Exception {
        final MvcResult result = this.mockMvc.perform(MockMvcRequestBuilders.get("/events/list")
                .accept(HypermediaTypes.APPLICATION_JSONLD))
                .andExpect(MockMvcResultMatchers.status()
                        .isOk())
                .andExpect(jsonPath("$.[0].@type").value("Event"))
                .andExpect(jsonPath("$.[0].performer").value("Walk off the Earth"))
                .andExpect(jsonPath("$.[0].['hydra:collection'][0].@id").value("http://localhost/reviews/events/1"))
                .andReturn();
        LOG.debug(result.getResponse()
                .getContentAsString());
    }

    @Test
    public void convertsResources() throws Exception {
        MvcResult result = null;
        result = this.mockMvc.perform(MockMvcRequestBuilders.get("/events")
                .accept(HypermediaTypes.APPLICATION_JSONLD))
                .andExpect(MockMvcResultMatchers.status()
                        .isOk())
                .andExpect(content().contentType("application/ld+json"))
                .andExpect(jsonPath("$.@type").value("hydra:Collection"))
                .andExpect(jsonPath("$.['hydra:member'][0].@type").value("Event"))
                .andExpect(jsonPath("$.['hydra:member'][0].@id").value("http://localhost/events/1"))
                .andExpect(jsonPath("$.['hydra:member'][0].performer").value("Walk off the Earth"))
                .andExpect(jsonPath("$.['hydra:member'][0].workPerformed.['hydra:collection'][0].@id").value
                        ("http://localhost/reviews/events/1"))
                .andExpect(jsonPath("$.['hydra:member'][1].@type").value("Event"))
                .andExpect(jsonPath("$.['hydra:member'][1].@id").value("http://localhost/events/2"))
                .andExpect(jsonPath("$.['hydra:member'][1].performer").value("Cornelia Bielefeldt"))
                .andExpect(jsonPath("$.['hydra:member'][1].workPerformed.['hydra:collection'][0].@id").value
                        ("http://localhost/reviews/events/2"))
                .andReturn();
        System.out.println(result.getResponse()
                .getContentAsString());
        LOG.debug(result.getResponse()
                .getContentAsString());
    }

    @Test
    public void convertsLinkToPost() throws Exception {
        MvcResult result = this.mockMvc.perform(MockMvcRequestBuilders.get("/events")
                .accept(HypermediaTypes.APPLICATION_JSONLD))
                .andExpect(MockMvcResultMatchers.status()
                        .isOk())
                .andExpect(content().contentType("application/ld+json"))
                .andExpect(jsonPath("$.['hydra:member'][0].workPerformed.['hydra:collection'][0].@id")
                        .value("http://localhost/reviews/events/1"))
                .andExpect(jsonPath("$.['hydra:member'][0].workPerformed.['hydra:collection'][0].['hydra:operation']" +
                        ".[0]" +
                        ".['hydra:method']").value("POST"))
                .andExpect(jsonPath("$.['hydra:member'][0].workPerformed.['hydra:collection'][0].['hydra:operation']" +
                        ".[0]" +
                        ".['hydra:expects'].['hydra:supportedProperty'].[?(@.['hydra:property']=='reviewBody')]")
                        .exists())
                .andExpect(jsonPath("$.['hydra:member'][0].workPerformed.['hydra:collection'][0].['hydra:operation']" +
                        ".[0]" +
                        ".['hydra:expects'].['hydra:supportedProperty'].[?(@.['hydra:property']=='reviewBody')]" +
                        ".['valuePattern']")
                        .value(".{10,}"))
                .andExpect(jsonPath("$.['hydra:member'][0].workPerformed.['hydra:collection'][0].['hydra:operation']" +
                        ".[0]" +
                        ".['hydra:expects'].['hydra:supportedProperty'].[?(@.['hydra:property']=='reviewRating')]")
                        .exists())
                .andExpect(jsonPath("$.['hydra:member'][0].workPerformed.['hydra:collection'][0].['hydra:operation']" +
                        ".[0]" +
                        ".['hydra:expects'].['hydra:supportedProperty'].[?(@.['hydra:property']=='reviewRating')]" +
                        ".['rangeIncludes']" +
                        ".['hydra:supportedProperty'][0].['hydra:property']").value("ratingValue"))
                .andExpect(jsonPath("$.['hydra:member'][0].workPerformed.['hydra:collection'][0].['hydra:operation']" +
                        ".[0]" +
                        ".['hydra:expects'].['hydra:supportedProperty'].[?(@.['hydra:property']=='reviewRating')]" +
                        ".['rangeIncludes']" +
                        ".['hydra:supportedProperty'][0].['minValue']").value(1))
                .andExpect(jsonPath("$.['hydra:member'][0].workPerformed.['hydra:collection'][0].['hydra:operation']" +
                        ".[0]" +
                        ".['hydra:expects'].['hydra:supportedProperty'].[?(@.['hydra:property']=='reviewRating')]" +
                        ".['rangeIncludes']" +
                        ".['hydra:supportedProperty'][0].['maxValue']").value(5))
                .andExpect(jsonPath("$.['hydra:member'][0].workPerformed.['hydra:collection'][0].['hydra:operation']" +
                        ".[0]" +
                        ".['hydra:expects'].['hydra:supportedProperty'].[?(@.['hydra:property']=='reviewRating')]" +
                        ".['rangeIncludes']" +
                        ".['hydra:supportedProperty'][0].['stepValue']").value(1))
                .andExpect(jsonPath("$.['hydra:member'][0].workPerformed.['hydra:collection'][0].['hydra:operation']" +
                        ".[0]" +
                        ".['hydra:expects'].['hydra:supportedProperty'].[?(@.['hydra:property']=='reviewRating')]" +
                        ".['rangeIncludes']" +
                        ".['hydra:supportedProperty'][0].['defaultValue']").value(3))
                .andReturn();
        System.out.println(result.getResponse()
                .getContentAsString());
        LOG.debug(result.getResponse()
                .getContentAsString());
    }

    @Test
    public void deserializesRequestBody() throws Exception {
        String reviewJson = "{\"reviewBody\": \"meh.\", \"reviewRating\": {\"ratingValue\": 3}}";
        MvcResult result = this.mockMvc.perform(MockMvcRequestBuilders.post("/reviews/events/1")
                .content(reviewJson)
                .contentType(HypermediaTypes.APPLICATION_JSONLD)
                .accept(HypermediaTypes.APPLICATION_JSONLD))
                .andExpect(MockMvcResultMatchers.status()
                        .isCreated())
                .andReturn();
        LOG.debug(result.getResponse()
                .getContentAsString());
    }

    @Test
    public void convertsTemplatedLinkToSampleInvocationAsIriTemplate() throws Exception {
        // TODO make sure a collection resource is rendered as collection with hydra:search property
        // TODO if its link has unsatisfied optional or required variables
        // TODO if the url has unsatisfied required variables, the collection has no @id, only a hydra:search
        // TODO distinguish if affordance:
        // is still templated after satisfying all variables from call values
        // if remaining are all optional and may be stripped when used as Uri
        // if a variable has a default value but might use other values which should be described in the IriTemplate
        // other cases?
        final MvcResult result = this.mockMvc.perform(MockMvcRequestBuilders.get("/reviews/events/1")
                .accept(HypermediaTypes.APPLICATION_JSONLD))
                .andExpect(content().contentType("application/ld+json"))
                .andExpect(jsonPath("$.['hydra:search'].@type").value("hydra:IriTemplate"))
                .andExpect(jsonPath("$.['hydra:search'].['hydra:template']").value("http://localhost/events/{eventId}"))
                .andExpect(jsonPath("$.['hydra:search'].['hydra:mapping'][0].['hydra:variable']").value("eventId"))
                .andExpect(jsonPath("$.['hydra:search'].['hydra:mapping'][0].['hydra:property']").value("eventId"))
                .andReturn();
        LOG.debug(result.getResponse()
                .getContentAsString());
    }


//    @Test
//    public void usesInputInclude() {
//
//        throw new UnsupportedOperationException();
//    }

    @Test
    public void convertsTemplatedLinkToMethodAsIriTemplate() throws Exception {
        final MvcResult result = this.mockMvc.perform(MockMvcRequestBuilders.get("/events")
                .accept(HypermediaTypes.APPLICATION_JSONLD))
                .andExpect(MockMvcResultMatchers.status()
                        .isOk())
                .andExpect(content().contentType("application/ld+json"))
                .andExpect(jsonPath("$.['hydra:search'].@type").value("hydra:IriTemplate"))
                .andExpect(jsonPath("$.['hydra:search'].['hydra:template']").value
                        ("http://localhost/events{?eventName}"))
                .andExpect(jsonPath("$.['hydra:search'].['hydra:mapping'][0].['hydra:variable']").value("eventName"))
                .andExpect(jsonPath("$.['hydra:search'].['hydra:mapping'][0].['hydra:property']").value
                        ("http://schema.org/name"))

                .andReturn();
        LOG.debug(result.getResponse()
                .getContentAsString());
    }

    @Test
    public void convertsTemplatedLinkWithRegexAsIriTemplate() throws Exception {
        final MvcResult result = this.mockMvc.perform(MockMvcRequestBuilders.get("/events")
                .accept(HypermediaTypes.APPLICATION_JSONLD))
                .andExpect(MockMvcResultMatchers.status()
                        .isOk())
                .andExpect(content().contentType("application/ld+json"))
                .andExpect(jsonPath("$.['ex:regex'].@type").value("hydra:IriTemplate"))
                .andExpect(jsonPath("$.['ex:regex'].['hydra:template']").value
                        ("http://localhost/events/regex/{eventId}"))
                .andExpect(jsonPath("$.['ex:regex'].['hydra:mapping'][0].['hydra:variable']").value("eventId"))
                .andExpect(jsonPath("$.['ex:regex'].['hydra:mapping'][0].['hydra:property']").value
                        ("ex:eventId"))

                .andReturn();
        LOG.debug(result.getResponse()
                .getContentAsString());
    }

    @Test
    public void convertsAffordanceWithRequestBody() throws Exception {
        final MvcResult result = this.mockMvc.perform(MockMvcRequestBuilders.get("/events")
                .accept(HypermediaTypes.APPLICATION_JSONLD))
                .andExpect(MockMvcResultMatchers.status()
                        .isOk())
                .andExpect(content().contentType("application/ld+json"))


                .andExpect(jsonPath("$.['hydra:member'][0]['hydra:operation'].[0]['hydra:method']")
                        .value("PUT"))
                .andExpect(jsonPath("$.['hydra:member'][0]['hydra:operation'].[0]['hydra:expects']" +
                        ".['@type']")
                        .value("Event"))
                .andExpect(jsonPath("$.['hydra:member'][0]['hydra:operation'].[1]['hydra:method']")
                        .value("DELETE"))

//                .andExpect(jsonPath("$.['hydra:member'][0]['hydra:operation'].['hydra:expects']
//.['hydra:supportedProperty'][0].@type")
//                        .value(Matchers.containsInAnyOrder("hydra:SupportedProperty", "PropertyValueSpecification")))

                .andReturn();
        LOG.debug(result.getResponse()
                .getContentAsString());
    }


}
