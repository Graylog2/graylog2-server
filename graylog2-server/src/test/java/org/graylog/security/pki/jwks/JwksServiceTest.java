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
package org.graylog.security.pki.jwks;

import org.graylog.collectors.CollectorsConfig;
import org.graylog.collectors.CollectorsConfigService;
import org.graylog.collectors.TokenSigningKey;
import org.graylog.security.pki.Algorithm;
import org.graylog.security.pki.KeyUtils;
import org.graylog.security.pki.PemUtils;
import org.graylog.testing.TestClocks;
import org.graylog2.security.encryption.EncryptedValueService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link JwksService}.
 */
@ExtendWith(MockitoExtension.class)
class JwksServiceTest {

    @Mock
    private CollectorsConfigService collectorsConfigService;

    private JwksService jwksService;
    private final EncryptedValueService encryptedValueService = new EncryptedValueService("1234567890abcdef");

    @BeforeEach
    void setUp() throws Exception {
        jwksService = new JwksService(collectorsConfigService);

        final var collectorsConfig = mock(CollectorsConfig.class);
        when(collectorsConfigService.get()).thenReturn(Optional.of(collectorsConfig));

        final var keyPair = KeyUtils.generateKeyPair(Algorithm.ED25519);
        lenient().when(collectorsConfig.tokenSigningKey()).thenReturn(new TokenSigningKey(
                encryptedValueService.encrypt(PemUtils.toPem(keyPair.getPrivate())),
                PemUtils.toPem(keyPair.getPublic()),
                KeyUtils.sha256Fingerprint(keyPair),
                Instant.now(TestClocks.fixedEpoch())
        ));
    }

    // getJwks tests

    @Test
    void getJwksReturnsEmptyResponseWhenNoSigningKey() {
        when(collectorsConfigService.get()).thenReturn(Optional.empty());

        final JwksResponse response = jwksService.getJwks();

        assertThat(response.keys()).isEmpty();
    }

    @Test
    void getJwksReturnsValidSigningKeys() throws Exception {
        final var tokenSigningKey = collectorsConfigService.get().map(CollectorsConfig::tokenSigningKey).orElseThrow(AssertionError::new);
        final JwksResponse response = jwksService.getJwks();

        assertThat(response.keys()).hasSize(1);
        assertThat(response.keys().get(0).kid()).isEqualTo(tokenSigningKey.fingerprint());
        assertThat(response.keys().get(0).kty()).isEqualTo("OKP");
    }
}
