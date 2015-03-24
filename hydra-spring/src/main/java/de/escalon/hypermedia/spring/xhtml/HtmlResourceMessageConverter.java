/*
 * Copyright (c) 2014. Escalon System-Entwicklung, Dietrich Schulten
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package de.escalon.hypermedia.spring.xhtml;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import de.escalon.hypermedia.DataType;
import de.escalon.hypermedia.spring.uber.NullValueSerializer;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.hateoas.Resource;
import org.springframework.hateoas.ResourceSupport;
import org.springframework.hateoas.Resources;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.AbstractHttpMessageConverter;
import org.springframework.http.converter.GenericHttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.util.*;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.net.URLDecoder;
import java.nio.charset.Charset;
import java.util.*;
import java.util.Map.Entry;

/**
 * Message converter which converts one ActionDescriptor or an array of ActionDescriptor items to an HTML page
 * containing one form per ActionDescriptor.
 * <p>
 * Add the following to your spring configuration to enable this converter:
 * </p>
 * <pre>
 *   &lt;bean
 *     class="org.springframework.web.servlet.mvc.annotation.AnnotationMethodHandlerAdapter"&gt;
 *     &lt;property name="messageConverters"&gt;
 *       &lt;list&gt;
 *         &lt;ref bean="jsonConverter" /&gt;
 *         &lt;ref bean="htmlFormMessageConverter" /&gt;
 *       &lt;/list&gt;
 *     &lt;/property&gt;
 *   &lt;/bean&gt;
 *
 *   &lt;bean id="htmlFormMessageConverter" class="de.escalon.hypermedia.spring.xhtml.HtmlResourceMessageConverter"&gt;
 *     &lt;property name="supportedMediaTypes" value="text/html" /&gt;
 *   &lt;/bean&gt;
 * </pre>
 *
 * @author Dietrich Schulten
 */
public class HtmlResourceMessageConverter extends AbstractHttpMessageConverter<Object>
        implements GenericHttpMessageConverter<Object> {

    private Charset charset = Charset.forName("UTF-8");

    public HtmlResourceMessageConverter() {
        this.setSupportedMediaTypes(
                Arrays.asList(MediaType.TEXT_HTML, MediaType.APPLICATION_FORM_URLENCODED));
    }

    @Override
    protected boolean supports(Class<?> clazz) {
        return true;
    }

    public Object read(java.lang.reflect.Type type, Class<?> contextClass, HttpInputMessage inputMessage)
            throws IOException, HttpMessageNotReadableException {

        final Class clazz;
        if (type instanceof Class) {
            clazz = (Class) type;
        } else if (type instanceof ParameterizedType) {
            ParameterizedType parameterizedType = (ParameterizedType) type;
            Type rawType = parameterizedType.getRawType();
            if (rawType instanceof Class) {
                clazz = (Class) rawType;
            } else {
                throw new IllegalArgumentException("unexpected raw type " + rawType);
            }
        } else {
            throw new IllegalArgumentException("unexpected type " + type);
        }
        return readInternal(clazz, inputMessage);

    }


    @Override
    protected Object readInternal(Class<? extends Object> clazz, HttpInputMessage inputMessage)
            throws IOException, HttpMessageNotReadableException {

        return readRequestBody(clazz, inputMessage);

    }

    private Object readRequestBody(Class<? extends Object> clazz, HttpInputMessage inputMessage)
            throws IOException {

        MediaType contentType = inputMessage.getHeaders()
                .getContentType();
        Charset charset =
                contentType.getCharSet() != null ?
                        contentType.getCharSet() : this.charset;
        String body = StreamUtils.copyToString(inputMessage.getBody(), charset);

        String[] pairs = StringUtils.tokenizeToStringArray(body, "&");

        MultiValueMap<String, String> formValues =
                new LinkedMultiValueMap<String, String>(pairs.length);

        for (String pair : pairs) {
            int idx = pair.indexOf('=');
            if (idx == -1) {
                formValues.add(URLDecoder.decode(pair, charset.name()), null);
            } else {
                String name =
                        URLDecoder.decode(pair.substring(0, idx), charset.name());
                String value =
                        URLDecoder.decode(pair.substring(idx + 1), charset.name());
                formValues.add(name, value);
            }
        }

        return recursivelyCreateObject(clazz, formValues);


    }

    private Object recursivelyCreateObject(Class<? extends Object> clazz,
                                           MultiValueMap<String, String> formValues) {

        if (Map.class.isAssignableFrom(clazz)) {
            throw new IllegalArgumentException("Map not supported");
        } else if (Collection.class.isAssignableFrom(clazz)) {
            throw new IllegalArgumentException("Collection not supported");
        } else {
            try {
                Constructor[] constructors = clazz.getConstructors();
                Constructor constructor = findDefaultCtor(constructors);
                if (constructor == null) {
                    constructor = findJsonCreator(constructors);
                }
                Assert.notNull(constructor, "no default constructor or JsonCreator found");
                int parameterCount = constructor.getParameterTypes().length;
                Object[] args = new Object[parameterCount];
                if (parameterCount > 0) {
                    Annotation[][] annotationsOnParameters = constructor.getParameterAnnotations();
                    Class[] parameters = constructor.getParameterTypes();
                    int paramIndex = 0;
                    for (Annotation[] annotationsOnParameter : annotationsOnParameters) {
                        for (Annotation annotation : annotationsOnParameter) {
                            if (JsonProperty.class == annotation.annotationType()) {
                                JsonProperty jsonProperty = (JsonProperty) annotation;
                                String paramName = jsonProperty.value();
                                List<String> formValue = formValues.get(paramName);
                                Class<?> parameterType = parameters[paramIndex];
                                if (DataType.isSingleValueType(parameterType)) {
                                    if (formValue != null) {
                                        if (formValue.size() == 1) {
                                            args[paramIndex++] =
                                                    DataType.asType(parameterType, formValue.get(0));
                                        } else {
//                                        // TODO create proper collection type
                                            throw new IllegalArgumentException("variable list not supported");
//                                        List<Object> listValue = new ArrayList<Object>();
//                                        for (String item : formValue) {
//                                            listValue.add(DataType.asType(parameterType, formValue.get(0)));
//                                        }
//                                        args[paramIndex++] = listValue;
                                        }
                                    } else {
                                        args[paramIndex++] = null;
                                    }
                                } else {
                                    args[paramIndex++] = recursivelyCreateObject(parameterType, formValues);
                                }
                            }
                        }
                    }
                    Assert.isTrue(args.length == paramIndex,
                            "not all constructor arguments of @JsonCreator are annotated with @JsonProperty");
                }
                Object ret = constructor.newInstance(args);
                BeanInfo beanInfo = Introspector.getBeanInfo(clazz);
                PropertyDescriptor[] propertyDescriptors =
                        beanInfo.getPropertyDescriptors();
                for (PropertyDescriptor propertyDescriptor : propertyDescriptors) {
                    Method writeMethod = propertyDescriptor.getWriteMethod();
                    String name = propertyDescriptor.getName();
                    List<String> strings = formValues.get(name);
                    if (writeMethod != null && strings != null && strings.size() == 1) {
                        writeMethod.invoke(ret, DataType.asType(propertyDescriptor.getPropertyType(),
                                strings.get(0))); // TODO lists, consume values from ctor
                    }
                }
                return ret;
            } catch (Exception e) {
                throw new RuntimeException("Failed to instantiate bean " + clazz.getName(),
                        e);
            }
        }
    }

    private Constructor findDefaultCtor(Constructor[] constructors) {
        // TODO duplicate on XhtmlWriter
        Constructor constructor = null;
        for (Constructor ctor : constructors) {
            if (ctor.getParameterTypes().length == 0) {
                constructor = ctor;
            }
        }
        return constructor;
    }

    private Constructor findJsonCreator(Constructor[] constructors) {
        // TODO duplicate on XhtmlWriter
        Constructor constructor = null;
        for (Constructor ctor : constructors) {
            if (AnnotationUtils.getAnnotation(ctor, JsonCreator.class) != null) {
                constructor = ctor;
                break;
            }
        }
        return constructor;
    }

    @Override
    protected void writeInternal(Object t, HttpOutputMessage outputMessage) throws IOException,
            HttpMessageNotWritableException {

        XhtmlWriter xhtmlWriter = new XhtmlWriter(new OutputStreamWriter(outputMessage.getBody()));
        xhtmlWriter.beginHtml("Input Data");
        writeResource(xhtmlWriter, t);
        xhtmlWriter.endHtml();
        xhtmlWriter.flush();

    }

    static final Set<String> FILTER_RESOURCE_SUPPORT = new HashSet<String>(Arrays.asList("class", "links", "id"));
    static final Set<String> FILTER_BEAN = new HashSet<String>(Arrays.asList("class"));

    /**
     * Recursively converts object to nodes of uber data.
     *
     * @param object to convert
     * @param writer to write to
     */
    private void writeResource(XhtmlWriter writer, Object object) {
        Set<String> filtered = FILTER_RESOURCE_SUPPORT;
        if (object == null) {
            return;
        }
        try {
            // TODO: move all returns to else branch of property descriptor handling
            if (object instanceof Resource) {
                Resource<?> resource = (Resource<?>) object;
                writer.addLinks(resource.getLinks());
                writeResource(writer, resource.getContent());
                return;
            } else if (object instanceof Resources) {
                Resources<?> resources = (Resources<?>) object;

                // TODO set name using EVO see HypermediaSupportBeanDefinitionRegistrar
                writer.addLinks(resources.getLinks());

                Collection<?> content = resources.getContent();
                writeResource(writer, content);
                return;
            } else if (object instanceof ResourceSupport) {
                ResourceSupport resource = (ResourceSupport) object;

                writer.addLinks(resource.getLinks());

                // wrap object attributes below to avoid endless loop

            } else if (object instanceof Collection) {
                Collection<?> collection = (Collection<?>) object;
                writer.beginUnorderedList();
                for (Object item : collection) {
                    writer.beginListItem();
                    writeResource(writer, item);
                    writer.endListItem();
                }
                writer.endUnorderedList();
                return;
            }
            if (object instanceof Map) {
                Map<?, ?> map = (Map<?, ?>) object;
                for (Entry<?, ?> entry : map.entrySet()) {
                    String name = entry.getKey()
                            .toString();
                    Object content = entry.getValue();
                    writeAttribute(writer, name, content);
                }
            } else if (object instanceof Enum) {
                writer.writeSpan(((Enum) object).name());
            } else if (object instanceof Currency) {
                // TODO configurable classes which should be rendered with toString
                // or use JsonSerializer?
                writer.writeSpan(object.toString());
            } else {
                Class<?> aClass = object.getClass();
                Map<String, PropertyDescriptor> propertyDescriptors = getPropertyDescriptors(object);
                Field[] fields = aClass.getFields();
                for (Field field : fields) {
                    String name = field.getName();
                    if (!propertyDescriptors.containsKey(name)) {
                        Object content = field.get(object);
                        writeAttribute(writer, name, content);
                    }
                }
                for (PropertyDescriptor propertyDescriptor : propertyDescriptors.values()) {
                    String name = propertyDescriptor.getName();
                    if (filtered.contains(name)) {
                        continue;
                    }
                    Object content = propertyDescriptor.getReadMethod()
                            .invoke(object);
                    writeAttribute(writer, name, content);
                }


            }
        } catch (Exception ex) {
            throw new RuntimeException("failed to transform object " + object, ex);
        }

    }

    private void writeAttribute(XhtmlWriter writer, String name, Object content) throws IOException {
        Object value = getContentAsScalarValue(content);

        writer.beginDiv();
        writer.writeSpan(name);
        writer.write(": ");
        if (value != null && value != NULL_VALUE) {
            writer.writeSpan(value.toString());
        } else {
            writeResource(writer, content);
        }
        writer.endDiv();
    }

    private static Map<String, PropertyDescriptor> getPropertyDescriptors(Object bean) {
        try {
            PropertyDescriptor[] propertyDescriptors = Introspector.getBeanInfo(bean.getClass())
                    .getPropertyDescriptors();
            Map<String, PropertyDescriptor> ret = new HashMap<String, PropertyDescriptor>();
            for (PropertyDescriptor propertyDescriptor : propertyDescriptors) {
                ret.put(propertyDescriptor.getName(), propertyDescriptor);
            }
            return ret;
        } catch (IntrospectionException e) {
            throw new RuntimeException("failed to get property descriptors of bean " + bean, e);
        }
    }

    @Override
    public boolean canRead(java.lang.reflect.Type type, Class<?> contextClass, MediaType mediaType) {
        if (MediaType.APPLICATION_FORM_URLENCODED == mediaType) {
            return true;
        } else {
            return false;
        }
    }

    static class NullValue {

    }

    /**
     * Uses {@link NullValueSerializer} to render undefined values as null.
     */
    public static final NullValue NULL_VALUE = new NullValue();

    private static Object getContentAsScalarValue(Object content) {
        Object value = null;

        if (content == null) {
            value = NULL_VALUE;
        } else if (content instanceof String || content instanceof Number || content.equals(false) || content.equals(true)) {
            value = content;
        }
        return value;
    }


}
