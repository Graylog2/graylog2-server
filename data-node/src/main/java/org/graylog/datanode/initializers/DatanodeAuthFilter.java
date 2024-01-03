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

import io.jsonwebtoken.Jwt;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.spec.SecretKeySpec;
import javax.inject.Named;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Collections;
import java.util.Optional;

/**
 * This is an authorization filter that  first try to verify presence and validity of a JWT token. If there is no
 * bearer token available, it will fallback to basic auth (or whatever filter is configured as fallback).
 * Allowing both auth methods allows easy access directly from CLI or browser and machine-machine communication from the graylog server.
 */
public class DatanodeAuthFilter implements ContainerRequestFilter {

    private static final Logger LOG = LoggerFactory.getLogger(DatanodeAuthFilter.class);
    private static final String AUTHORIZATION_PROPERTY = "Authorization";
    private static final String AUTHENTICATION_SCHEME = "Bearer";
    private final ContainerRequestFilter fallbackFilter;
    private final String signingKey;


    public DatanodeAuthFilter(ContainerRequestFilter fallbackFilter, @Named("password_secret") String signingKey) {
        this.fallbackFilter = fallbackFilter;
        this.signingKey = signingKey;
    }

    private Optional<String> getBearerHeader(ContainerRequestContext requestContext) {
        final MultivaluedMap<String, String> headers = requestContext.getHeaders();
        return headers.getOrDefault(AUTHORIZATION_PROPERTY, Collections.emptyList())
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
            verifyJwtHeader(requestContext, header.get());
        }
    }

    private void verifyJwtHeader(ContainerRequestContext requestContext, String authHeader) {
        final String jwtToken = authHeader.replaceFirst(AUTHENTICATION_SCHEME + " ", "");
        SignatureAlgorithm signatureAlgorithm = SignatureAlgorithm.HS256;
        Key signingKey = new SecretKeySpec(this.signingKey.getBytes(StandardCharsets.UTF_8), signatureAlgorithm.getJcaName());
        final JwtParser parser = Jwts.parserBuilder()
                .setSigningKey(signingKey)
                .requireSubject("admin")
                .requireIssuer("graylog")
                .build();
        try {
            final Jwt parsed = parser.parse(jwtToken);
            verifySignature(parsed, signatureAlgorithm);
        } catch (Exception e) {
            abortRequest(requestContext, e);
        }
    }

    private void verifySignature(Jwt token, SignatureAlgorithm expectedAlgorithm) {
        final SignatureAlgorithm usedAlgorithm = Optional.of(token.getHeader())
                .map(h -> h.get("alg"))
                .map(Object::toString)
                .map(SignatureAlgorithm::forName)
                .orElseThrow(() -> new IllegalArgumentException("Token doesn't provide valid signature algorithm"));

        if (expectedAlgorithm != usedAlgorithm) {
            throw new IllegalArgumentException("Token is using unsupported signature algorithm :" + usedAlgorithm);
        }
    }

    private void abortRequest(ContainerRequestContext requestContext, Exception e) {
        LOG.error("Failed to parse JWT auth header", e);
        requestContext.abortWith(Response.status(Response.Status.UNAUTHORIZED)
                .entity("Failed to parse JWT auth header")
                .type(MediaType.TEXT_PLAIN_TYPE)
                .build());
    }
}
