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
package org.graylog.security.certutil;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import java.security.KeyStore;
import java.time.Duration;

class CertificateGeneratorTest {

    @Test
    void testToKeystore() throws Exception {
        final KeyPair keyPair = CertificateGenerator.generate(CertRequest.selfSigned("datanode").isCA(false).validity(Duration.ofDays(99 * 365)));
        final String alias = "my-key";
        final char[] password = "my-password".toCharArray();
        final KeyStore keystore = keyPair.toKeystore(alias, password);

        Assertions.assertThat(keystore.getKey(alias, password)).isEqualTo(keyPair.privateKey());
        Assertions.assertThat(keystore.getCertificate(alias).getPublicKey()).isEqualTo(keyPair.publicKey());
    }
}
