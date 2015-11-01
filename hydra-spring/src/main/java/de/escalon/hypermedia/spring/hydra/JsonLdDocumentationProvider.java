package de.escalon.hypermedia.spring.hydra;

import de.escalon.hypermedia.affordance.AnnotatedParameter;
import de.escalon.hypermedia.AnnotationUtils;
import de.escalon.hypermedia.hydra.mapping.Expose;
import de.escalon.hypermedia.hydra.serialize.LdContextFactory;
import de.escalon.hypermedia.hydra.serialize.MixinSource;
import de.escalon.hypermedia.spring.xhtml.DocumentationProvider;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;

/**
 * Created by Dietrich on 05.04.2015.
 */
public class JsonLdDocumentationProvider implements DocumentationProvider {

    private LdContextFactory ldContextFactory = new LdContextFactory();

    private MixinSource mixinSource = new MixinSource() {
        @Override
        public Class<?> findMixInClassFor(Class<?> clazz) {
            return null;
        }
    };

    @Override
    public String getDocumentationUrl(AnnotatedParameter parameter, Object content) {
        final Expose expose = parameter.getAnnotation(Expose.class);
        String ret;
        if (content == null) {
            Class<?> clazz = parameter.getDeclaringClass();
            ret = getExposedUrl(parameter.getParameterName(), vocabFromClass(clazz), termsFromClazz(clazz), expose);
        } else {
            ret = getExposedUrl(parameter.getParameterName(), vocabFromBean(content), termsFromBean(content), expose);
        }
        return ret;
    }

    @Override
    public String getDocumentationUrl(Field field, Object content) {
        final Expose expose = AnnotationUtils.getAnnotation(field, Expose.class);
        // TODO can we support Mixins from here?
//        final Class<?> mixin = provider.getConfig()
//                .findMixInClassFor(bean.getClass());
//        final Expose mixinExpose = getAnnotation(mixin, Expose.class);
        return getExposedUrl(field.getName(), vocabFromBean(content), termsFromBean(content), expose);
    }

    @Override
    public String getDocumentationUrl(Method method, Object content) {
        final Expose expose = AnnotationUtils.getAnnotation(method, Expose.class);
        // TODO can we support Mixins from here?
//        final Class<?> mixin = provider.getConfig()
//                .findMixInClassFor(bean.getClass());
//        final Expose mixinExpose = getAnnotation(mixin, Expose.class);
        String methodName = method.getName();
        String propertyName;
        if (methodName.startsWith("get")) {
            propertyName = StringUtils.uncapitalize(StringUtils.removeStart(methodName, "get"));
        } else {
            propertyName = StringUtils.uncapitalize(StringUtils.removeStart(methodName, "is"));
        }
        return getExposedUrl(propertyName, vocabFromBean(content), termsFromBean(content), expose);
    }

    @Override
    public String getDocumentationUrl(Class clazz, Object content) {
        final Expose expose = AnnotationUtils.getAnnotation(clazz, Expose.class);
        // TODO can we support Mixins from here?
//        final Class<?> mixin = provider.getConfig()
//                .findMixInClassFor(bean.getClass());
//        final Expose mixinExpose = getAnnotation(mixin, Expose.class);
        return getExposedUrl(clazz.getSimpleName(), vocabFromBean(content), termsFromBean(content), expose);
    }

    @Nullable
    private String getExposedUrl(String plainName, String vocab, Map<String, Object> terms, Expose expose) {
        final String name;
        if (expose != null) {
            name = expose.value(); // expose is better than Java name
        } else {
            name = plainName;
        }

        String url;
        if (name.matches("http(s)?://.+")) {
            url = name;
        } else if (name.contains(":")) {
            url = resolveCurie(terms, name);
        } else {
            url = makeVocabUrl(vocab, name);
        }
        return url;
    }

    @Nullable
    private String resolveCurie(Map<String, Object> terms, String name) {
        String url;
        String[] curie = name.split(":");
        Object termDef = terms.get(curie[0]);
        if (termDef != null) {
            if (termDef instanceof Map) {
                // TODO handle @name:@vocab etc.
                url = null;
            } else {
                url = termDef.toString() + curie[1];
            }
        } else {
            url = null;
        }
        return url;
    }

    @Override
    public String getDocumentationUrl(String name, Object content) {
        String ret;
        if (content == null) {
            ret = null;
        } else {
            ret = getExposedUrl(name, vocabFromBean(content), termsFromBean(content), null);
        }
        return ret;
    }

    @Nullable
    private String makeVocabUrl(String vocab, String name) {
        String url;
        if (vocab != null) {
            url = vocab + name;
        } else {
            url = null;
        }
        return url;
    }

    @Nullable
    private String vocabFromBean(Object content) {
        return ldContextFactory.getVocab(mixinSource, content, null);
    }

    private Map<String, Object> termsFromBean(Object content) {
        return ldContextFactory.getTerms(mixinSource, content, null);
    }

    private Map<String, Object> termsFromClazz(Class<?> clazz) {
        return ldContextFactory.termsFromClass(clazz);
    }

    private String vocabFromClass(Class<?> clazz) {
        String vocabFromClassOrPackage = ldContextFactory.vocabFromClassOrPackage(clazz);
        return vocabFromClassOrPackage == null ? LdContextFactory.HTTP_SCHEMA_ORG : vocabFromClassOrPackage;
    }

}
