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
package org.graylog2.shared.rest;

import com.google.common.collect.ImmutableMap;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.core.Response;
import java.io.IOException;

public class CORSFilter implements ContainerRequestFilter, ContainerResponseFilter {

    // Allows Cross-Origin requests from ANY origin
    // These headers should only be used for development!
    private static final ImmutableMap<String, Object> CORS_HEADERS =
            ImmutableMap.of(
                    "Access-Control-Allow-Origin", "*",
                    "Access-Control-Allow-Credentials", true,
                    "Access-Control-Allow-Headers", "Authorization, Content-Type, X-Graylog-No-Session-Extension, X-Requested-With, X-Requested-By",
                    "Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS",
                    // In order to avoid redoing the preflight thingy for every request, see http://stackoverflow.com/a/12021982/1088469
                    "Access-Control-Max-Age", "600" // 10 minutes seems to be the maximum allowable value
            );

    @Override
    public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) throws IOException {
        // we have already added the necessary headers for OPTIONS requests below
        if ("options".equalsIgnoreCase(requestContext.getRequest().getMethod())) {
            if (Response.Status.Family.familyOf(responseContext.getStatus()) == Response.Status.Family.SUCCESSFUL) {
                return;
            }
            responseContext.setStatus(Response.Status.NO_CONTENT.getStatusCode());
            responseContext.setEntity("");
        }

        String origin = requestContext.getHeaders().getFirst("Origin");
        if (origin != null && !origin.isEmpty()) {
            CORS_HEADERS.forEach((k, v) -> responseContext.getHeaders().add(k, v));
        }
    }

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        // answer OPTIONS requests early so we don't have jersey produce WADL responses for them (we only use them for CORS preflight)
        if ("options".equalsIgnoreCase(requestContext.getRequest().getMethod())) {
            final Response.ResponseBuilder options = Response.noContent();
            String origin = requestContext.getHeaders().getFirst("Origin");
            if (origin != null && !origin.isEmpty()) {
                CORS_HEADERS.forEach(options::header);
                requestContext.abortWith(options.build());
            }
        }
    }
}
