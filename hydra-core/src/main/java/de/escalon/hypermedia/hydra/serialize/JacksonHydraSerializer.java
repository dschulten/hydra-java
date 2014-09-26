/*
 * Copyright (c) 2014. Escalon System-Entwicklung, Dietrich Schulten
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */
package de.escalon.hypermedia.hydra.serialize;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.ser.impl.BeanAsArraySerializer;
import com.fasterxml.jackson.databind.ser.impl.ObjectIdWriter;
import com.fasterxml.jackson.databind.ser.std.BeanSerializerBase;
import com.fasterxml.jackson.databind.util.NameTransformer;
import de.escalon.hypermedia.hydra.mapping.Expose;
import de.escalon.hypermedia.hydra.mapping.Term;
import de.escalon.hypermedia.hydra.mapping.Terms;
import de.escalon.hypermedia.hydra.mapping.Vocab;
import org.apache.commons.lang3.text.WordUtils;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.Map;

public class JacksonHydraSerializer extends BeanSerializerBase {

    public static final String KEY_LD_CONTEXT = "de.escalon.hypermedia.ld-context";
    public static final String AT_VOCAB = "@vocab";
    public static final String AT_TYPE = "@type";
    public static final String AT_ID = "@id";

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
        Deque<String> deque = (Deque<String>) serializerProvider.getAttribute(KEY_LD_CONTEXT);
        if (deque == null) {
            deque = new ArrayDeque<String>();
            serializerProvider.setAttribute(KEY_LD_CONTEXT, deque);
        }

        serializeContext(bean, jgen, serializerProvider, deque);
        serializeType(bean, jgen, serializerProvider);
        serializeFields(bean, jgen, serializerProvider);
        if (!isUnwrappingSerializer()) {
            jgen.writeEndObject();
        }
        deque = (Deque<String>) serializerProvider.getAttribute(KEY_LD_CONTEXT);
        if (!deque.isEmpty()) {
            deque.pop();
        }
    }

    private void serializeType(Object bean, JsonGenerator jgen, SerializerProvider provider) throws IOException {
        // adds @type attribute, reflecting the simple name of the class or the exposed annotation on the class.
        final Expose classExpose = getAnnotation(bean.getClass(), Expose.class);
        // TODO allow to search up the hierarchy for ResourceSupport mixins and cache find result?
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

        jgen.writeStringField(AT_TYPE, val);
    }

    private void serializeContext(Object bean, JsonGenerator jgen,
                                  SerializerProvider serializerProvider, Deque<String> deque) throws IOException {
        try {
            // TODO use serializerProvider.getAttributes to hold a stack of contexts
            // and check if we need to write a context for the current bean at all
            // If it is in the same vocab: no context
            // If the terms are already defined in the context: no context

            SerializationConfig config = serializerProvider.getConfig();
            final Class<?> mixInClass = config.findMixInClassFor(bean.getClass());

            String vocab = getVocab(bean, mixInClass);
            Map<String, Object> terms = getTerms(bean, mixInClass);

            final String currentVocab = deque.peek();

            deque.push(vocab);
            boolean mustWriteContext;
            if (currentVocab == null || !vocab.equals(currentVocab)) {
                mustWriteContext = true;
            } else {
                // only write if bean has terms
                if (terms.isEmpty()) {
                    mustWriteContext = false;
                } else {
                    // TODO actually, need not repeat vocab in context if same
                    mustWriteContext = true;
                }
            }

            if (mustWriteContext) {
                // begin context
                // default context: schema.org vocab or vocab package annotation
                jgen.writeObjectFieldStart("@context");
                // TODO do not repeat vocab if already defined in current context
                if (currentVocab == null || !vocab.equals(currentVocab)) {
                    jgen.writeStringField(AT_VOCAB, vocab);
                }

                for (Map.Entry<String, Object> termEntry : terms.entrySet()) {
                    if (termEntry.getValue() instanceof String) {
                        jgen.writeStringField(termEntry.getKey(), termEntry.getValue()
                                .toString());
                    } else {
                        jgen.writeObjectField(termEntry.getKey(), termEntry.getValue());
                    }
                }


                jgen.writeEndObject();
            }

            // end context

            // TODO build the context from @Vocab and @Term and @Expose and write it as local or external context with
            // TODO jsonld extension (using apt?)
            // TODO also allow manually created jsonld contexts
            // TODO how to define a context containing several context objects? @context is then an array of
            // TODO external context strings pointing to json-ld, and json objects containing terms
            // TODO another option: create custom vocabulary without reference to public vocabs
            // TODO support additionalType from goodrelations
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private String getVocab(Object bean, Class<?> mixInClass) {
        // write vocab in context
        final Vocab packageVocab = getAnnotation(bean.getClass()
                .getPackage(), Vocab.class);
        final Vocab classVocab = getAnnotation(bean.getClass(), Vocab.class);

        final Vocab mixinVocab = getAnnotation(mixInClass, Vocab.class);

        String vocab;
        if (mixinVocab != null) {
            vocab = mixinVocab.value(); // wins over class
        } else if (classVocab != null) {
            vocab = classVocab.value(); // wins over package
        } else if (packageVocab != null) {
            vocab = packageVocab.value();
        } else {
            vocab = "http://schema.org/";
        }
        return vocab;
    }

    private Map<String, Object> getTerms(Object bean,
                                         Class<?> mixInClass) throws IntrospectionException, IllegalAccessException, NoSuchFieldException {
        // define terms from package or type in context
        final Class<?> beanClass = bean.getClass();
        Map<String, Object> termsMap = getAnnotatedTerms(beanClass.getPackage(), beanClass.getPackage()
                .getName());
        Map<String, Object> classTermsMap = getAnnotatedTerms(beanClass, beanClass
                .getName());
        Map<String, Object> mixinTermsMap = getAnnotatedTerms(mixInClass, beanClass
                .getName());

        // class terms override package terms
        termsMap.putAll(classTermsMap);
        // mixin terms override class terms
        termsMap.putAll(mixinTermsMap);

        final Field[] fields = beanClass
                .getDeclaredFields();
        for (Field field : fields) {
            final Expose fieldExpose = field.getAnnotation(Expose.class);
            if (Enum.class.isAssignableFrom(field.getType())) {
                Map<String, String> map = new LinkedHashMap<String, String>();
                termsMap.put(field.getName(), map);
                if (fieldExpose != null) {
                    map.put(AT_ID, fieldExpose.value());
                }
                map.put(AT_TYPE, AT_VOCAB);
                final Enum value = (Enum)field.get(bean);
                final Expose enumValueExpose = getAnnotation(value.getClass().getField(value.name()), Expose.class);
                // TODO redefine actual enum value to exposed on enum value definition
                if (enumValueExpose != null) {
                    termsMap.put(value.toString(), enumValueExpose.value());
                } else {
                    // might use upperToCamelCase if nothing is exposed
                    final String camelCaseEnumValue = WordUtils.capitalizeFully(value.toString(), new char[]{'_'})
                            .replaceAll("_", "");
                    termsMap.put(value.toString(), camelCaseEnumValue);
                }
            } else {
                if (fieldExpose != null) {
                    termsMap.put(field.getName(), fieldExpose.value());
                }
            }
        }

        // TODO do this recursively for nested beans and collect as long as
        // nested beans have same vocab
        // expose getters in context
        final BeanInfo beanInfo = Introspector.getBeanInfo(beanClass);
        final PropertyDescriptor[] propertyDescriptors = beanInfo.getPropertyDescriptors();
        for (PropertyDescriptor propertyDescriptor : propertyDescriptors) {
            final Method method = propertyDescriptor.getReadMethod();

            final Expose expose = method.getAnnotation(Expose.class);
            if (expose != null) {
                termsMap.put(propertyDescriptor.getName(), expose.value());
            }
        }
        return termsMap;
    }

    /**
     * Gets explicitly defined terms, e.g. on package, class or mixin.
     *
     * @param annotatedElement to find terms
     * @param name             of annotated element, i.e. class name or package name
     * @return terms
     */
    private Map<String, Object> getAnnotatedTerms(AnnotatedElement annotatedElement, String name) {
        final Terms annotatedTerms = getAnnotation(annotatedElement, Terms.class);
        final Term annotatedTerm = getAnnotation(annotatedElement, Term.class);

        if (annotatedTerms != null && annotatedTerm != null) {
            throw new IllegalStateException("found both @Terms and @Term in " + name + ", use either one or the other");
        }
        Map<String, Object> annotatedTermsMap = new LinkedHashMap<String, Object>();
        if (annotatedTerms != null) {
            final Term[] terms = annotatedTerms.value();
            for (Term term : terms) {
                final String define = term.define();
                final String as = term.as();
                if (annotatedTermsMap.containsKey(as)) {
                    throw new IllegalStateException("duplicate definition of term '" + define + "' in " + name);
                }
                annotatedTermsMap.put(define, as);
            }
        }
        if (annotatedTerm != null) {
            annotatedTermsMap.put(annotatedTerm.define(), annotatedTerm.as());
        }
        return annotatedTermsMap;
    }

    private <T extends Annotation> T getAnnotation(AnnotatedElement annotated, Class<T> annotationClass) {
        T ret;
        if (annotated == null) {
            ret = null;
        } else {
            ret = annotated.getAnnotation(annotationClass);
        }
        return ret;
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
