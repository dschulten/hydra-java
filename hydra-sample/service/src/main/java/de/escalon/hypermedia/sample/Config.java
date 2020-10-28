package de.escalon.hypermedia.sample;

import de.escalon.hypermedia.spring.HypermediaTypes;
import de.escalon.hypermedia.spring.hydra.HydraMessageConverter;
import de.escalon.hypermedia.spring.hydra.JsonLdDocumentationProvider;
import de.escalon.hypermedia.spring.siren.SirenMessageConverter;
import de.escalon.hypermedia.spring.uber.UberJackson2HttpMessageConverter;
import de.escalon.hypermedia.spring.xhtml.XhtmlResourceMessageConverter;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.hateoas.MediaTypes;
import org.springframework.hateoas.UriTemplate;
import org.springframework.hateoas.mediatype.MessageResolver;
import org.springframework.hateoas.mediatype.hal.CurieProvider;
import org.springframework.hateoas.mediatype.hal.DefaultCurieProvider;
import org.springframework.hateoas.mediatype.hal.Jackson2HalModule;
import org.springframework.hateoas.server.LinkRelationProvider;
import org.springframework.hateoas.server.core.AnnotationLinkRelationProvider;
import org.springframework.hateoas.server.core.DefaultLinkRelationProvider;
import org.springframework.hateoas.server.core.DelegatingLinkRelationProvider;
import org.springframework.hateoas.server.core.EvoInflectorLinkRelationProvider;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.plugin.core.PluginRegistry;
import org.springframework.plugin.core.config.EnablePluginRegistries;
import org.springframework.util.ClassUtils;
import org.springframework.web.servlet.HandlerExceptionResolver;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.mvc.method.annotation.ExceptionHandlerExceptionResolver;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Sample configuration. Created by dschulten on 28.12.2014.
 */
@Configuration
@EnableWebMvc
@EnablePluginRegistries(LinkRelationProvider.class)
@ComponentScan({"de.escalon.hypermedia.sample.event", "de.escalon.hypermedia.sample.store"})
public class Config implements WebMvcConfigurer {

    private static final boolean EVO_PRESENT =
            ClassUtils.isPresent("org.atteo.evo.inflector.English", null);

    @Autowired
    private PluginRegistry<LinkRelationProvider, LinkRelationProvider.LookupContext> relProviderRegistry;

    @Override
    public void configureMessageConverters(List<HttpMessageConverter<?>> converters) {
        converters.add(hydraMessageConverter());
        converters.add(sirenMessageConverter());
//        converters.add(halConverter());
        converters.add(uberConverter());
        converters.add(xhtmlMessageConverter());
        converters.add(jsonConverter());
    }

    @Bean
    public HttpMessageConverter<?> uberConverter() {
        UberJackson2HttpMessageConverter converter = new UberJackson2HttpMessageConverter();
        converter.setSupportedMediaTypes(Collections.singletonList(HypermediaTypes.UBER_JSON));
        return converter;
    }


    private HttpMessageConverter<?> xhtmlMessageConverter() {
        XhtmlResourceMessageConverter xhtmlResourceMessageConverter = new XhtmlResourceMessageConverter();
        xhtmlResourceMessageConverter.setStylesheets(
            Collections.singletonList(
                "https://maxcdn.bootstrapcdn.com/bootstrap/3.3.4/css/bootstrap.min.css"
                                     ));
        xhtmlResourceMessageConverter.setDocumentationProvider(new JsonLdDocumentationProvider());
        return xhtmlResourceMessageConverter;
    }

    @Override
    public void configureHandlerExceptionResolvers(List<HandlerExceptionResolver> exceptionResolvers) {
        final ExceptionHandlerExceptionResolver resolver = new ExceptionHandlerExceptionResolver();
        resolver.setWarnLogCategory(resolver.getClass()
                .getName());
        exceptionResolvers.add(resolver);
    }

    @Bean
    public HydraMessageConverter hydraMessageConverter() {
        return new HydraMessageConverter();
    }

    @Bean
    public SirenMessageConverter sirenMessageConverter() {
        SirenMessageConverter sirenMessageConverter = new SirenMessageConverter();
        sirenMessageConverter.setRelProvider(new DelegatingLinkRelationProvider(relProviderRegistry));
        sirenMessageConverter.setDocumentationProvider(new JsonLdDocumentationProvider());
        sirenMessageConverter.setSupportedMediaTypes(Collections.singletonList(HypermediaTypes.SIREN_JSON));
        return sirenMessageConverter;
    }


    @Bean
    public ObjectMapper jacksonObjectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);
        return objectMapper;
    }

    @Bean
    public MappingJackson2HttpMessageConverter jsonConverter() {
        MappingJackson2HttpMessageConverter jacksonConverter = new
                MappingJackson2HttpMessageConverter();
        jacksonConverter.setSupportedMediaTypes(Collections.singletonList(MediaType.valueOf("application/json")));
        jacksonConverter.setObjectMapper(jacksonObjectMapper());
        return jacksonConverter;
    }

    @Bean
    public CurieProvider curieProvider() {
        return new DefaultCurieProvider("ex", UriTemplate.of("http://localhost:8080/webapp/hypermedia-api/rels/{rels}"));
    }

    @Bean
    public MappingJackson2HttpMessageConverter halConverter() {
//        CurieProvider curieProvider = curieProvider();
//
//        LinkRelationProvider relProvider = new DelegatingLinkRelationProvider(relProviderRegistry);
//        ObjectMapper halObjectMapper = new ObjectMapper();
//
//        halObjectMapper.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);
//
//        halObjectMapper.registerModule(new Jackson2HalModule());
//        halObjectMapper.setHandlerInstantiator(new Jackson2HalModule.HalHandlerInstantiator(relProvider, curieProvider, null));

        MappingJackson2HttpMessageConverter halConverter = new MappingJackson2HttpMessageConverter();
        halConverter.setSupportedMediaTypes(Collections.singletonList(MediaTypes.HAL_JSON));
//        halConverter.setObjectMapper(halObjectMapper);
        return halConverter;
    }

    @Bean
    LinkRelationProvider defaultRelProvider() {
        return EVO_PRESENT ? new EvoInflectorLinkRelationProvider() : new DefaultLinkRelationProvider();
    }

    @Bean
    LinkRelationProvider annotationRelProvider() {
        return new AnnotationLinkRelationProvider();
    }
}
