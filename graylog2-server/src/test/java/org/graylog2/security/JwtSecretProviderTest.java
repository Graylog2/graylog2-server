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
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

class JwtSecretProviderTest {

    @Test
    void testShortPassword() {
        final String shortPassword = RandomStringUtils.random(16);
        final JwtSecretProvider provider = new JwtSecretProvider(shortPassword);
        final String signingKey = new String(provider.get().getBytes(), StandardCharsets.UTF_8);
        Assertions.assertThat(signingKey.length()).isEqualTo(64);
    }


    @Test
    void testLongPassword() {
        final String longPassword = RandomStringUtils.random(96);
        final JwtSecretProvider provider = new JwtSecretProvider(longPassword);
        final String signingKey = new String(provider.get().getBytes(), StandardCharsets.UTF_8);
        Assertions.assertThat(signingKey).isEqualTo(longPassword);
    }
}
