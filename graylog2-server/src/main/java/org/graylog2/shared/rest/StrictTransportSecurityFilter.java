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

import jakarta.inject.Inject;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import org.graylog2.configuration.HttpConfiguration;

import java.util.Optional;

import static org.graylog2.shared.utilities.StringUtils.f;

public class StrictTransportSecurityFilter implements ContainerResponseFilter {
    public static final String STRICT_TRANSPORT_SECURITY = "Strict-Transport-Security";

    private final Optional<String> headerValue;

    @Inject
    public StrictTransportSecurityFilter(HttpConfiguration httpConfiguration) {
        this.headerValue = httpConfiguration.isHttpHstsEnabled() && httpConfiguration.isHttpEnableTls()
                ? Optional.of(buildHeaderValue(httpConfiguration))
                : Optional.empty();
    }

    private static String buildHeaderValue(HttpConfiguration httpConfiguration) {
        final String maxAge = f("max-age=%d", httpConfiguration.getHttpHstsMaxAge().toSeconds());
        return httpConfiguration.isHttpHstsIncludeSubdomains() ? maxAge + "; includeSubDomains" : maxAge;
    }

    @Override
    public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) {
        if (requestContext.getSecurityContext() == null || !requestContext.getSecurityContext().isSecure()) {
            return;
        }
        headerValue.ifPresent(value -> responseContext.getHeaders().putSingle(STRICT_TRANSPORT_SECURITY, value));
    }
}
