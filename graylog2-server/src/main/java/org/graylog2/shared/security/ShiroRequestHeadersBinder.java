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
package org.graylog2.shared.security;

import joptsimple.internal.Strings;
import org.apache.shiro.util.ThreadContext;

import javax.annotation.Priority;
import javax.ws.rs.Priorities;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.core.MultivaluedMap;
import java.io.IOException;
import java.util.Optional;

/**
 * This filter makes the request headers accessible within Shiro's {@link ThreadContext}.
 */
// Needs to run after RequestIdFilter
@Priority(Priorities.AUTHENTICATION - 8)
public class ShiroRequestHeadersBinder implements ContainerRequestFilter, ContainerResponseFilter {
    public static final String REQUEST_HEADERS = "REQUEST_HEADERS";

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        final MultivaluedMap<String, String> headers = requestContext.getHeaders();
        ThreadContext.put(REQUEST_HEADERS, headers);
    }

    public static Optional<String> getHeaderFromThreadContext(String headerName) {
        @SuppressWarnings("unchecked")
        final MultivaluedMap<String, String> requestHeaders =
                (MultivaluedMap<String, String>) ThreadContext.get(REQUEST_HEADERS);
        if (requestHeaders != null) {
            final String header = requestHeaders.getFirst(headerName);
            if (!Strings.isNullOrEmpty(header)) {
                return Optional.of(header);
            }
        }
        return Optional.empty();
    }

    @Override
    public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) throws IOException {
        // Ensure removal of request headers to avoid leaking them for the next request
        ThreadContext.remove(REQUEST_HEADERS);
    }
}
