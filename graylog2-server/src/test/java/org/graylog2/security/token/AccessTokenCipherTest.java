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
package org.graylog2.security.token;

import org.graylog2.Configuration;
import org.graylog2.security.AccessTokenCipher;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

public class AccessTokenCipherTest {
    @Rule
    public final MockitoRule mockitoRule = MockitoJUnit.rule();

    private AccessTokenCipher cipher;

    @Mock
    Configuration configuration;

    @Before
    public void setUp() throws Exception {
        when(configuration.getPasswordSecret()).thenReturn("0123456789ABCDEF");

        cipher = new AccessTokenCipher(configuration);
    }

    @Test
    public void roundTrip() {
        assertThat(cipher.decrypt(cipher.encrypt("cleartext"))).isEqualTo("cleartext");
    }

    @Test
    public void encrypt() {
        final String ciphertext = cipher.encrypt("cleartext");
        assertThat(ciphertext).isEqualTo("e5cb18c577b661b60ef0810675962f3fb9b0c2ad63b9e76afb");
    }

    @Test
    public void decrypt() {
        final String cleartext = cipher.decrypt("e5cb18c577b661b60ef0810675962f3fb9b0c2ad63b9e76afb");
        assertThat(cleartext).isEqualTo("cleartext");
    }

    @Test
    public void encryptionIsDeterministic() {
        final String c1 = cipher.encrypt("cleartext");
        final String c2 = cipher.encrypt("cleartext");
        final String c3 = cipher.encrypt("cleartext");
        assertThat(c1).isEqualTo(c2)
                .isEqualTo(c3);
    }
}
