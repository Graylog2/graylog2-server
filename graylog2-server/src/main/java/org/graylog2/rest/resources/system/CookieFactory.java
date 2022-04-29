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
package org.graylog2.rest.resources.system;

import com.google.common.base.Strings;
import org.graylog2.configuration.HttpConfiguration;
import org.graylog2.rest.models.system.sessions.responses.SessionResponse;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.NewCookie;
import java.net.URI;
import java.util.Date;
import java.util.Optional;

public class CookieFactory {
    private static final String HEADER_ORIGIN = "Origin";
    private static final String HEADER_X_FORWARDED_PROTO = "X-Forwarded-Proto";

    NewCookie createAuthenticationCookie(SessionResponse token, ContainerRequestContext requestContext) {
        return makeCookie(token.getAuthenticationToken(), token.validUntil(), requestContext);
    }

    NewCookie deleteAuthenticationCookie(ContainerRequestContext requestContext) {
        return makeCookie("", new Date(), requestContext);
    }

    private NewCookie makeCookie(String value, Date validUntil, ContainerRequestContext requestContext) {
        final Date now = new Date();
        final int maxAge = Long.valueOf((validUntil.getTime() - now.getTime()) / 1000).intValue();

        final URI baseUri = baseUriFromRequest(requestContext);
        final String basePath = Optional.ofNullable(Strings.emptyToNull(baseUri.getPath())).orElse("/");

        final boolean isSecure = schemeFromRequest(requestContext)
                .map(scheme -> scheme.equalsIgnoreCase("https"))
                .orElse(false);

        return new NewCookie("authentication",
                value,
                basePath,
                null,
                Cookie.DEFAULT_VERSION,
                "Authentication Cookie",
                maxAge,
                validUntil,
                isSecure,
                true);
    }

    private Optional<String> schemeFromRequest(ContainerRequestContext requestContext) {
        final Optional<URI> graylogUrlFromHeader = uriFromHeader(requestContext, HttpConfiguration.OVERRIDE_HEADER);

        if (graylogUrlFromHeader.isPresent()) {
            return graylogUrlFromHeader.map(URI::getScheme);
        }

        final Optional<String> xForwardedProtoHeader = Optional.ofNullable(requestContext.getHeaderString(HEADER_X_FORWARDED_PROTO))
                .filter(header -> !Strings.isNullOrEmpty(header));

        if (xForwardedProtoHeader.isPresent()) {
            return xForwardedProtoHeader;
        }

        final Optional<URI> origin = uriFromHeader(requestContext, HEADER_ORIGIN);

        return origin.map(URI::getScheme);
    }

    private Optional<URI> uriFromHeader(ContainerRequestContext requestContext, String headerName) {
        return Optional.ofNullable(requestContext.getHeaderString(headerName))
                .filter(header -> !Strings.isNullOrEmpty(header))
                .flatMap(this::safeCreateUri);
    }

    private URI baseUriFromRequest(ContainerRequestContext requestContext) {
        final Optional<URI> graylogUrlFromHeader = uriFromHeader(requestContext, HttpConfiguration.OVERRIDE_HEADER);
        if (graylogUrlFromHeader.isPresent()) {
            return graylogUrlFromHeader.get();
        }

        final Optional<URI> origin = uriFromHeader(requestContext, HEADER_ORIGIN);

        return origin.orElseGet(() -> requestContext.getUriInfo().getBaseUri());
    }

    private Optional<URI> safeCreateUri(String uri) {
        try {
            return Optional.of(URI.create(uri));
        } catch (IllegalArgumentException ignored) {
            return Optional.empty();
        }
    }

}
