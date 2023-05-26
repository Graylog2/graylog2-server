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
package org.graylog2.shared.rest.resources.csp;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.core.MultivaluedMap;

import static org.graylog2.shared.rest.resources.csp.CSPDynamicFeature.CSP_NONCE_PROPERTY;

public class CSPResponseFilter implements ContainerResponseFilter {
    public final static String CSP_HEADER = "Content-Security-Policy";
    private final static String noncePattern = "\\{nonce}";
    private String value;

    public CSPResponseFilter(String value) {
        this.value = value;
    }

    @Override
    public void filter(final ContainerRequestContext requestContext,
                       ContainerResponseContext responseContext) {
        final MultivaluedMap<String, Object> headers = responseContext.getHeaders();
        if (!headers.containsKey(CSP_HEADER)) {
            final var cspNonce = (String) requestContext.getProperty(CSP_NONCE_PROPERTY);
            final var valueWithNonce = value.replaceAll(noncePattern, cspNonce);
            headers.add(CSP_HEADER, valueWithNonce);
        }
    }
}
