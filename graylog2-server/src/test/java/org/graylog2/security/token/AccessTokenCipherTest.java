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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.WARN)
public class AccessTokenCipherTest {

    private AccessTokenCipher cipher;

    @Mock
    Configuration configuration;

    @BeforeEach
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
