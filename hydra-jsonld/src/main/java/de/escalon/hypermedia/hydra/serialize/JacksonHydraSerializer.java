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
import org.apache.commons.lang3.text.WordUtils;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.util.*;

public class JacksonHydraSerializer extends BeanSerializerBase {

    public static final String KEY_LD_VOCAB = "de.escalon.hypermedia.ld-vocab";
    public static final String KEY_LD_CONTEXT = "de.escalon.hypermedia.ld-context";
    public static final String AT_VOCAB = "@vocab";
    public static final String AT_TYPE = "@type";
    public static final String AT_ID = "@id";
    public static final String HTTP_SCHEMA_ORG = "http://schema.org/";

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
        // TODO use serializerProvider.getAttributes to hold a stack of contexts

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

        jgen.writeStringField(AT_TYPE, val);
    }

    private void serializeContext(Object bean, JsonGenerator jgen,
                                  SerializerProvider serializerProvider, Deque<LdContext> contextStack) throws IOException {
        try {
            SerializationConfig config = serializerProvider.getConfig();
            final Class<?> mixInClass = config.findMixInClassFor(bean.getClass());

            final LdContext parentContext = contextStack.peek();
            LdContext currentContext = new LdContext(parentContext, getVocab(config, bean, mixInClass), getTerms(config, bean, mixInClass));
            contextStack.push(currentContext);
            // check if we need to write a context for the current bean at all
            // If it is in the same vocab: no context
            // If the terms are already defined in the context: no context
            boolean mustWriteContext;
            // not contained: vocab is not equal or current terms are not in parent
            // TODO prob: subcontexts only check their immediate parent
            // TODO merge parent and beanlocal context to become the current context on stack?
            // TODO chain contexts so that they know their parent?
            // TODO prob: the package context has no effect on Resource and Resources mixins
            // @ContextProvider -> invoke and recursively find non-collection/map type using first collection/map item
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
                    jgen.writeStringField(AT_VOCAB, currentContext.vocab);
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

    /**
     * Gets vocab for given bean.
     *
     * @param bean       to inspect for vocab
     * @param mixInClass for bean which might define a vocab or has a context provider
     * @return explicitly defined vocab or http://schema.org
     */
    private String getVocab(SerializationConfig config, Object bean, Class<?> mixInClass) {
        // determine vocab in context
        final Vocab packageVocab = getAnnotation(bean.getClass()
                .getPackage(), Vocab.class);
        final Vocab classVocab = getAnnotation(bean.getClass(), Vocab.class);

        final Vocab mixinVocab = getAnnotation(mixInClass, Vocab.class);

        Object nestedContextProviderFromMixin = getNestedContextProviderFromMixin(config, bean, mixInClass);
        String contextProviderVocab = null;
        if (nestedContextProviderFromMixin != null) {
            contextProviderVocab = getVocab(config, nestedContextProviderFromMixin, null);
        }

        String vocab;
        if (mixinVocab != null) {
            vocab = mixinVocab.value(); // wins over class
        } else if (classVocab != null) {
            vocab = classVocab.value(); // wins over package
        } else if (packageVocab != null) {
            vocab = packageVocab.value(); // wins over context provider
        } else if (contextProviderVocab != null) {
            vocab = contextProviderVocab; // wins over last resort
        } else {
            vocab = HTTP_SCHEMA_ORG;
        }
        return vocab;
    }

    private Map<String, Object> getTerms(SerializationConfig config, Object bean,
                                         Class<?> mixInClass) throws IllegalAccessException, NoSuchFieldException, IntrospectionException, InvocationTargetException {

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

        Object nestedContextProviderFromMixin = getNestedContextProviderFromMixin(config, bean, mixInClass);
        if (nestedContextProviderFromMixin != null) {
            termsMap.putAll(getTerms(config, nestedContextProviderFromMixin, null));
        }

        final Field[] fields = beanClass
                .getDeclaredFields();
        for (Field field : fields) {
            if (Modifier.isPublic(field.getModifiers())) {
                final Expose expose = field.getAnnotation(Expose.class);
                if (Enum.class.isAssignableFrom(field.getType())) {
                    addEnumTerms(termsMap, expose, field.getName(), (Enum) field.get(bean));
                } else {
                    if (expose != null) {
                        termsMap.put(field.getName(), expose.value());
                    }
                }
            }
        }

        final BeanInfo beanInfo = Introspector.getBeanInfo(beanClass);
        final PropertyDescriptor[] propertyDescriptors = beanInfo.getPropertyDescriptors();
        for (PropertyDescriptor propertyDescriptor : propertyDescriptors) {
            final Method method = propertyDescriptor.getReadMethod();
            if (method != null) {
                final Expose expose = method.getAnnotation(Expose.class);
                if (Enum.class.isAssignableFrom(method.getReturnType())) {
                    addEnumTerms(termsMap, expose, propertyDescriptor.getName(), (Enum) method.invoke(bean));
                } else {
                    if (expose != null) {
                        termsMap.put(propertyDescriptor.getName(), expose.value());
                    }
                }
            }
        }
        return termsMap;
    }

    private Object getNestedContextProviderFromMixin(SerializationConfig config, Object bean, Class<?> mixinClass) {
        if (mixinClass == null) {
            return null;
        }
        try {
            Method mixinContextProvider = getContextProvider(mixinClass);
            if (mixinContextProvider == null) {
                return null;
            }
            Class<?> beanClass = bean.getClass();
            Object contextual = beanClass.getMethod(mixinContextProvider.getName()).invoke(bean);
            Object ret = null;
            if (contextual instanceof Collection) {
                Collection collection = (Collection) contextual;
                if (!collection.isEmpty()) {
                    Object item = collection.iterator()
                            .next();
                    final Class<?> mixInClass = config.findMixInClassFor(item.getClass());
                    if (mixInClass == null) {
                        ret = item;
                    } else {
                        ret = getNestedContextProviderFromMixin(config, item, mixInClass);
                    }
                }
            } else if (contextual instanceof Map) {
                Map map = (Map) contextual;
                if (!map.isEmpty()) {
                    Object item = map.values()
                            .iterator()
                            .next();
                    final Class<?> mixInClass = config.findMixInClassFor(item.getClass());
                    if (mixInClass == null) {
                        ret = item;
                    } else {
                        ret = getNestedContextProviderFromMixin(config, item, mixInClass);
                    }
                }
            } else {
                ret = contextual;
            }
            return ret;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private Method getContextProvider(Class<?> beanClass) {
        Class<? extends Annotation> annotation = ContextProvider.class;
        Method contextProvider = getAnnotatedMethod(beanClass, annotation);
        if (contextProvider.getParameterTypes().length > 0) {
            throw new IllegalStateException("the context provider method " + contextProvider.getName() + " must not have arguments");
        }
        return contextProvider;
    }


    private void addEnumTerms(Map<String, Object> termsMap, Expose expose, String name,
                              Enum value) throws NoSuchFieldException {
        if (value != null) {
            Map<String, String> map = new LinkedHashMap<String, String>();
            if (expose != null) {
                map.put(AT_ID, expose.value());
            }
            map.put(AT_TYPE, AT_VOCAB);
            termsMap.put(name, map);
            final Expose enumValueExpose = getAnnotation(value.getClass()
                    .getField(value.name()), Expose.class);

            if (enumValueExpose != null) {
                termsMap.put(value.toString(), enumValueExpose.value());
            } else {
                // might use upperToCamelCase if nothing is exposed
                final String camelCaseEnumValue = WordUtils.capitalizeFully(value.toString(), new char[]{'_'})
                        .replaceAll("_", "");
                termsMap.put(value.toString(), camelCaseEnumValue);
            }
        }
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

    private static <T extends Annotation> T getAnnotation(AnnotatedElement annotated, Class<T> annotationClass) {
        T ret;
        if (annotated == null) {
            ret = null;
        } else {
            ret = annotated.getAnnotation(annotationClass);
        }
        return ret;
    }

    private static Method getAnnotatedMethod(Class<?> clazz, Class<? extends Annotation> annotation) {
        Method[] methods = clazz.getMethods();
        Method ret = null;
        for (Method method : methods) {
            if (method.getAnnotation(annotation) != null) {
                ret = method;
                break;
            }
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
