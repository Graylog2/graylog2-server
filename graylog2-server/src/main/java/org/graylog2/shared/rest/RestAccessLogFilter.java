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

import org.glassfish.grizzly.http.server.Response;
import org.graylog2.rest.RestTools;
import org.graylog2.utilities.IpSubnet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import java.io.IOException;
import java.util.Date;
import java.util.Set;

import static java.util.Objects.requireNonNull;

public class RestAccessLogFilter implements ContainerResponseFilter {
    private static final Logger LOG = LoggerFactory.getLogger("org.graylog2.rest.accesslog");

    private final Response response;
    private final Set<IpSubnet> trustedProxies;

    @Inject
    public RestAccessLogFilter(@Context Response response, @Named("trusted_proxies") Set<IpSubnet> trustedProxies) {
        this.response = requireNonNull(response);
        this.trustedProxies = requireNonNull(trustedProxies);
    }

    @Override
    public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) throws IOException {
        if (LOG.isDebugEnabled()) {
            try {
                final String rawQuery = requestContext.getUriInfo().getRequestUri().getRawQuery();
                final Date requestDate = requestContext.getDate();
                final String userId = RestTools.getUserIdFromRequest(requestContext);
                final String remoteAddress = RestTools.getRemoteAddrFromRequest(response.getRequest(), trustedProxies);
                final String userAgent = requestContext.getHeaderString(HttpHeaders.USER_AGENT);

                LOG.debug("{} {} [{}] \"{} {}{}\" {} {} {}",
                        remoteAddress,
                        userId == null ? "-" : userId,
                        (requestDate == null ? "-" : requestDate),
                        requestContext.getMethod(),
                        requestContext.getUriInfo().getPath(),
                        (rawQuery == null ? "" : "?" + rawQuery),
                        (userAgent == null ? "-" : userAgent),
                        responseContext.getStatus(),
                        responseContext.getLength());
            } catch (Exception e) {
                LOG.error("Error while processing REST API access log", e);
            }
        }
    }
}
