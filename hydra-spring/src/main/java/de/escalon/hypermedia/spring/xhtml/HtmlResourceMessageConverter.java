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

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import de.escalon.hypermedia.action.Type;
import de.escalon.hypermedia.action.ActionDescriptor;
import de.escalon.hypermedia.action.ActionInputParameter;
import de.escalon.hypermedia.spring.HypermediaTypes;
import de.escalon.hypermedia.spring.de.escalon.hypermedia.spring.jackson.JacksonHydraModule;
import de.escalon.hypermedia.spring.uber.NullValueSerializer;
import org.springframework.hateoas.*;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.AbstractHttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.util.FileCopyUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestMethod;

import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.lang.reflect.Field;
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
public class HtmlResourceMessageConverter extends AbstractHttpMessageConverter<Object> {

    /**
     * expects title
     */
    public static final String HTML_START = "" + //
            // "<?xml version='1.0' encoding='UTF-8' ?>" + // formatter
            "<!DOCTYPE html>" + //
            "<html xmlns='http://www.w3.org/1999/xhtml'>" + //
            "  <head>" + //
            "    <title>%s</title>" + //
            "  </head>" + //
            "  <body>";

    public static final String DIV_START = "" + //
            "    <div>";

    public static final String DIV_END = "" + //
            "    </div>";

    /**
     * expects action url, form name, form method, form h1
     */
    public static final String FORM_START = "" + //
            "    <form action='%s' name='%s' method='%s'>" + //
            "      <h1>%s</h1>"; //

    /**
     * expects input field label, type, name and value
     */
    public static final String FORM_INPUT_LABELED = "" + //
            "      <label>%s<input type='%s' name='%s' value='%s' /></label>";

    /**
     * expects input field label, type and name
     */
    public static final String FORM_INPUT_LABELED_START = "" + //
            "      <label>%s<input type='%s' name='%s'";

    /**
     * expects attribute name and value
     */
    public static final String FORM_INPUT_ATTRIBUTE = "" + //
            " %s='%s'";

    /**
     * expects input field value
     */
    public static final String FORM_INPUT_LABELED_END = "" + //
            " value='%s' /></label>";

    /**
     * expects input field type, name and value
     */
    public static final String FORM_INPUT = "" + //
            "      <input type='%s' name='%s' value='%s' />";

    /**
     * expects the name of the field the label is for and the label caption
     */
    public static final String FORM_LABEL_FOR = "" + //
            "      <label for='%s'>%s</label>";

    /**
     * expects select field name, id and size
     */
    public static final String FORM_SELECT_ONE_START = "" + //
            "      <select name='%s' id='%s' size='%d' >";

    /**
     * expects select field name, id and size
     */
    public static final String FORM_SELECT_MULTI_START = "" + //
            "      <select name='%s' id='%s' size='%d' multiple='multiple'>";

    /**
     * expects select value
     */
    public static final String FORM_SELECT_OPTION = "" + //
            "      <option>%s</option>";

    /**
     * expects select value
     */
    public static final String FORM_SELECT_OPTION_SELECTED = "" + //
            "      <option selected='selected'>%s</option>";

    /**
     * closes a select
     */
    public static final String FORM_SELECT_END = "" + //
            "      </select>";

    /**
     * closes the form
     */
    public static final String FORM_END = "" + //
            "      <input type='submit' value='Submit' />" + //
            "    </form>";
    public static final String HTML_END = "" + //
            "  </body>" + //
            "</html>";

    public HtmlResourceMessageConverter() {
        this.setSupportedMediaTypes(
                Arrays.asList(MediaType.TEXT_HTML));
    }

    @Override
    protected boolean supports(Class<?> clazz) {
        return true;
    }

    @Override
    protected Object readInternal(Class<? extends Object> clazz, HttpInputMessage inputMessage) throws IOException,
            HttpMessageNotReadableException {
        return new Object();
    }

    @Override
    protected void writeInternal(Object t, HttpOutputMessage outputMessage) throws IOException,
            HttpMessageNotWritableException {

        XhtmlWriter xhtmlWriter = new XhtmlWriter(new OutputStreamWriter(outputMessage.getBody()));

        xhtmlWriter.write(String.format(HTML_START, "Input Data"));
        writeResource(xhtmlWriter, t);
        xhtmlWriter.write(HTML_END);
        xhtmlWriter.flush();

//        StringBuilder sb = new StringBuilder();
//        sb.append(String.format(HTML_START, "Input Data"));
//
//        if (t instanceof ActionDescriptor[]) {
//            ActionDescriptor[] descriptors = (ActionDescriptor[]) t;
//            for (ActionDescriptor actionDescriptor : descriptors) {
//                appendForm(sb, actionDescriptor);
//            }
//        } else {
//            ActionDescriptor actionDescriptor = (ActionDescriptor) t;
//            appendForm(sb, actionDescriptor);
//        }
//        sb.append(HTML_END);
//        FileCopyUtils.copy(sb.toString()
//                .getBytes("UTF-8"), outputMessage.getBody());

    }

    static final Set<String> FILTER_RESOURCE_SUPPORT = new HashSet<String>(Arrays.asList("class", "links", "id"));
    static final Set<String> FILTER_BEAN = new HashSet<String>(Arrays.asList("class"));


//    public static class XhtmlOutput {
//
//        public void addLinks(List<Link> links) {
//        }
//
//        public void addData(Object itemNode) {
//
//        }
//    }
    /**
     * Recursively converts object to nodes of uber data.
     *
     * @param object to convert
     * @param writer to write to
     */
    public static void writeResource(XhtmlWriter writer, Object object) {
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
                    if(!propertyDescriptors.containsKey(name)) {
                        Object content = field.get(object);
                        writeAttribute(writer, name, content);
                    }
                }
                for (PropertyDescriptor propertyDescriptor : propertyDescriptors.values()) {
                    String name = propertyDescriptor.getName();
                    if (filtered.contains(name)) {
                        continue;
                    }
                    Object content = propertyDescriptor.getReadMethod().invoke(object);
                    writeAttribute(writer, name, content);
                }


            }
        } catch (Exception ex) {
            throw new RuntimeException("failed to transform object " + object, ex);
        }

    }

    private static void writeAttribute(XhtmlWriter writer, String name, Object content) throws IOException {
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

//    /**
//     * Converts link to uber node.
//     *
//     * @param link to convert
//     * @return uber link
//     */
//    public static UberNode toUberLink(Link link) {
//        UberNode uberLink = new UberNode();
//        uberLink.setRel(Arrays.asList(link.getRel()));
//        uberLink.setUrl(getUrlProperty(link));
//        uberLink.setModel(getModelProperty(link));
////		uberLink.setAction(UberAction.forRequestMethod(link.getRequestMethod()));
//        if (true) throw new UnsupportedOperationException();
//        return uberLink;
//    }

    private static String getModelProperty(Link link) {
        if (true) throw new UnsupportedOperationException();
        RequestMethod httpMethod = RequestMethod.DELETE;// link.getRequestMethod();
        UriTemplate uriTemplate = new UriTemplate(link.getHref());
        final String model;
        switch (httpMethod) {
            case GET:
            case DELETE: {
                model = buildModel(uriTemplate.getVariables(), "{?", ",", "}", "%s");
                break;
            }
            case POST:
            case PUT:
            case PATCH: {
                model = buildModel(uriTemplate.getVariables(), "", "&", "", "%s={%s}");
                break;
            }
            default:
                model = null;
        }
        return StringUtils.isEmpty(model) ? null : model;
    }

    private static String getUrlProperty(Link link) {
        throw new UnsupportedOperationException();
//		return UriComponentsBuilder.fromUriString(link.getBaseUri()).build().normalize().toString();
    }

    private static String buildModel(List<TemplateVariable> variables, String prefix, String separator, String suffix,
                                     String parameterTemplate) {
        StringBuilder sb = new StringBuilder();
        for (TemplateVariable variable : variables) {
            if (sb.length() == 0) {
                sb.append(prefix);
            } else {
                sb.append(separator);
            }
            String parameterName = variable.getName();
            sb.append(String.format(parameterTemplate, parameterName, parameterName));

        }
        if (sb.length() > 0) {
            sb.append(suffix);
        }
        return sb.toString();
    }

    private void appendForm(StringBuilder sb, ActionDescriptor actionDescriptor) {
        if(true) {
            throw new UnsupportedOperationException("converter only suitable for action descriptor as return value");
        }
        String action = ""; // TODO: was actionDescriptor.getActionLink();
        String formName = actionDescriptor.getActionName();

        String formH1 = "Form " + formName;
        sb.append(String.format(FORM_START, action, formName, actionDescriptor.getHttpMethod()
                .toString(), formH1));

        // build the form
        Collection<String> requestParams = actionDescriptor.getRequestParamNames();
        for (String requestParamName : requestParams) {
            ActionInputParameter actionInputParameter = actionDescriptor.getActionInputParameter(requestParamName);
            // TODO support list and matrix parameters?
            // TODO support RequestBody mapped by object marshaler? Look at bean properties in that case instead of
            // RequestParam arguments.
            // TODO support valid value ranges, possible values, value constraints?
            Object[] possibleValues = actionInputParameter.getPossibleValues(actionDescriptor);
            if (possibleValues.length > 0) {
                if (actionInputParameter.isArrayOrCollection()) {
                    appendSelectMulti(sb, requestParamName, possibleValues, actionInputParameter.getCallValues());
                } else {
                    appendSelectOne(sb, requestParamName, possibleValues, actionInputParameter.getCallValue());
                }
            } else {
                if (actionInputParameter.isArrayOrCollection()) {
                    Object[] callValues = actionInputParameter.getCallValues();
                    int items = callValues.length;
                    for (int i = 0; i < items; i++) {
                        Object value;
                        if (i < callValues.length) {
                            value = callValues[i];
                        } else {
                            value = null;
                        }
                        appendInput(sb, requestParamName, actionInputParameter, value);
                    }
                } else {
                    String callValueFormatted = actionInputParameter.getCallValueFormatted();
                    appendInput(sb, requestParamName, actionInputParameter, callValueFormatted);
                }
            }
        }
        sb.append(FORM_END);
    }

    private void appendInput(StringBuilder sb, String requestParamName, ActionInputParameter actionInputParameter,
                             Object value) {
        String fieldLabel = requestParamName + ": ";
        Type inputFieldType = actionInputParameter.getHtmlInputFieldType();
        String val = value == null ? "" : value.toString();
        if (!actionInputParameter.isRequestBody()) {
            sb.append(DIV_START);
            if (Type.HIDDEN == inputFieldType) {
                sb.append(String.format(FORM_INPUT, inputFieldType, requestParamName, val));
            } else {
                if (actionInputParameter.hasInputConstraints()) {
                    sb.append(String.format(FORM_INPUT_LABELED_START, fieldLabel, inputFieldType, requestParamName));
                    for (Entry<String, Object> entry : actionInputParameter.getInputConstraints()
                            .entrySet()) {
                        sb.append(String.format(FORM_INPUT_ATTRIBUTE, entry.getKey(), entry.getValue()));
                    }
                    sb.append(String.format(FORM_INPUT_LABELED_END, val));
                } else {
                    sb.append(String.format(FORM_INPUT_LABELED, fieldLabel, inputFieldType, requestParamName, val));
                }
            }
            sb.append(DIV_END);
        }
    }

    private void appendSelectOne(StringBuilder sb, String requestParamName, Object[] possibleValues, Object callValue) {
        sb.append(DIV_START);
        sb.append(String.format(FORM_LABEL_FOR, requestParamName, requestParamName + ": "));
        sb.append(String.format(FORM_SELECT_ONE_START, requestParamName, requestParamName, possibleValues.length));
        for (Object possibleValue : possibleValues) {
            if (possibleValue.equals(callValue)) {
                sb.append(String.format(FORM_SELECT_OPTION_SELECTED, possibleValue.toString()));
            } else {
                sb.append(String.format(FORM_SELECT_OPTION, possibleValue.toString()));
            }
        }
        sb.append(FORM_SELECT_END);
        sb.append(DIV_END);
    }

    private void appendSelectMulti(StringBuilder sb, String requestParamName, Object[] possibleValues,
                                   Object[] actualValues) {
        sb.append(DIV_START);
        sb.append(String.format(FORM_LABEL_FOR, requestParamName, requestParamName + ": "));
        sb.append(String.format(FORM_SELECT_MULTI_START, requestParamName, requestParamName, possibleValues.length));
        for (Object possibleValue : possibleValues) {
            if (arrayContains(actualValues, possibleValue)) {
                sb.append(String.format(FORM_SELECT_OPTION_SELECTED, possibleValue.toString()));
            } else {
                sb.append(String.format(FORM_SELECT_OPTION, possibleValue.toString()));
            }
        }
        sb.append(FORM_SELECT_END);
        sb.append(DIV_END);
    }

    private boolean arrayContains(Object[] values, Object value) {
        for (int i = 0; i < values.length; i++) {
            Object item = values[i];
            if (item.equals(value)) {
                return true;
            }
        }
        return false;
    }

}
