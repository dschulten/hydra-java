package de.escalon.hypermedia.spring;

import org.springframework.util.MultiValueMap;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * Created by Dietrich on 07.05.2016.
 */
public class HalFormsForwardingFilter implements Filter {


    @Override
    public void init(FilterConfig filterConfig) throws ServletException {

    }

    @Override
    public void doFilter(ServletRequest req, ServletResponse response, FilterChain chain) throws IOException,
            ServletException {
        if (req instanceof HttpServletRequest) {

            HttpServletRequest request = (HttpServletRequest) req;
            String accept = request.getHeader("Accept");
            if (!"application/prs.hal-forms+json".equals(accept)) {
                chain.doFilter(request, response);
            }
            UriComponents uriComponents = UriComponentsBuilder.fromUriString(request.getRequestURI() + "?" + request
                    .getQueryString())
                    .build();
            MultiValueMap<String, String> queryParams = uriComponents.getQueryParams();
            String source = queryParams.getFirst("src");
            try {
                URI sourceUri = new URI(source);

                if (sourceUri.getPath()
                        .equals(request.getRequestURI())) {
                    chain.doFilter(request, response);
                }
                String contextPath = request.getContextPath();
                String servletPath = request.getServletPath();
                // after http(s)://
                String contextAndServlet = contextPath;//  + servletPath;
                int servletPathPos = source.indexOf(contextAndServlet);

                String relativeSourcePath = source.substring(servletPathPos + contextAndServlet.length());
                RequestDispatcher requestDispatcher = request.getRequestDispatcher(relativeSourcePath);
                request.setAttribute("org.springframework.hateoas.FORWARDED_RELATION_TYPE",
                        request.getRequestURL()
                                .toString());
                requestDispatcher.forward(request, response);
            } catch (URISyntaxException e) {
                chain.doFilter(request, response);
            }
        }
    }

    @Override
    public void destroy() {

    }
}
