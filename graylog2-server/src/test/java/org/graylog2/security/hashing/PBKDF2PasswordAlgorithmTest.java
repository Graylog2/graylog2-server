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
package org.graylog2.security.hashing;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PBKDF2PasswordAlgorithmTest {

    private static PBKDF2PasswordAlgorithm hasher;

    @BeforeAll
    public static void setUp() throws Exception {
        hasher = new PBKDF2PasswordAlgorithm(PBKDF2PasswordAlgorithm.DEFAULT_ITERATIONS);
    }

    @Test
    void supports() {
        assertThat(hasher.supports("some_string")).isFalse();
        assertThat(hasher.supports("{PBKDF2}")).isFalse();
        assertThat(hasher.supports("{PBKDF2}somestring%foo")).isFalse();
        assertThat(hasher.supports("{PBKDF2}100%salt%hash")).isTrue();
    }

    @Test
    void hashMatches() {
        final String test = hasher.hash("test");
        assertThat(hasher.matches(test, "test")).isTrue();
    }

}
