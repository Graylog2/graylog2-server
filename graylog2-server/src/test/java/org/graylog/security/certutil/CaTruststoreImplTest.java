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

import jakarta.annotation.Nonnull;
import org.assertj.core.api.Assertions;
import org.graylog.security.certutil.ca.exceptions.KeyStoreStorageException;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.security.KeyStore;
import java.security.KeyStoreException;
import java.time.Duration;
import java.util.Optional;

class CaTruststoreImplTest {

    @Test
    void testAbsenceOfPrivateKey() throws Exception {
        final KeyPair caKeys = generateCaKeys();

        final String password = "my-pass";
        final KeyStore keystore = caKeys.toKeystore(CertConstants.CA_KEY_ALIAS, password.toCharArray());

        // verify that the keystore contains a private key. It should be there, together with a cert
        Assertions.assertThat(keystore.isKeyEntry(CertConstants.CA_KEY_ALIAS)).isTrue();

        final CaTruststore truststore = new CaTruststoreImpl(mockCaService(keystore, password));
        Assertions.assertThat(truststore.getTrustStore())
                .isPresent()
                .hasValueSatisfying(ts -> {
                    try {
                        // no private key, just a certificate
                        Assertions.assertThat(ts.isKeyEntry(CertConstants.CA_KEY_ALIAS)).isFalse();
                        Assertions.assertThat(ts.isCertificateEntry(CertConstants.CA_KEY_ALIAS)).isTrue();
                    } catch (KeyStoreException e) {
                        throw new RuntimeException(e);
                    }
                });
    }

    @Nonnull
    private static CaPersistenceService mockCaService(KeyStore keystore, String password) throws KeyStoreStorageException {
        final CaPersistenceService caPersistenceService = Mockito.mock(CaPersistenceService.class);
        Mockito.when(caPersistenceService.loadKeyStore()).thenReturn(Optional.of(new CaKeystoreWithPassword(keystore, password)));
        return caPersistenceService;
    }

    @Nonnull
    private static KeyPair generateCaKeys() throws Exception {
        return CertificateGenerator.generate(CertRequest.selfSigned("my-ca").isCA(true).validity(Duration.ofDays(999)));
    }
}
