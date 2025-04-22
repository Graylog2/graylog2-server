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

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;

class IndexerIndexerJwtAuthTokenProviderTest {

    @Test
    void testTokenParsing() {

        final RandomStringUtils randomStringUtils = RandomStringUtils.secure();

        final JwtSecret jwtSecret = new JwtSecret(randomStringUtils.nextAlphabetic(96));

        final Instant now = Instant.now();

        final IndexerJwtAuthTokenProvider tokenProvider = new IndexerJwtAuthTokenProvider(
                jwtSecret,
                Duration.seconds(90),
                Duration.seconds(60),
                true,
                Clock.fixed(now, ZoneOffset.UTC)
        );

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

                    // There is a small time difference between now and actually creating the token, in the .get() call
                    // Additionally, there may be small difference, as JWT is using only seconds but now is more precise.
                    // Let's give or take 5s, this won't change the validity of the test.
                    final long delta = Duration.seconds(5).toMilliseconds();
                    Assertions.assertThat(claims.getIssuedAt()).isCloseTo(now, delta);
                    Assertions.assertThat(claims.getExpiration()).isCloseTo(now.plus(90, ChronoUnit.SECONDS), delta);
                });
    }
}
