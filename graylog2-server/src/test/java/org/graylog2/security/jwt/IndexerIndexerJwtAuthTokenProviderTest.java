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
package org.graylog2.security.jwt;

import com.github.joschi.jadconfig.util.Duration;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import org.apache.commons.lang3.RandomStringUtils;
import org.assertj.core.api.Assertions;
import org.graylog2.security.JwtSecret;
import org.junit.jupiter.api.Test;

class IndexerIndexerJwtAuthTokenProviderTest {
    @Test
    void testTokenParsing() {

        final RandomStringUtils randomStringUtils = RandomStringUtils.secure();

        final JwtSecret jwtSecret = new JwtSecret(randomStringUtils.nextAlphabetic(96));

        final IndexerJwtAuthTokenProvider tokenProvider = new IndexerJwtAuthTokenProvider(
                jwtSecret,
                Duration.seconds(60),
                Duration.seconds(60),
                true);

        final IndexerJwtAuthToken bearerToken = tokenProvider.get();

        Assertions.assertThat(bearerToken.rawTokenValue())
                .isPresent()
                .hasValueSatisfying(tokenValue -> {
                    final Claims claims = Jwts.parser()
                            .verifyWith(jwtSecret.getSigningKey())
                            .build()
                            .parseSignedClaims(tokenValue)
                            .getPayload();

                    Assertions.assertThat(claims.getSubject()).isEqualTo("admin");
                    Assertions.assertThat(claims.getIssuer()).isEqualTo("graylog");
                });
    }
}
