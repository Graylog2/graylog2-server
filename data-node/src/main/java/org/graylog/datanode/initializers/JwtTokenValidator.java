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
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Optional;

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
        SignatureAlgorithm signatureAlgorithm = SignatureAlgorithm.HS256;
        Key signingKey = new SecretKeySpec(this.signingKey.getBytes(StandardCharsets.UTF_8), signatureAlgorithm.getJcaName());
        final JwtParser parser = Jwts.parserBuilder()
                .setSigningKey(signingKey)
                .requireSubject(REQUIRED_SUBJECT)
                .requireIssuer(REQUIRED_ISSUER)
                .build();
        try {
            final Jwt parsed = parser.parse(token);
            verifySignature(parsed, signatureAlgorithm);
        } catch (Exception e) {
            throw new TokenVerificationException(e);
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
}
