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
package de.escalon.hypermedia.hydra.serialize;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.ser.impl.BeanAsArraySerializer;
import com.fasterxml.jackson.databind.ser.impl.ObjectIdWriter;
import com.fasterxml.jackson.databind.ser.std.BeanSerializerBase;
import com.fasterxml.jackson.databind.util.NameTransformer;
import de.escalon.hypermedia.hydra.mapping.*;

import java.io.IOException;
import java.util.*;

import static de.escalon.hypermedia.AnnotationUtils.getAnnotation;

public class JacksonHydraSerializer extends BeanSerializerBase {

    public static final String KEY_LD_CONTEXT = "de.escalon.hypermedia.ld-context";

    private LdContextFactory ldContextFactory = new LdContextFactory();

    public JacksonHydraSerializer(BeanSerializerBase source) {
        super(source);
    }

    public JacksonHydraSerializer(BeanSerializerBase source,
                                  ObjectIdWriter objectIdWriter) {
        super(source, objectIdWriter);
    }

    public JacksonHydraSerializer(BeanSerializerBase source,
                                  String[] toIgnore) {
        super(source, toIgnore);
    }

    public BeanSerializerBase withObjectIdWriter(
            ObjectIdWriter objectIdWriter) {
        return new JacksonHydraSerializer(this, objectIdWriter);
    }

    protected BeanSerializerBase withIgnorals(String[] toIgnore) {
        return new JacksonHydraSerializer(this, toIgnore);
    }

    @Override
    protected BeanSerializerBase asArraySerializer() {
    /* Can not:
     *
     * - have Object Id (may be allowed in future)
     * - have any getter
     *
     */
        if ((_objectIdWriter == null)
                && (_anyGetterWriter == null)
                && (_propertyFilterId == null)
                ) {
            return new BeanAsArraySerializer(this);
        }
        // already is one, so:
        return this;
    }

    @Override
    protected BeanSerializerBase withFilterId(Object filterId) {
        final JacksonHydraSerializer ret = new JacksonHydraSerializer(this);
        ret.withFilterId(filterId);
        return ret;
    }

    @Override
    public void serialize(Object bean, JsonGenerator jgen,
                          SerializerProvider serializerProvider) throws IOException {
        if (!isUnwrappingSerializer()) {
            jgen.writeStartObject();
        }
        Deque<LdContext> contextStack = (Deque<LdContext>) serializerProvider.getAttribute(KEY_LD_CONTEXT);
        if (contextStack == null) {
            contextStack = new ArrayDeque<LdContext>();
            serializerProvider.setAttribute(KEY_LD_CONTEXT, contextStack);
        }

        serializeContext(bean, jgen, serializerProvider, contextStack);
        serializeType(bean, jgen, serializerProvider);
        serializeFields(bean, jgen, serializerProvider);
        if (!isUnwrappingSerializer()) {
            jgen.writeEndObject();
        }
        contextStack = (Deque<LdContext>) serializerProvider.getAttribute(KEY_LD_CONTEXT);
        if (!contextStack.isEmpty()) {
            contextStack.pop();
        }
    }

    private void serializeType(Object bean, JsonGenerator jgen, SerializerProvider provider) throws IOException {
        // adds @type attribute, reflecting the simple name of the class or the exposed annotation on the class.
        final Expose classExpose = getAnnotation(bean.getClass(), Expose.class);
        // TODO allow to search up the hierarchy for ResourceSupport mixins and cache found result?
        final Class<?> mixin = provider.getConfig()
                .findMixInClassFor(bean.getClass());
        final Expose mixinExpose = getAnnotation(mixin, Expose.class);
        final String val;
        if (mixinExpose != null) {
            val = mixinExpose.value(); // mixin wins over class
        } else if (classExpose != null) {
            val = classExpose.value(); // expose is better than Java type
        } else {
            val = bean.getClass()
                    .getSimpleName();
        }

        jgen.writeStringField(JsonLdKeywords.AT_TYPE, val);
    }

    private void serializeContext(Object bean, JsonGenerator jgen,
                                  SerializerProvider serializerProvider, Deque<LdContext> contextStack) throws IOException {

            MixinSource mixinSource = new JacksonMixinSource(serializerProvider.getConfig());
            final Class<?> mixInClass = mixinSource.findMixInClassFor(bean.getClass());

            final LdContext parentContext = contextStack.peek();
            LdContext currentContext = new LdContext(parentContext, ldContextFactory.getVocab(mixinSource, bean, mixInClass), ldContextFactory.getTerms(mixinSource, bean, mixInClass));
            contextStack.push(currentContext);
            // check if we need to write a context for the current bean at all
            // If it is in the same vocab: no context
            // If the terms are already defined in the context: no context
            boolean mustWriteContext;
            if (parentContext == null || !parentContext.contains(currentContext)) {
                mustWriteContext = true;
            } else {
                mustWriteContext = false;
            }

            if (mustWriteContext) {
                // begin context
                // default context: schema.org vocab or vocab package annotation
                jgen.writeObjectFieldStart("@context");
                // do not repeat vocab if already defined in current context
                if (parentContext == null || parentContext.vocab == null ||
                        (currentContext.vocab != null && !currentContext.vocab.equals(parentContext.vocab))) {
                    jgen.writeStringField(JsonLdKeywords.AT_VOCAB, currentContext.vocab);
                }

                for (Map.Entry<String, Object> termEntry : currentContext.terms.entrySet()) {
                    if (termEntry.getValue() instanceof String) {
                        jgen.writeStringField(termEntry.getKey(), termEntry.getValue()
                                .toString());
                    } else {
                        jgen.writeObjectField(termEntry.getKey(), termEntry.getValue());
                    }
                }
                jgen.writeEndObject();
                // end context
            }
            // TODO build the context from @Vocab and @Term and @Expose and write it as local or external context with
            // TODO jsonld extension (using apt?)
            // TODO also allow manually created jsonld contexts
            // TODO how to define a context containing several context objects? @context is then an array of
            // TODO external context strings pointing to json-ld, and json objects containing terms
            // TODO another option: create custom vocabulary without reference to public vocabs
            // TODO support additionalType from goodrelations

    }

    @Override
    public JsonSerializer<Object> unwrappingSerializer(NameTransformer unwrapper) {
        return new UnwrappingJacksonHydraSerializer(this);
    }

    @Override
    public void resolve(SerializerProvider provider) throws JsonMappingException {
        super.resolve(provider);
    }

    @Override
    public JsonSerializer<?> createContextual(SerializerProvider provider,
                                              BeanProperty property) throws JsonMappingException {
        return super.createContextual(provider, property);
    }
}
