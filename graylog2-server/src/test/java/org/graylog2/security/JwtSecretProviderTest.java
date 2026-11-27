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
package org.graylog2.security;

import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class JwtSecretProviderTest {

    @Test
    void testShortPassword() {
        final String shortPassword = RandomStringUtils.secure().nextAlphanumeric(16);
        final JwtSecret secret = new JwtSecretProvider(shortPassword).get();
        assertThat(secret.getBytes()).hasSize(64);
        assertThat(secret.getSigningKey().getAlgorithm()).isEqualTo("HmacSHA512");
    }

    @Test
    void testLongPassword() {
        final String longPassword = RandomStringUtils.secure().nextAlphanumeric(96);
        final JwtSecret secret = new JwtSecretProvider(longPassword).get();
        assertThat(secret.getBytes()).hasSize(64);
        assertThat(secret.getBytes()).isNotEqualTo(longPassword.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void testDerivationIsDeterministic() {
        final String password = RandomStringUtils.secure().nextAlphanumeric(32);
        assertThat(new JwtSecretProvider(password).get().getBytes())
                .isEqualTo(new JwtSecretProvider(password).get().getBytes());
        assertThat(new JwtSecretProvider(password).get().getBytes())
                .isNotEqualTo(new JwtSecretProvider(password + "x").get().getBytes());
    }

    @Test
    void testPasswordSecretNotLeaked() {
        final String password = "thisisverysecretpasswordthisisverysecretpasswordthisisverysecret";
        final JwtSecret secret = new JwtSecretProvider(password).get();
        assertThat(new String(secret.getBytes(), StandardCharsets.UTF_8)).doesNotContain(password);
        assertThat(secret.getBase64Encoded()).isNotEqualTo(new JwtSecret(password).getBase64Encoded());
    }
}
