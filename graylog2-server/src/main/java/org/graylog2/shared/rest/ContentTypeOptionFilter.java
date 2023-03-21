package org.graylog2.shared.rest;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import java.io.IOException;

public class ContentTypeOptionFilter implements ContainerResponseFilter {
    public static final String X_CONTENT_TYPE_OPTIONS = "X-Content-Type-Options";

    @Override
    public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) throws IOException {
        responseContext.getHeaders().add(X_CONTENT_TYPE_OPTIONS, "nosniff");
    }
}
