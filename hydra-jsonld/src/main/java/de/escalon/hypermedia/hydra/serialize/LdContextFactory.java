package de.escalon.hypermedia.hydra.serialize;

import de.escalon.hypermedia.AnnotationUtils;
import de.escalon.hypermedia.hydra.mapping.*;
import org.apache.commons.lang3.text.WordUtils;

import java.beans.BeanInfo;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

import static de.escalon.hypermedia.AnnotationUtils.findAnnotation;

/**
 * Provides LdContext information. Created by Dietrich on 05.04.2015.
 */
public class LdContextFactory {

    public static final String HTTP_SCHEMA_ORG = "http://schema.org/";
    private ProxyUnwrapper proxyUnwrapper;

    /**
     * Gets vocab for given bean.
     *
     * @param mixinSource to inspect from
     * @param bean to inspect for vocab
     * @param mixInClass for bean which might define a vocab or has a context provider
     * @return explicitly defined vocab or http://schema.org
     */
    public String getVocab(MixinSource mixinSource, Object bean, Class<?> mixInClass) {
        if (proxyUnwrapper != null) {
            bean = proxyUnwrapper.unwrapProxy(bean);
        }

        // determine vocab in context
        String classVocab = bean == null ? null : vocabFromClassOrPackage(bean.getClass());

        final Vocab mixinVocab = findAnnotation(mixInClass, Vocab.class);

        Object nestedContextProviderFromMixin = getNestedContextProviderFromMixin(mixinSource, bean, mixInClass);
        String contextProviderVocab = null;
        if (nestedContextProviderFromMixin != null) {
            contextProviderVocab = getVocab(mixinSource, nestedContextProviderFromMixin, null);
        }

        String vocab;
        if (mixinVocab != null) {
            vocab = mixinVocab.value(); // wins over class
        } else if (classVocab != null) {
            vocab = classVocab; // wins over context provider
        } else if (contextProviderVocab != null) {
            vocab = contextProviderVocab; // wins over last resort
        } else {
            vocab = HTTP_SCHEMA_ORG;
        }
        return vocab;
    }

    public Map<String, Object> getTerms(MixinSource mixinSource, Object bean, Class<?> mixInClass) {

        try {

            if (proxyUnwrapper != null) {
                bean = proxyUnwrapper.unwrapProxy(bean);
            }

            Map<String, Object> termsMap = new LinkedHashMap<>();
            if (bean != null) {
                final Class<?> beanClass = bean.getClass();
                termsMap.putAll(termsFromClass(beanClass));
                Map<String, Object> mixinTermsMap = getAnnotatedTerms(mixInClass, beanClass
                        .getName());

                // mixin terms override class terms
                termsMap.putAll(mixinTermsMap);

                Object nestedContextProviderFromMixin = getNestedContextProviderFromMixin(mixinSource, bean,
                        mixInClass);

                if (nestedContextProviderFromMixin != null) {
                    termsMap.putAll(getTerms(mixinSource, nestedContextProviderFromMixin, null));
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
                        } else if ( returnsEnumCollection( method ) ) {
							Collection<? extends Enum> coll = ( Collection<? extends Enum> ) method.invoke( bean );
							for ( Enum item : coll ) {
								addEnumTerms(termsMap, expose, propertyDescriptor.getName(), item);
							}
                        } else {
                            if (expose != null) {
                                termsMap.put(propertyDescriptor.getName(), expose.value());
                            }
                        }
                    }
                }
            }
            return termsMap;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

	private boolean returnsEnumCollection( Method method ) {
		if ( Collection.class.isAssignableFrom( method.getReturnType() ) ) {
			Type t = method.getGenericReturnType();
			if ( t instanceof ParameterizedType ) {
				ParameterizedType pt = (ParameterizedType) t;
				if ( pt.getActualTypeArguments().length == 1 ) {
					Type arg = pt.getActualTypeArguments()[0];
					return arg instanceof Class && Enum.class.isAssignableFrom( (Class) arg);
				}
			}
		}
		return false;
    }


	/**
     * Gets explicitly defined terms, e.g. on package, class or mixin.
     *
     * @param annotatedElement
     *         to find terms
     * @param name
     *         of annotated element, i.e. class name or package name
     * @return terms
     */
    private Map<String, Object> getAnnotatedTerms(AnnotatedElement annotatedElement, String name) {
        final Terms annotatedTerms = findAnnotation(annotatedElement, Terms.class);
        final Term annotatedTerm = findAnnotation(annotatedElement, Term.class);

        if (annotatedTerms != null && annotatedTerm != null) {
            throw new IllegalStateException("found both @Terms and @Term in " + name + ", use either one or the other");
        }
        Map<String, Object> annotatedTermsMap = new LinkedHashMap<>();
        if (annotatedTerms != null) {
            final Term[] terms = annotatedTerms.value();
            for (Term term : terms) {
                collectTerms(name, annotatedTermsMap, term);
            }
        } else if (annotatedTerm != null) { // only one term
            collectTerms(name, annotatedTermsMap, annotatedTerm);
        }
        return annotatedTermsMap;
    }

    private void collectTerms(String name, Map<String, Object> annotatedTermsMap, Term term) {
        final String define = term.define();
        final String as = term.as();
        final boolean reverse = term.reverse();
        if (annotatedTermsMap.containsKey(as)) {
            throw new IllegalStateException("duplicate definition of term '" + define + "' in " + name);
        }
        if (!reverse) {
            annotatedTermsMap.put(define, as);
        } else {
            Map<String, String> reverseTerm = new LinkedHashMap<>();
            reverseTerm.put(JsonLdKeywords.AT_REVERSE, as);
            annotatedTermsMap.put(define, reverseTerm);
        }
    }

    private Object getNestedContextProviderFromMixin(MixinSource mixinSource, Object bean, Class<?> mixinClass) {
        // TODO does not consider Collection<EntityModel> or Collection<PersistentEntityResource> to find mixin of
        // object wrapped in resource
        // TODO does not consider package of object wrapped in resource
        // TODO: we do not know CollectionModel here
        if (mixinClass == null) {
            return null;
        }
        try {
            Method mixinContextProvider = getContextProvider(mixinClass);
            if (mixinContextProvider == null) {
                return null;
            }
            Class<?> beanClass = bean.getClass();
            Object contextual = beanClass.getMethod(mixinContextProvider.getName())
                    .invoke(bean);
            Object ret = null;
            if (contextual instanceof Collection) {
                Collection collection = (Collection) contextual;
                if (!collection.isEmpty()) {
                    Object item = collection.iterator()
                            .next();
                    final Class<?> mixInClass = mixinSource.findMixInClassFor(item.getClass());
                    if (mixInClass == null) {
                        ret = item;
                    } else {
                        ret = getNestedContextProviderFromMixin(mixinSource, item, mixInClass);
                    }
                }
            } else if (contextual instanceof Map) {
                Map map = (Map) contextual;
                if (!map.isEmpty()) {
                    Object item = map.values()
                            .iterator()
                            .next();
                    final Class<?> mixInClass = mixinSource.findMixInClassFor(item.getClass());
                    if (mixInClass == null) {
                        ret = item;
                    } else {
                        ret = getNestedContextProviderFromMixin(mixinSource, item, mixInClass);
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
        Method contextProvider = AnnotationUtils.getAnnotatedMethod(beanClass, annotation);
        if (contextProvider != null && contextProvider.getParameterTypes().length > 0) {
            throw new IllegalStateException("the context provider method " + contextProvider.getName() + " must not " +
                    "have arguments");
        }
        return contextProvider;
    }

    private void addEnumTerms(Map<String, Object> termsMap, Expose expose, String name,
                              Enum value) throws NoSuchFieldException {
        if (value != null) {
            Map<String, String> map = new LinkedHashMap<>();
            if (expose != null) {
                map.put(JsonLdKeywords.AT_ID, expose.value());
            }
            map.put(JsonLdKeywords.AT_TYPE, JsonLdKeywords.AT_VOCAB);
            termsMap.put(name, map);
            final Expose enumValueExpose = findAnnotation(value.getClass()
                    .getField(value.name()), Expose.class);

            if (enumValueExpose != null) {
                termsMap.put(value.toString(), enumValueExpose.value());
            } else {
                // might use upperToCamelCase if nothing is exposed
                final String camelCaseEnumValue = WordUtils.capitalizeFully(value.toString(), '_')
                        .replaceAll("_", "");
                termsMap.put(value.toString(), camelCaseEnumValue);
            }
        }
    }


    public String vocabFromClassOrPackage(Class<?> clazz) {
        // vocab and terms of defining class: class and package
        final Vocab packageVocab = findAnnotation(clazz
                .getPackage(), Vocab.class);
        final Vocab classVocab = findAnnotation(clazz, Vocab.class);

        String vocab;
        if (classVocab != null) {
            vocab = classVocab.value(); // wins over package
        } else if (packageVocab != null) {
            vocab = packageVocab.value(); // wins over context provider
        } else {
            vocab = null;
        }
        return vocab;
    }

    public Map<String, Object> termsFromClass(Class<?> clazz) {
        Map<String, Object> termsMap = getAnnotatedTerms(clazz.getPackage(), clazz.getPackage()
                .getName());
        Map<String, Object> classTermsMap = getAnnotatedTerms(clazz, clazz.getName());

        // class terms override package terms
        termsMap.putAll(classTermsMap);
        return termsMap;
    }

    public void setProxyUnwrapper(ProxyUnwrapper proxyUnwrapper) {
        this.proxyUnwrapper = proxyUnwrapper;
    }
}
