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
package org.graylog.datanode.rest;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.UnsupportedJwtException;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import org.graylog.datanode.initializers.JwtTokenAuthFilter;
import org.graylog.datanode.initializers.TokenVerificationException;
import org.graylog2.security.JwtSecret;

import javax.crypto.SecretKey;
import java.util.List;

@Path("/jwt")
@Produces(MediaType.APPLICATION_JSON)
public class JwtDebugController {

    private final JwtSecret jwtSecret;

    @Inject
    public JwtDebugController(JwtSecret jwtSecret) {
        this.jwtSecret = jwtSecret;
    }

    @GET
    public JwtDebugInfo status(@Context HttpHeaders headers) {
        final List<String> authHeaders = headers.getRequestHeader(HttpHeaders.AUTHORIZATION);
        final String token = authHeaders.stream().findFirst().map(h -> h.replaceFirst(JwtTokenAuthFilter.AUTHENTICATION_SCHEME + " ", "")).get();
        try {
            return verifyToken(token);
        } catch (TokenVerificationException e) {
            return new JwtDebugInfo(e.getMessage());
        }
    }

    private JwtDebugInfo verifyToken(String token) throws TokenVerificationException {
        final SecretKey key = this.jwtSecret.getSigningKey();
        final JwtParser parser = Jwts.parser()
                .verifyWith(key)
                .requireSubject(JwtTokenAuthFilter.REQUIRED_SUBJECT)
                .requireIssuer(JwtTokenAuthFilter.REQUIRED_ISSUER)
                .build();
        try {
            final Jws<Claims> claims = parser.parseSignedClaims(token);
            final Claims payload = claims.getPayload();
            return new JwtDebugInfo(payload.getIssuedAt(), payload.getExpiration());
        } catch (UnsupportedJwtException e) {
            throw new TokenVerificationException("Token format/configuration is not supported", e);
        } catch (Throwable e) {
            throw new TokenVerificationException(e);
        }
    }
}
