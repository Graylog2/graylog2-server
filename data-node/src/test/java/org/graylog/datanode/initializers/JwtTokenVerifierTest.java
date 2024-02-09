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

import com.github.joschi.jadconfig.util.Duration;
import io.jsonwebtoken.Jwts;
import org.apache.commons.lang.RandomStringUtils;
import org.assertj.core.api.Assertions;
import org.graylog2.security.IndexerJwtAuthTokenProvider;
import org.junit.jupiter.api.Test;

import java.util.Date;
import java.util.Map;

class JwtTokenVerifierTest {
    @Test
    void testValidToken() {
        final String signingKey = RandomStringUtils.random(64);
        final AuthTokenValidator tokenVerifier = new JwtTokenValidator(signingKey);
        final IndexerJwtAuthTokenProvider tokenProvider = new IndexerJwtAuthTokenProvider(signingKey, Duration.seconds(10), Duration.seconds(1));

        try {
            tokenVerifier.verifyToken(tokenProvider.get().replace("Bearer ", ""));
        } catch (TokenVerificationException e) {
            Assertions.fail("Verification of the token shouldn't fail");
        }
    }

    @Test
    void testNoneAlgorithmToken() {
        final Date now = new Date();
        final String insecureToken = Jwts.builder()
                .addClaims(Map.of("os_roles", "admin"))
                .setIssuedAt(now)
                .setSubject("admin")
                .setIssuer("graylog")
                .setNotBefore(now)
                .setExpiration(new Date(now.getTime() + Duration.seconds(10).toMilliseconds()))
                .compact();

        final String signingKey = RandomStringUtils.random(64);
        final AuthTokenValidator tokenVerifier = new JwtTokenValidator(signingKey);

        Assertions.assertThatThrownBy(() -> tokenVerifier.verifyToken(insecureToken))
                .isInstanceOf(TokenVerificationException.class)
                .hasMessageContaining("Token is using unsupported signature algorithm :NONE");
    }
}
