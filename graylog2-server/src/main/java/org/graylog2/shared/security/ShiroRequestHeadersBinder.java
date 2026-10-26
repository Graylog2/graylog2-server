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

import com.google.common.base.Strings;
import jakarta.annotation.Priority;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.MultivaluedMap;
import org.apache.shiro.util.ThreadContext;

import java.io.IOException;
import java.util.Optional;
import java.util.function.Supplier;

import static org.graylog2.shared.rest.RequestIdFilter.X_REQUEST_ID;

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

    /**
     * Runs the supplier with only the given X-Request-Id header bound to the thread context and restores the
     * previous headers afterwards. {@link org.graylog2.security.realm.MongoDbAuthorizationRealm} scopes its
     * authorization cache by this header, so code running outside of REST requests needs to provide one.
     */
    public static <T> T withRequestId(String requestId, Supplier<T> supplier) {
        final Object previousHeaders = ThreadContext.get(REQUEST_HEADERS);
        final MultivaluedMap<String, String> headers = new MultivaluedHashMap<>();
        headers.putSingle(X_REQUEST_ID, requestId);
        ThreadContext.put(REQUEST_HEADERS, headers);
        try {
            return supplier.get();
        } finally {
            ThreadContext.put(REQUEST_HEADERS, previousHeaders);
        }
    }

    @Override
    public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) throws IOException {
        // Ensure removal of request headers to avoid leaking them for the next request
        ThreadContext.remove(REQUEST_HEADERS);
    }
}
