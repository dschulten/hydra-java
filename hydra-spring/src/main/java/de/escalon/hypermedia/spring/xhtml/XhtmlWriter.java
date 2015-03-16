package de.escalon.hypermedia.spring.xhtml;

import de.escalon.hypermedia.DataType;
import de.escalon.hypermedia.PropertyUtil;
import de.escalon.hypermedia.action.ActionDescriptor;
import de.escalon.hypermedia.action.ActionInputParameter;
import de.escalon.hypermedia.action.Type;
import de.escalon.hypermedia.hydra.mapping.Expose;
import de.escalon.hypermedia.hydra.serialize.JacksonHydraSerializer;
import de.escalon.hypermedia.spring.Affordance;
import org.springframework.beans.BeanUtils;
import org.springframework.core.MethodParameter;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.convert.Property;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.TemplateVariable;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.RequestMethod;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.io.IOException;
import java.io.Writer;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static de.escalon.hypermedia.spring.xhtml.XhtmlWriter.OptionalAttributes.attr;

/**
 * Created by Dietrich on 09.02.2015.
 */
public class XhtmlWriter extends Writer {
    private Writer writer;

    public static final String HTML_START = "" + //
            "<?xml version='1.0' encoding='UTF-8' ?>" + // formatter
            "<!DOCTYPE html>" + //
            "<html xmlns='http://www.w3.org/1999/xhtml'>" + //
            "  <head>" + //
            "    <title>%s</title>" + //
            "  </head>" + //
            "  <body>";


    public static final String HTML_END = "" + //
            "  </body>" + //
            "</html>";


    public XhtmlWriter(Writer writer) {
        this.writer = writer;
    }

    public void beginDiv() throws IOException {
        writer.write("<div>");
    }

    public void endDiv() throws IOException {
        writer.write("</div>");
    }

    @Override
    public void write(char[] cbuf, int off, int len) throws IOException {
        writer.write(cbuf, off, len);
    }

    @Override
    public void flush() throws IOException {
        writer.flush();
    }

    @Override
    public void close() throws IOException {
        writer.close();
    }

    public void beginUnorderedList() throws IOException {
        writer.write("<ul>");
    }

    public void endUnorderedList() throws IOException {
        writer.write("</ul>");
    }

    public void beginListItem() throws IOException {
        writer.write("<li>");
    }

    public void endListItem() throws IOException {
        writer.write("</li>");
    }


    public void beginSpan() throws IOException {
        writer.write("<span>");
    }

    public void endSpan() throws IOException {
        writer.write("</span>");
    }

    public void writeSpan(Object value) throws IOException {
        beginSpan();
        writer.write(value.toString());
        endSpan();

    }

    public static class OptionalAttributes {

        private Map<String, String> attributes = new LinkedHashMap<String, String>();

        /**
         * Creates OptionalAttributes with one optional attribute having name if value is not null.
         *
         * @param name  of first attribute
         * @param value may be null
         * @return builder with one attribute, attr builder if value is null
         */
        public static OptionalAttributes attr(String name, String value) {
            Assert.isTrue(name != null && value != null || value == null);
            OptionalAttributes attributeBuilder = new OptionalAttributes();
            addAttributeIfValueNotNull(name, value, attributeBuilder);
            return attributeBuilder;
        }

        private static void addAttributeIfValueNotNull(String name, String value, OptionalAttributes attributeBuilder) {
            if (value != null) {
                attributeBuilder.attributes.put(name, value);
            }
        }


        public OptionalAttributes and(String name, String value) {
            addAttributeIfValueNotNull(name, value, this);
            return this;
        }

        public Map<String, String> build() {
            return attributes;
        }

        public static OptionalAttributes attr() {
            return attr(null, null);
        }
    }


    public void addLinks(List<Link> links) throws IOException {
        for (Link link : links) {

            if (link instanceof Affordance) {
                Affordance affordance = (Affordance) link;
                List<ActionDescriptor> actionDescriptors = affordance.getActionDescriptors();
                if (actionDescriptors.isEmpty()) {
                    writeLinkWithoutActionDescriptor(affordance);
                } else {
                    if (affordance.isTemplated()) {
                        Affordance expanded = affordance.expand();
                        for (ActionDescriptor actionDescriptor : actionDescriptors) {
                            RequestMethod httpMethod = actionDescriptor.getHttpMethod();
                            switch (httpMethod) {
                                case GET:
                                    // TODO use appendForm here?
                                    beginForm(OptionalAttributes.attr("action", expanded.getHref())
                                            .and("method", "GET"));
                                    List<TemplateVariable> variables = link.getVariables();
                                    for (TemplateVariable variable : variables) {
                                        String variableName = variable.getName();
                                        String label = variable.hasDescription() ?
                                                variable.getDescription() : variableName;
                                        beginLabel(label);

                                        writeInput(variableName, Type.TEXT);
                                        endLabel();
                                    }
                                    input(Type.SUBMIT, "Query");
                                    endForm();
                                    break;
                            }

                            // GET form for template
                            // html does not allow templated action for POST

                        }
                        // TODO write description of additional methods?
                    } else {
                        for (ActionDescriptor actionDescriptor : actionDescriptors) {
                            // TODO handle PUT and DELETE as POST
                            // TODO write documentation about the supported action and maybe fields?
                            // especially to make PUT and DELETE obvious
                            appendForm(affordance, actionDescriptor);
                        }
                    }
                }
            } else { // simple link
                writeLinkWithoutActionDescriptor(link);
            }
        }
    }

    private void writeLinkWithoutActionDescriptor(Link link) throws IOException {
        if (link.isTemplated()) {
            Link expanded = link.expand();
            beginForm(OptionalAttributes.attr("action", expanded.getHref())
                    .and("method", "GET"));
            List<TemplateVariable> variables = link.getVariables();
            for (TemplateVariable variable : variables) {
                String variableName = variable.getName();
                String label = variable.hasDescription() ? variable.getDescription() : variableName;
                beginLabel(label);

                writeInput(variableName, Type.TEXT);
                endLabel();
            }
        } else {
            String rel = link.getRel();
            String title = (rel != null ? rel : link.getHref());
            writeAnchor(OptionalAttributes.attr("href", link.getHref()), title);
        }
    }


    private void writeInput(String name, Type type) throws IOException {
        input(name, type, OptionalAttributes.attr(null, null));
    }

    /**
     * Classic submit or reset button.
     *
     * @param type  submit or reset
     * @param value caption on the button
     * @throws IOException
     */
    private void input(Type type, String value) throws IOException {
        write("<input type=\"");
        write(type.toString());
        write("\" ");
        write("value");
        write("=");
        quote();
        write(value);
        quote();
        write("/>");
    }

    private void input(String name, Type type, OptionalAttributes attributes) throws IOException {
        write("<input name=\"");
        write(name);
        write("\" type=\"");
        write(type.toString());
        write("\" ");
        writeAttributes(attributes);
        write("/>");
    }

    private void beginLabel(String label) throws IOException {
        beginLabel(label, attr());
    }

    private void beginLabel(String label, OptionalAttributes attributes) throws IOException {
        write("<label");
        writeAttributes(attributes);
        endTag();
        write(label + ": ");
    }

    private void endLabel() throws IOException {
        write("</label>");
    }


    private void beginForm(OptionalAttributes attrs) throws IOException {
        write("<form ");
        writeAttributes(attrs);
    }

    private void writeAttributes(OptionalAttributes attrs) throws IOException {
        Map<String, String> attributes = attrs.build();
        for (Map.Entry<String, String> entry : attributes.entrySet()) {
            write(" ");
            write(entry.getKey());
            write("=");
            quote();
            write(entry.getValue());
            quote();
        }
    }

    private void quote() throws IOException {
        write("\"");
    }

    private void endForm() throws IOException {
        write("</form>");
    }

    private void writeAnchor(OptionalAttributes attrs, String value) throws IOException {
        write("<a ");
        writeAttributes(attrs);
        endTag();
        write(value);
        write("</a>");
    }

    private void appendForm(Affordance affordance, ActionDescriptor actionDescriptor) throws IOException {
        // TODO DELETE and PUT affordances as POST?
        // see http://stackoverflow.com/questions/13629653/using-put-and-delete-methods-in-spring-mvc
        // see http://docs.spring.io/spring/docs/3.0.x/javadoc-api/org/springframework/web/filter/HiddenHttpMethodFilter.html
        String formName = actionDescriptor.getActionName();

        beginForm(OptionalAttributes.attr("action", affordance.getHref())
                .and("method", actionDescriptor.getHttpMethod()
                        .name())
                .and("name", formName));
        write("<h1>");
        String formH1 = "Form " + formName;
        write(formH1);
        write("</h1>");

        // build the form
        if (actionDescriptor.hasRequestBody()) {
            ActionInputParameter requestBody = actionDescriptor.getRequestBody();
            Class<?> nestedParameterType = requestBody.getNestedParameterType();
            recurseBeanProperties(nestedParameterType, actionDescriptor, requestBody, requestBody.getCallValue());
        } else {
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
                        appendSelectMulti(writer, requestParamName, possibleValues,
                                actionInputParameter.getCallValues());
                    } else {
                        appendSelectOne(writer, requestParamName, possibleValues,
                                actionInputParameter.getCallValue());
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
                            appendInput(requestParamName, actionInputParameter, value);
                        }
                    } else {
                        String callValueFormatted = actionInputParameter.getCallValueFormatted();
                        appendInput(requestParamName, actionInputParameter, callValueFormatted);
                    }
                }
            }
        }
        input(Type.SUBMIT, "Submit");
        endForm();
    }

    private void recurseBeanProperties(Class<?> beanType, ActionDescriptor actionDescriptor,
                                       ActionInputParameter actionInputParameter, Object currentCallValue)
            throws IOException {
        // TODO support Option provider by other method args?
        final BeanInfo beanInfo = getBeanInfo(beanType);
        final PropertyDescriptor[] propertyDescriptors = beanInfo.getPropertyDescriptors();
        // TODO collection and map

        // TODO non-writable properties and public fields; make sure the inputs are part of a form
        for (PropertyDescriptor propertyDescriptor : propertyDescriptors) {
            final Method writeMethod = propertyDescriptor.getWriteMethod();
            if (writeMethod == null) {
                continue;
            }
            final Class<?> propertyType = propertyDescriptor.getPropertyType();

            String propertyName = propertyDescriptor.getName();

            if (DataType.isScalar(propertyType)) {

                final Property property = new Property(beanType,
                        propertyDescriptor.getReadMethod(),
                        propertyDescriptor.getWriteMethod(),
                        propertyDescriptor.getName());

                //Object callValue = actionInputParameter.getCallValue();
                Object propertyValue = null;
                if (currentCallValue != null) {
                    try {

                        BeanInfo info = Introspector.getBeanInfo(currentCallValue.getClass());
                        PropertyDescriptor[] pds = info.getPropertyDescriptors();
                        for (PropertyDescriptor pd : pds) {
                            if (propertyName.equals(pd.getName())) {
                                propertyValue = pd.getReadMethod()
                                        .invoke(currentCallValue);
                                break;
                            }
                        }
                    } catch (IntrospectionException e) {
                        e.printStackTrace();
                    } catch (InvocationTargetException e) {
                        e.printStackTrace();
                    } catch (IllegalAccessException e) {
                        e.printStackTrace();
                    }
                }
                ActionInputParameter propertySetterInputParameter = new ActionInputParameter(
                        new MethodParameter(propertyDescriptor.getWriteMethod(), 0), propertyValue);
                final Object[] possibleValues =
                        actionInputParameter.getPossibleValues(property, actionDescriptor);
                if (possibleValues.length > 0) {
                    if (actionInputParameter.isArrayOrCollection()) {
                        // TODO multiple formatted callvalues
                        appendSelectMulti(writer, propertyName, possibleValues,
                                propertySetterInputParameter.getCallValues());
                    } else {
                        appendSelectOne(writer, propertyName, possibleValues,
                                propertySetterInputParameter.getCallValue());
                    }
                } else {
                    //String callValueFormatted = actionInputParameter.getCallValueFormatted();
                    appendInput(propertyName, propertySetterInputParameter,
                            propertySetterInputParameter.getCallValue());
                }
            } else if (actionInputParameter.isArrayOrCollection()) {
                Object[] callValues = actionInputParameter.getCallValues();
                int items = callValues.length;
                for (int i = 0; i < items; i++) {
                    Object value;
                    if (i < callValues.length) {
                        value = callValues[i];
                    } else {
                        value = null;
                    }
                    recurseBeanProperties(actionInputParameter.getNestedParameterType(),
                            actionDescriptor, actionInputParameter, value);
                }
            } else {
                beginDiv();
                write(propertyName + ":");

                Object propertyValue = PropertyUtil.getPropertyValue(currentCallValue, propertyDescriptor);


                recurseBeanProperties(propertyType, actionDescriptor,
                        actionInputParameter, propertyValue);
                endDiv();
            }
        }

    }

    private BeanInfo getBeanInfo(Class<?> beanType) {
        try {
            return Introspector.getBeanInfo(beanType);
        } catch (IntrospectionException e) {
            throw new RuntimeException(e);
        }
    }

    private void appendInput(String requestParamName, ActionInputParameter actionInputParameter,
                             Object value) throws IOException {
        String fieldLabel = requestParamName;
        Type inputFieldType = actionInputParameter.getHtmlInputFieldType();
        String val = value == null ? "" : value.toString();
        if (!actionInputParameter.isRequestBody()) {
            beginDiv();
            if (Type.HIDDEN == inputFieldType) {
                input(requestParamName, inputFieldType, OptionalAttributes.attr("value", val));
            } else {
                if (actionInputParameter.hasInputConstraints()) {

                    beginLabel(fieldLabel);
                    OptionalAttributes attrs = OptionalAttributes.attr();
                    for (Map.Entry<String, Object> entry : actionInputParameter.getInputConstraints()
                            .entrySet()) {
                        attrs.and(entry.getKey(), entry.getValue()
                                .toString());
                    }
                    attrs.and("value", val);
                    input(requestParamName, inputFieldType, attrs);
                    endLabel();
                } else {
                    beginLabel(fieldLabel);
                    input(requestParamName, inputFieldType, attr("value", val));
                }
            }
            endDiv();
        }
    }

    private void appendSelectOne(Writer writer, String requestParamName, Object[] possibleValues,
                                 Object callValue) throws IOException {
        beginDiv();
        beginLabel(requestParamName, attr("for", requestParamName));
        beginSelect(requestParamName, requestParamName, possibleValues.length);
        for (Object possibleValue : possibleValues) {
            if (possibleValue.equals(callValue)) {
                option(possibleValue.toString(), attr("selected", "selected"));
            } else {
                option(possibleValue.toString());
            }
        }
        endSelect();
        endDiv();
    }

    private void option(String option) throws IOException {
        option(option, attr());
    }

    private void option(String option, OptionalAttributes attr) throws IOException {
        // <option selected='selected'>%s</option>
        beginTag("option");
        writeAttributes(attr);
        endTag();
        write(option);
        write("</option>");
    }

    private void beginTag(String tag) throws IOException {
        write("<");
        write(tag);

    }

    private void endTag() throws IOException {
        write(">");
    }

    private void beginSelect(String name, String id, int size) throws IOException {
        beginSelect(name, id, size, attr());
    }

    private void beginSelect(String name, String id, int size, OptionalAttributes attrs) throws IOException {
        beginTag("select");
        write(" name=");
        quote(name);
        write(" id=");
        quote(id);
        //write(" size=");
        //quote(Integer.toString(size));
        writeAttributes(attrs);
        endTag();
    }

    private void endSelect() throws IOException {
        write("</select>");
    }

    private void quote(String s) throws IOException {
        quote();
        write(s);
        quote();
    }

    private void appendSelectMulti(Writer writer, String requestParamName, Object[] possibleValues,
                                   Object[] actualValues) throws IOException {
        beginDiv();
        beginLabel(requestParamName, attr("for", requestParamName));
        beginSelect(requestParamName, requestParamName, possibleValues.length, attr("multiple", "multiple"));
        for (Object possibleValue : possibleValues) {
            if (arrayContains(actualValues, possibleValue)) {
                option(possibleValue.toString(), attr("selected", "selected"));
            } else {
                option(possibleValue.toString());
            }
        }
        endForm();
        endDiv();
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
