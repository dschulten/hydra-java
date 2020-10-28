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

import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializationConfig;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.BeanSerializerModifier;
import com.fasterxml.jackson.databind.ser.std.BeanSerializerBase;
import de.escalon.hypermedia.affordance.TypedResource;
import de.escalon.hypermedia.hydra.serialize.JacksonHydraSerializer;
import de.escalon.hypermedia.hydra.serialize.ProxyUnwrapper;
import org.springframework.hateoas.PagedModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.RepresentationModel;
import org.springframework.hateoas.CollectionModel;


/**
 * Jackson Module which applies json-ld serialization to Spring Hateoas responses. Created by dschulten on 14.09.2014.
 */
public class JacksonHydraModule extends SimpleModule {

    private ProxyUnwrapper proxyUnwrapper;

    public JacksonHydraModule() {
        this(null);
    }

    public JacksonHydraModule(ProxyUnwrapper proxyUnwrapper) {
        super("json-hydra-module", new Version(1, 0, 0, null, "de.escalon.hypermedia", "hydra-spring"));
        this.proxyUnwrapper = proxyUnwrapper;
        setMixInAnnotation(RepresentationModel.class, RepresentationModelMixin.class);
        setMixInAnnotation(CollectionModel.class, CollectionModelMixin.class);
        setMixInAnnotation(PagedModel.class, PagedModelMixin.class);
        setMixInAnnotation(EntityModel.class, ResourceMixin.class);
        setMixInAnnotation(TypedResource.class, TypedResourceMixin.class);
        addSerializer(EntityModel.class, new ResourceSerializer());
        addSerializer(PagedModel.class, new PagedResourcesSerializer(proxyUnwrapper));

    }

    public void setupModule(SetupContext context) {
        super.setupModule(context);

        context.addBeanSerializerModifier(new BeanSerializerModifier() {

            public JsonSerializer<?> modifySerializer(
                    SerializationConfig config,
                    BeanDescription beanDesc,
                    JsonSerializer<?> serializer) {

                if (serializer instanceof BeanSerializerBase) {
                    JacksonHydraSerializer jacksonHydraSerializer = new JacksonHydraSerializer(
                            (BeanSerializerBase) serializer, proxyUnwrapper);
                    return jacksonHydraSerializer;
                } else {
                    return serializer;
                }
            }
        });
    }

}
