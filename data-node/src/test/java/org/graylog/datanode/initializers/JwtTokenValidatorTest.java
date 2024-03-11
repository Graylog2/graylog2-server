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
import org.assertj.core.api.Assertions;
import org.graylog2.security.IndexerJwtAuthTokenProvider;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

class JwtTokenValidatorTest {

    @Test
    void verifyValidToken() throws TokenVerificationException {
        final String key = "gTVfiF6A0pB70A3UP1EahpoR6LId9DdNadIkYNygK5Z8lpeJIpw9vN0jZ6fdsfeuV9KIg9gVLkCHIPj6FHW5Q9AvpOoGZO3h";
        final JwtTokenValidator validator = new JwtTokenValidator(key);
        validator.verifyToken(generateToken(key));
    }

    @Test
    void verifyInvalidToken() {
        final String generationKey = "gTVfiF6A0pB70A3UP1EahpoR6LId9DdNadIkYNygK5Z8lpeJIpw9vN0jZ6fdsfeuV9KIg9gVLkCHIPj6FHW5Q9AvpOoGZO3h";
        final String verificationKey = "n51wcO3jn8w3JNyGgKc7k1fTCr1FWvGg7ODfQOyBT2fizBrCVsRJg2GsbYGLNejfi3QsKaqJgo3zAWMuAZhJznuizHZpv92S";
        final JwtTokenValidator validator = new JwtTokenValidator(verificationKey);
        Assertions.assertThatThrownBy(() -> validator.verifyToken(generateToken(generationKey)))
                .isInstanceOf(TokenVerificationException.class)
                .hasMessageContaining("JWT signature does not match locally computed signature");
    }

    @Test
    void testNoneAlgorithm() {
        final String key = "gTVfiF6A0pB70A3UP1EahpoR6LId9DdNadIkYNygK5Z8lpeJIpw9vN0jZ6fdsfeuV9KIg9gVLkCHIPj6FHW5Q9AvpOoGZO3h";
        final JwtTokenValidator validator = new JwtTokenValidator(key);
        Assertions.assertThatThrownBy(() -> validator.verifyToken(removeSignature(generateToken(key))))
                .isInstanceOf(TokenVerificationException.class)
                .hasMessageContaining("Token format/configuration is not supported");
    }

    private String removeSignature(String token) {
        final String header = Base64.getEncoder()
                .encodeToString("{\"alg\": \"none\"}"
                        .getBytes(StandardCharsets.UTF_8));

        return header + token.substring(token.indexOf('.'), token.lastIndexOf('.') + 1);
    }

    @NotNull
    private static String generateToken(String signingKey) {
        return IndexerJwtAuthTokenProvider.createToken(signingKey.getBytes(StandardCharsets.UTF_8), Duration.seconds(180));
    }
}
