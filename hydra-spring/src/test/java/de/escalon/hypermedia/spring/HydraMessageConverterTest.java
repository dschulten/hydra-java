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

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.module.SimpleModule;
import de.escalon.hypermedia.spring.de.escalon.hypermedia.spring.sample.EventController;
import de.escalon.hypermedia.spring.de.escalon.hypermedia.spring.jackson.JacksonHydraModule;
import de.escalon.hypermedia.spring.de.escalon.hypermedia.spring.jackson.ResourceSerializer;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.hateoas.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
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
import org.springframework.web.servlet.config.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.ExceptionHandlerExceptionResolver;

import java.util.List;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

/**
 * Created by dschulten on 11.09.2014.
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
        public EventController eventController() {
            return new EventController();
        }

        @Override
        public void configureMessageConverters(List<HttpMessageConverter<?>> converters) {
            super.configureMessageConverters(converters);
            converters.add(hydraMessageConverter());
        }

        @Override
        public void configureHandlerExceptionResolvers(List<HandlerExceptionResolver> exceptionResolvers) {
            final ExceptionHandlerExceptionResolver resolver = new ExceptionHandlerExceptionResolver();
            resolver.setWarnLogCategory(resolver.getClass()
                    .getName());
            exceptionResolvers.add(resolver);
        }

        private HttpMessageConverter<Object> hydraMessageConverter() {
            final MappingJackson2HttpMessageConverter converter = new MappingJackson2HttpMessageConverter();
            ObjectMapper objectMapper = new ObjectMapper();

            SimpleModule module = new JacksonHydraModule();
            objectMapper.registerModule(module);
            // TODO curie and relprovider?
            converter.setObjectMapper(objectMapper);
            return converter;
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
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.status()
                        .isOk())
                .andExpect(content().contentType("application/json;charset=UTF-8"))
                .andExpect(jsonPath("$.@type").value("Event"))
                .andExpect(jsonPath("$.performer").value("Cornelia Bielefeldt"))
                .andExpect(jsonPath("$.reviews.@id").value("http://localhost/reviews"))
                .andReturn();
        LOG.debug(result.getResponse().getContentAsString());
    }

    @Test
    public void convertsResourceSupport() throws Exception {
        final MvcResult result = this.mockMvc.perform(MockMvcRequestBuilders.get("/events/resourcesupport/1")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.status()
                        .isOk())
                .andExpect(content().contentType("application/json;charset=UTF-8"))
                .andExpect(jsonPath("$.@type").value("Event"))
                .andExpect(jsonPath("$.performer").value("Cornelia Bielefeldt"))
                .andExpect(jsonPath("$.reviews.@id").value("http://localhost/reviews"))
                .andReturn();
        LOG.debug(result.getResponse().getContentAsString());
    }

    @Test
    public void convertsListOfResourceOfEvent() throws Exception {
        final MvcResult result = this.mockMvc.perform(MockMvcRequestBuilders.get("/events/list")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.status()
                        .isOk())
                .andExpect(jsonPath("$.[0].@type").value("Event"))
                .andExpect(jsonPath("$.[0].performer").value("Walk off the Earth"))
                .andExpect(jsonPath("$.[0].reviews.@id").value("http://localhost/reviews/events/1"))
                .andReturn();
        LOG.debug(result.getResponse().getContentAsString());
    }

    @Test
    public void convertsResources() throws Exception {
        final MvcResult result = this.mockMvc.perform(MockMvcRequestBuilders.get("/events")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.status()
                        .isOk())
                .andExpect(content().contentType("application/json;charset=UTF-8"))
                .andExpect(jsonPath("$.@type").value("hydra:Collection"))
                .andExpect(jsonPath("$.['hydra:member'][0].@type").value("Event"))
                .andExpect(jsonPath("$.['hydra:member'][0].performer").value("Walk off the Earth"))
                .andExpect(jsonPath("$.['hydra:member'][0].reviews.@id").value("http://localhost/reviews/events/1"))
                .andReturn();
        LOG.debug(result.getResponse().getContentAsString());
    }
}
