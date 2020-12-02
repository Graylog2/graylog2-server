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

public class SHA1HashPasswordAlgorithmTest {
    private SHA1HashPasswordAlgorithm SHA1HashPasswordAlgorithm;

    @Before
    public void setUp() throws Exception {
        this.SHA1HashPasswordAlgorithm = new SHA1HashPasswordAlgorithm("passwordSecret");
    }

    @Test
    public void testSupports() throws Exception {
        assertThat(SHA1HashPasswordAlgorithm.supports("deadbeefaffedeadbeefdeadbeefaffedeadbeef")).isTrue();
        assertThat(SHA1HashPasswordAlgorithm.supports("{bcrypt}foobar")).isFalse();
        assertThat(SHA1HashPasswordAlgorithm.supports("{foobar}foobar")).isFalse();
    }

    @Test
    public void testHash() throws Exception {
        assertThat(SHA1HashPasswordAlgorithm.hash("foobar")).isEqualTo("baae906e6bbb37ca5033600fcb4824c98b0430fb");
    }

    @Test
    public void testMatches() throws Exception {
        assertThat(SHA1HashPasswordAlgorithm.matches("baae906e6bbb37ca5033600fcb4824c98b0430fb", "foobar")).isTrue();
    }
}