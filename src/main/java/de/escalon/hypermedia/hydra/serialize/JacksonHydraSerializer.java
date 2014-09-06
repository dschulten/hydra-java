package de.escalon.hypermedia.hydra.serialize;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.impl.BeanAsArraySerializer;
import com.fasterxml.jackson.databind.ser.impl.ObjectIdWriter;
import com.fasterxml.jackson.databind.ser.std.BeanSerializerBase;
import de.escalon.hypermedia.hydra.mapping.Expose;
import de.escalon.hypermedia.hydra.mapping.Term;
import de.escalon.hypermedia.hydra.mapping.Terms;
import de.escalon.hypermedia.hydra.mapping.Vocab;

import java.beans.BeanInfo;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;

/**
 * Created by dschulten on 23.08.2014.
 */
public class JacksonHydraSerializer extends BeanSerializerBase {

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

    public void serialize(Object bean, JsonGenerator jgen,
                          SerializerProvider provider) throws IOException,
            JsonGenerationException {
        jgen.writeStartObject();
        serializeContext(bean, jgen, provider);
        serializeType(bean, jgen, provider);
        serializeFields(bean, jgen, provider);
        jgen.writeEndObject();
    }

    private void serializeType(Object bean, JsonGenerator jgen, SerializerProvider provider) throws IOException {
        // adds @type attribute, reflecting the simple name of the class or the exposed annotation on the class.
        final Expose expose = getAnnotation(bean.getClass(), Expose.class);
        final String val;
        if (expose == null) {
            val = bean.getClass().getSimpleName();
        } else {
            val = expose.value();
        }
        jgen.writeStringField("@type", val);
    }

    private void serializeContext(Object bean, JsonGenerator jgen, SerializerProvider serializerProvider) throws IOException {
        try {
            // write vocab in context
            final Vocab packageVocab = getAnnotation(bean.getClass().getPackage(), Vocab.class);
            final Vocab classVocab = getAnnotation(bean.getClass(), Vocab.class);

            // begin context
            // default context: schema.org vocab or vocab package annotation
            jgen.writeObjectFieldStart("@context");

            String vocab;
            if (classVocab != null) {
                vocab = classVocab.value(); // wins over package
            } else if (packageVocab != null) {
                vocab = packageVocab.value();
            } else {
                vocab = "http://schema.org/";
            }
            jgen.writeStringField("@vocab", vocab);

            // define terms from package or type in context
            Map<String, String> packageTermsMap = getTerms(bean.getClass().getPackage(), bean.getClass().getPackage().getName());
            Map<String, String> classTermsMap = getTerms(bean.getClass(), bean.getClass().getName());
            // class terms override package terms
            packageTermsMap.putAll(classTermsMap);
            for (Map.Entry<String, String> termEntry : packageTermsMap.entrySet()) {
                jgen.writeStringField(termEntry.getKey(), termEntry.getValue());
            }

            // expose fields in context
            final Field[] fields = bean.getClass().getDeclaredFields();
            for (Field field : fields) {
                final Expose expose = field.getAnnotation(Expose.class);
                if (expose != null) {
                    jgen.writeStringField(field.getName(), expose.value());
                }
            }

            // expose getters in context
            final BeanInfo beanInfo = Introspector.getBeanInfo(bean.getClass());
            final PropertyDescriptor[] propertyDescriptors = beanInfo.getPropertyDescriptors();
            for (PropertyDescriptor propertyDescriptor : propertyDescriptors) {
                final Method method = propertyDescriptor.getReadMethod();
                final Expose expose = method.getDeclaredAnnotation(Expose.class);
                if (expose != null) {
                    jgen.writeStringField(propertyDescriptor.getName(), expose.value());
                }
            }

            jgen.writeEndObject();

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

    private Map<String, String> getTerms(AnnotatedElement annotatedElement, String name) {
        final Terms packageTerms = getAnnotation(annotatedElement, Terms.class);
        final Term packageTerm = getAnnotation(annotatedElement, Term.class);

        if (packageTerms != null && packageTerm != null) {
            throw new IllegalStateException("found both @Terms and @Term in " +
                    annotatedElement.getClass().getName() + " " + name +", use either one or the other");
        }
        Map<String, String> packageTermsMap = new HashMap<String, String>();
        if (packageTerms != null) {
            final Term[] terms = packageTerms.value();
            for (Term term : terms) {
                final String define = term.define();
                final String as = term.as();
                if (packageTermsMap.containsKey(as)) {
                    throw new IllegalStateException("duplicate definition of term '" + define + "' in " +
                            annotatedElement.getClass().getName() +  " " + name);
                }
                packageTermsMap.put(define, as);
            }
        }
        if(packageTerm != null) {
            packageTermsMap.put(packageTerm.define(), packageTerm.as());
        }
        return packageTermsMap;
    }

    private <T extends Annotation> T getAnnotation(AnnotatedElement annotated, Class<T> annotationClass) {
        final T ann = annotated.getAnnotation(annotationClass);
        return ann;
    }

}
