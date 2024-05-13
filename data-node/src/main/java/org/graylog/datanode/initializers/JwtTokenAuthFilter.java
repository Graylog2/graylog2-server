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

import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.Keys;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.SecretKey;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Optional;

@Singleton
public class JwtTokenAuthFilter implements ContainerRequestFilter {

    private static final Logger LOG = LoggerFactory.getLogger(JwtTokenAuthFilter.class);

    private static final String AUTHENTICATION_SCHEME = "Bearer";
    public static final String REQUIRED_SUBJECT = "admin";
    public static final String REQUIRED_ISSUER = "graylog";
    private final String signingKey;

    public JwtTokenAuthFilter(@Named("password_secret") final String signingKey) {
        this.signingKey = signingKey;
    }

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        final Optional<String> header = getBearerHeader(requestContext);
        if (header.isEmpty()) {
            // no JWT token, we'll fail immediately
            abortRequest(requestContext);
        } else {
            final String token = header.map(h -> h.replaceFirst(AUTHENTICATION_SCHEME + " ", "")).get();
            try {
                verifyToken(token);
            } catch (TokenVerificationException e) {
                LOG.error("Failed to verify auth token", e);
                abortRequest(requestContext);
            }
        }
    }

    private Optional<String> getBearerHeader(ContainerRequestContext requestContext) {
        final MultivaluedMap<String, String> headers = requestContext.getHeaders();
        return headers.getOrDefault(HttpHeaders.AUTHORIZATION, Collections.emptyList())
                .stream()
                .filter(a -> a.startsWith(AUTHENTICATION_SCHEME))
                .findFirst();
    }

    void verifyToken(String token) throws TokenVerificationException {
        final SecretKey key = Keys.hmacShaKeyFor(this.signingKey.getBytes(StandardCharsets.UTF_8));
        final JwtParser parser = Jwts.parser()
                .verifyWith(key)
                .requireSubject(REQUIRED_SUBJECT)
                .requireIssuer(REQUIRED_ISSUER)
                .build();
        try {
            parser.parse(token);
        } catch (UnsupportedJwtException e) {
            throw new TokenVerificationException("Token format/configuration is not supported", e);
        } catch (Throwable e) {
            throw new TokenVerificationException(e);
        }
    }


    private void abortRequest(ContainerRequestContext requestContext) {
        requestContext.abortWith(Response.status(Response.Status.UNAUTHORIZED)
                .entity("Failed to parse auth header")
                .type(MediaType.TEXT_PLAIN_TYPE)
                .build());
    }
}
