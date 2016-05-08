package de.escalon.hypermedia.spring.halforms;

import de.escalon.hypermedia.ResourceSupportVisitor;
import de.escalon.hypermedia.ResourceTraversal;
import de.escalon.hypermedia.affordance.ActionDescriptor;
import de.escalon.hypermedia.affordance.Affordance;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.Links;
import org.springframework.hateoas.ResourceSupport;
import org.springframework.hateoas.hal.CurieProvider;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.converter.AbstractHttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.util.MultiValueMap;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.web.util.UriTemplate;

import javax.servlet.http.HttpServletRequest;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Created by Dietrich on 07.05.2016.
 */
public class HalFormsMessageConverter extends AbstractHttpMessageConverter<Object> {

    private ResourceTraversal resourceTraversal = new ResourceTraversal();

    @Autowired
    private CurieProvider curieProvider;


    @Override
    protected boolean supports(Class<?> clazz) {
        return true;
    }

    @Override
    protected Object readInternal(Class<?> clazz, HttpInputMessage inputMessage) throws IOException,
            HttpMessageNotReadableException {
        return null;
    }

    @Override
    protected void writeInternal(Object o, HttpOutputMessage outputMessage) throws IOException,
            HttpMessageNotWritableException {
        RequestAttributes requestAttributes =
                RequestContextHolder.getRequestAttributes();

        if (requestAttributes instanceof ServletRequestAttributes) {
            HttpServletRequest request =
                    ((ServletRequestAttributes) requestAttributes).getRequest();
            Object requestedRelAttr = request.getAttribute("org.springframework.hateoas.FORWARDED_RELATION_TYPE");
            if (requestedRelAttr == null) {
                throw new IllegalStateException(
                        "Could not find relation type, did you configure a HalFormsForwardingFilter?");
            }
            String requestedRel = requestedRelAttr.toString();

            UriComponents uriComponents = UriComponentsBuilder.fromUriString(request.getRequestURI() + "?" + request
                    .getQueryString())
                    .build();
            MultiValueMap<String, String> queryParams = uriComponents.getQueryParams();
            String dest = queryParams.getFirst("dest");

            String rel = getRelValueFromRelUrl(requestedRel);

            if (rel != null) {
                ResourceSupport resource = (ResourceSupport) o;
                FindLinkVisitor visitor = new FindLinkVisitor(rel, dest);
                resourceTraversal.traverseResource(visitor, o);
                Link linkToRenderAsForm = visitor.getLink();
                //HalFormUtils.toHalForm(link);
                BufferedWriter body = new BufferedWriter(new OutputStreamWriter(outputMessage.getBody()));
                body.write(linkToRenderAsForm.toString());
                body.newLine();
                if (linkToRenderAsForm instanceof Affordance) {
                    Affordance affordance = (Affordance) linkToRenderAsForm;
                    List<ActionDescriptor> actionDescriptors = affordance.getActionDescriptors();
                    for (ActionDescriptor actionDescriptor : actionDescriptors) {
                        body.write(actionDescriptor.getHttpMethod());
                        body.newLine();
                        if (actionDescriptor.hasRequestBody()) {
                            body.write(actionDescriptor.getRequestBody()
                                    .getParameterType()
                                    .getName());
                            body.newLine();
                        }
                    }
                }
                body.flush();


            }
        }

    }

    @Nullable
    private String getRelValueFromRelUrl(String requestedRel) {
        Collection<?> curieInformation =
                curieProvider.getCurieInformation(new Links());

        String rel = null;
        for (Object info : curieInformation) {
            if (info instanceof Link) {
                Link curieLink = (Link) info;
                String curieTemplate = curieLink.getHref();
                // the springframework.web.util.UriTemplate
                UriTemplate uriTemplate = new UriTemplate(curieTemplate);
                if (uriTemplate.matches(requestedRel)) {
                    Map<String, String> match =
                            uriTemplate.match(requestedRel);
                    rel = match.values()
                            .iterator()
                            .next();
                    break;
                }
            }
        }
        return rel;
    }

    class FindLinkVisitor extends DefaultResourceSupportVisitor {

        private final String rel;
        private final String href;
        private Link link;

        public FindLinkVisitor(String rel, String href) {

            this.rel = rel;
            this.href = href;
        }


        @Override
        public boolean visitLinks(List<Link> links) {
            for (Link link : links) {
                if (link.getRel()
                        .equals(rel) && (link.getHref()
                        .equals(href))) {
                    this.link = link;
                    return false;
                }
            }
            return true;
        }

        public Link getLink() {
            return link;
        }
    }

    ;

    class DefaultResourceSupportVisitor implements ResourceSupportVisitor {

        @Override
        public boolean visitLinks(List<Link> links) {
            return true;
        }

        @Override
        public boolean visitEnterCollection(Collection<?> collection) {
            return true;
        }

        @Override
        public boolean visitLeaveCollection(Collection<?> collection) {
            return true;
        }

        @Override
        public boolean visitEnterProperty(String name, Class<?> propertyType, Object value) {
            return true;
        }

        @Override
        public boolean visitProperty(String name, Object value, Object o) {
            return true;
        }

        @Override
        public boolean visitLeaveProperty(String name, Class<?> propertyType, Object value) {
            return true;
        }
    }

    ;
}
