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

import com.google.common.base.CharMatcher;
import com.google.common.base.Strings;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.NewCookie;
import org.graylog2.configuration.HttpConfiguration;
import org.graylog2.rest.RestTools;
import org.graylog2.rest.models.system.sessions.responses.SessionResponse;

import java.net.URI;
import java.util.Date;
import java.util.Optional;

import static com.google.common.base.CharMatcher.ascii;
import static com.google.common.base.CharMatcher.isNot;
import static com.google.common.base.CharMatcher.javaIsoControl;

@Singleton
public class CookieFactory {
    private static final String HEADER_ORIGIN = "Origin";
    private static final String HEADER_X_FORWARDED_PROTO = "X-Forwarded-Proto";
    private static final CharMatcher PATH_MATCHER = ascii().and(isNot(';')).and(javaIsoControl().negate());
    private final URI httpExternalUri;

    @Inject
    public CookieFactory(HttpConfiguration httpConfiguration) {
        httpExternalUri = httpConfiguration.getHttpExternalUri();
    }

    NewCookie createAuthenticationCookie(SessionResponse token, ContainerRequestContext requestContext) {
        return makeCookie(token.getAuthenticationToken(), token.validUntil(), requestContext);
    }

    NewCookie deleteAuthenticationCookie(ContainerRequestContext requestContext) {
        return makeCookie("", new Date(), requestContext);
    }

    private NewCookie makeCookie(String value, Date validUntil, ContainerRequestContext requestContext) {
        final Date now = new Date();
        final int maxAge = Long.valueOf((validUntil.getTime() - now.getTime()) / 1000).intValue();

        final String basePath = cookiePathFromRequest(requestContext);

        final boolean isSecure = schemeFromRequest(requestContext)
                .map(scheme -> scheme.equalsIgnoreCase("https"))
                .orElse(false);

        return new NewCookie.Builder("authentication")
                .value(value)
                .comment("Authentication Cookie")
                .path(basePath)
                .maxAge(maxAge)
                .expiry(validUntil)
                .secure(true)
                .httpOnly(true)
                .sameSite(NewCookie.SameSite.NONE)
                .build();
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

    private String cookiePathFromRequest(ContainerRequestContext requestContext) {
        return sanitizePath(
                RestTools.buildExternalUri(requestContext.getHeaders(), httpExternalUri).getPath()
        );
    }

    private Optional<URI> safeCreateUri(String uri) {
        try {
            return Optional.of(URI.create(uri));
        } catch (IllegalArgumentException ignored) {
            return Optional.empty();
        }
    }

    /**
     * Remove characters that are unsafe to use for the path in the cookie.
     * According to RFC 6265 the allowed characters are:
     * <p>
     * {@code
     * <any CHAR except CTLs or ";">
     * }
     */
    private String sanitizePath(String path) {
        return PATH_MATCHER.retainFrom(path);
    }

}
