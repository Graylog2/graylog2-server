package org.graylog2.shared.rest;

import com.google.common.base.Strings;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.core.Response;
import java.io.IOException;

public class NotAuthorizedResponseFilter implements ContainerResponseFilter {
    @Override
    public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) throws IOException {
        if (responseContext.getStatusInfo().equals(Response.Status.UNAUTHORIZED)) {
            final String requestedWith = requestContext.getHeaderString("X-Requested-With");
            if (!Strings.isNullOrEmpty(requestedWith) && requestedWith.equalsIgnoreCase("XMLHttpRequest")) {
                responseContext.getHeaders().remove("WWW-Authenticate");
            }
        }
    }
}
