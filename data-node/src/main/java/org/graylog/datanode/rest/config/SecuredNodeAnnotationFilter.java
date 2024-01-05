/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
 */
package org.graylog.datanode.rest.config;

import org.glassfish.jersey.server.ContainerRequest;
import org.glassfish.jersey.server.model.ResourceMethod;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.Objects;

public class SecuredNodeAnnotationFilter implements ContainerRequestFilter {

    private final boolean isInsecureNode;

    public SecuredNodeAnnotationFilter(boolean isInsecureNode) {
        this.isInsecureNode = isInsecureNode;
    }

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        if (requestContext instanceof ContainerRequest request) {

            final ResourceMethod method = request.getUriInfo().getMatchedResourceMethod();
            final OnlyInSecuredNode annotation = method.getInvocable()
                    .getHandlingMethod().getAnnotation(OnlyInSecuredNode.class);

            if (Objects.nonNull(annotation) && isInsecureNode) {
                requestContext.abortWith(Response.status(Response.Status.UNAUTHORIZED)
                        .entity("This resource can only be accessed in secured data nodes.")
                        .type(MediaType.TEXT_PLAIN_TYPE)
                        .build());
            }

        } else {
            requestContext.abortWith(Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Server is no Jetty Server.")
                    .type(MediaType.TEXT_PLAIN_TYPE)
                    .build());
        }

    }
}
