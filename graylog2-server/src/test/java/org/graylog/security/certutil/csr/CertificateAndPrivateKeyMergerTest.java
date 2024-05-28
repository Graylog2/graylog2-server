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
package org.graylog.security.certutil.csr;

import org.graylog.security.certutil.CertRequest;
import org.graylog.security.certutil.CertificateGenerator;
import org.graylog.security.certutil.KeyPair;
import org.graylog.security.certutil.cert.CertificateChain;
import org.graylog.security.certutil.privatekey.PrivateKeyEncryptedStorage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.security.GeneralSecurityException;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.time.Duration;
import java.util.Enumeration;
import java.util.List;

import static org.graylog.security.certutil.CertConstants.KEY_GENERATION_ALGORITHM;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class CertificateAndPrivateKeyMergerTest {

    CertificateAndPrivateKeyMerger toTest;

    @BeforeEach
    void setUp() {
        toTest = new CertificateAndPrivateKeyMerger();
    }

    @Test
    void testThrowsExceptionIfPrivateKeyAndCertificateDoNotMatch() throws Exception {

        KeyPairGenerator keyGen1 = KeyPairGenerator.getInstance(KEY_GENERATION_ALGORITHM);
        keyGen1.initialize(2048);
        final java.security.KeyPair keyPair1 = keyGen1.genKeyPair();
        final PrivateKeyEncryptedStorage keyStorage = mockPrivateKeyStorage(keyPair1.getPrivate());

        final CertRequest req = CertRequest.selfSigned("localhost")
                .validity(Duration.ofHours(24));
        final KeyPair keyPair = CertificateGenerator.generate(req);

        assertThrows(GeneralSecurityException.class, () -> toTest.merge(new CertificateChain(keyPair.certificate(), List.of()),
                keyStorage,
                "privPass".toCharArray(),
                "certPass".toCharArray(),
                "data-node")
        );
    }

    @Test
    void testMergingOnRealPairImplementation() throws Exception {
        final CertRequest req = CertRequest.selfSigned("localhost")
                .validity(Duration.ofHours(24));
        final KeyPair keyPair = CertificateGenerator.generate(req);

        final char[] privPass = "privPass".toCharArray();
        final char[] certPass = "certPass".toCharArray();
        final String alias = "data-node";

        final PrivateKeyEncryptedStorage storage = mockPrivateKeyStorage(keyPair.privateKey());

        final KeyStore merged = toTest.merge(new CertificateChain(keyPair.certificate(), List.of()),
                storage,
                privPass,
                certPass,
                alias);

        assertEquals(keyPair.privateKey(), merged.getKey(alias, certPass));
        assertEquals(keyPair.certificate(), merged.getCertificate(alias));

        final Enumeration<String> aliases = merged.aliases();
        assertEquals(alias, aliases.nextElement());
        assertFalse(aliases.hasMoreElements());
    }

    private static PrivateKeyEncryptedStorage mockPrivateKeyStorage(PrivateKey privateKey) {
        return new PrivateKeyEncryptedStorage() {
            @Override
            public void writeEncryptedKey(char[] password, PrivateKey privateKey) {
                // do nothing
            }

            @Override
            public PrivateKey readEncryptedKey(char[] password) {
                return privateKey;
            }
        };
    }
}
