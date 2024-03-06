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

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

@Singleton
public class JwtTokenValidator implements AuthTokenValidator {

    public static final String REQUIRED_SUBJECT = "admin";
    public static final String REQUIRED_ISSUER = "graylog";
    private final String signingKey;

    public JwtTokenValidator(@Named("password_secret") final String signingKey) {
        this.signingKey = signingKey;
    }

    @Override
    public void verifyToken(String token) throws TokenVerificationException {
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
}
