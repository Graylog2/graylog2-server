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
package org.graylog.security.pki;

import org.junit.jupiter.api.Test;

import java.security.KeyPair;

import static org.assertj.core.api.Assertions.assertThat;

class KeyUtilsTest {
    @Test
    void generateKeyPairEd25519() throws Exception {
        final KeyPair keyPair = KeyUtils.generateKeyPair(Algorithm.ED25519);

        assertThat(keyPair).isNotNull();
        assertThat(keyPair.getPublic().getAlgorithm()).isEqualTo("Ed25519");
        assertThat(keyPair.getPrivate().getAlgorithm()).isEqualTo("Ed25519");
    }

    @Test
    void generateKeyPairRsa4096() throws Exception {
        final KeyPair keyPair = KeyUtils.generateKeyPair(Algorithm.RSA_4096);

        assertThat(keyPair).isNotNull();
        assertThat(keyPair.getPublic().getAlgorithm()).isEqualTo("RSA");
        assertThat(keyPair.getPrivate().getAlgorithm()).isEqualTo("RSA");
    }

    @Test
    void generateKeyPairProducesUniqueKeys() throws Exception {
        final KeyPair keyPair1 = KeyUtils.generateKeyPair(Algorithm.ED25519);
        final KeyPair keyPair2 = KeyUtils.generateKeyPair(Algorithm.ED25519);

        assertThat(keyPair1.getPublic().getEncoded()).isNotEqualTo(keyPair2.getPublic().getEncoded());
    }

    @Test
    void sha256FingerprintHasExpectedFormat() throws Exception {
        final KeyPair keyPair = KeyUtils.generateKeyPair(Algorithm.ED25519);
        final String fingerprint = KeyUtils.sha256Fingerprint(keyPair);

        // SHA-256 base64 without padding: "SHA256:" + 43 base64 chars
        assertThat(fingerprint).startsWith("SHA256:");
        assertThat(fingerprint).matches("SHA256:[A-Za-z0-9+/]{43}");
    }

    @Test
    void sha256FingerprintIsDeterministicForSameKey() throws Exception {
        final KeyPair keyPair = KeyUtils.generateKeyPair(Algorithm.ED25519);

        assertThat(KeyUtils.sha256Fingerprint(keyPair)).isEqualTo(KeyUtils.sha256Fingerprint(keyPair));
    }

    @Test
    void sha256FingerprintDiffersForDifferentKeys() throws Exception {
        final KeyPair keyPair1 = KeyUtils.generateKeyPair(Algorithm.ED25519);
        final KeyPair keyPair2 = KeyUtils.generateKeyPair(Algorithm.ED25519);

        assertThat(KeyUtils.sha256Fingerprint(keyPair1)).isNotEqualTo(KeyUtils.sha256Fingerprint(keyPair2));
    }

    @Test
    void sha256FingerprintWorksWithRsa() throws Exception {
        final KeyPair keyPair = KeyUtils.generateKeyPair(Algorithm.RSA_4096);
        final String fingerprint = KeyUtils.sha256Fingerprint(keyPair);

        assertThat(fingerprint).matches("SHA256:[A-Za-z0-9+/]{43}");
    }
}
