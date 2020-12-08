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

import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class BCryptPasswordAlgorithmTest {
    private BCryptPasswordAlgorithm bCryptPasswordAlgorithm;

    @Before
    public void setUp() throws Exception {
        this.bCryptPasswordAlgorithm = new BCryptPasswordAlgorithm(10);
    }

    @Test
    public void testSupports() throws Exception {
        assertThat(bCryptPasswordAlgorithm.supports("foobar")).isFalse();
        assertThat(bCryptPasswordAlgorithm.supports("{bcrypt}foobar")).isFalse();
        assertThat(bCryptPasswordAlgorithm.supports("{bcrypt}foobar{salt}pepper")).isTrue();
        assertThat(bCryptPasswordAlgorithm.supports("{foobar}foobar")).isFalse();
    }

    @Test
    public void testHash() throws Exception {
        final String clearTextPassword = "foobar";
        final String hashedPassword = bCryptPasswordAlgorithm.hash(clearTextPassword);

        assertThat(hashedPassword)
                .isNotEmpty()
                .startsWith("{bcrypt}")
                .contains("{salt}");

        assertThat(bCryptPasswordAlgorithm.matches(hashedPassword, clearTextPassword)).isTrue();
    }

    @Test
    public void testMatches() throws Exception {
        assertThat(bCryptPasswordAlgorithm.matches("{bcrypt}$2a$12$8lRgZZTqRWO2.Mk37Gl7re7uD0QoDkdSF/UtFfVx0BqqgI23/jtkO{salt}$2a$12$8lRgZZTqRWO2.Mk37Gl7re", "foobar")).isTrue();
    }
}