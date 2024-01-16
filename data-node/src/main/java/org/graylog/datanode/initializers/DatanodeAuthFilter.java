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
package org.graylog.datanode.initializers;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Collections;
import java.util.Optional;

/**
 * This is an authorization filter that  first try to verify presence and validity of a bearer token. If there is no
 * bearer token available, it will fallback to basic auth (or whatever filter is configured as fallback).
 * Allowing both auth methods allows easy access directly from CLI or browser and machine-machine communication from the graylog server.
 */
public class DatanodeAuthFilter implements ContainerRequestFilter {

    private static final Logger LOG = LoggerFactory.getLogger(DatanodeAuthFilter.class);
    private static final String AUTHENTICATION_SCHEME = "Bearer";
    private final ContainerRequestFilter fallbackFilter;
    private final AuthTokenValidator tokenVerifier;


    public DatanodeAuthFilter(ContainerRequestFilter fallbackFilter, AuthTokenValidator tokenVerifier) {
        this.fallbackFilter = fallbackFilter;
        this.tokenVerifier = tokenVerifier;
    }

    private Optional<String> getBearerHeader(ContainerRequestContext requestContext) {
        final MultivaluedMap<String, String> headers = requestContext.getHeaders();
        return headers.getOrDefault(HttpHeaders.AUTHORIZATION, Collections.emptyList())
                .stream()
                .filter(a -> a.startsWith(AUTHENTICATION_SCHEME))
                .findFirst();
    }

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        final Optional<String> header = getBearerHeader(requestContext);
        if (header.isEmpty()) {
            // no JWT token, we'll fallback to basic auth
            fallbackFilter.filter(requestContext);
        } else {
            final String token = header.map(h -> h.replaceFirst(AUTHENTICATION_SCHEME + " ", "")).get();
            try {
                tokenVerifier.verifyToken(token);
            } catch (TokenVerificationException e) {
                LOG.error("Failed to verify auth token", e);
                abortRequest(requestContext);
            }
        }
    }


    private void abortRequest(ContainerRequestContext requestContext) {
        requestContext.abortWith(Response.status(Response.Status.UNAUTHORIZED)
                .entity("Failed to parse auth header")
                .type(MediaType.TEXT_PLAIN_TYPE)
                .build());
    }
}
