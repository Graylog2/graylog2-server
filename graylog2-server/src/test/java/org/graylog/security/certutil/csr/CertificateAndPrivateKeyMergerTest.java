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
import org.graylog.security.certutil.privatekey.PrivateKeyEncryptedStorage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.util.Enumeration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
public class CertificateAndPrivateKeyMergerTest {

    @Mock
    KeyPairChecker keyPairChecker;
    @Mock
    PrivateKeyEncryptedStorage privateKeyEncryptedStorage;

    CertificateAndPrivateKeyMerger toTest;

    @BeforeEach
    void setUp() {
        toTest = new CertificateAndPrivateKeyMerger(keyPairChecker);
    }

    @Test
    void testThrowsExceptionIfPrivateKeyAndCertificateDoNotMatch() throws Exception {
        final PrivateKey privateKey = mock(PrivateKey.class);
        final PublicKey publicKey = mock(PublicKey.class);
        final X509Certificate signedCertificate = mock(X509Certificate.class);
        doReturn(publicKey).when(signedCertificate).getPublicKey();
        doReturn(false).when(keyPairChecker).matchingKeys(privateKey, publicKey);
        doReturn(privateKey).when(privateKeyEncryptedStorage).readEncryptedKey("privPass".toCharArray());
        assertThrows(GeneralSecurityException.class, () -> toTest.merge(signedCertificate,
                privateKeyEncryptedStorage,
                "privPass".toCharArray(),
                "certPass".toCharArray(),
                "data-node")
        );
    }

    @Test
    void testMergingOnRealPairImplementation() throws Exception {
        final CertRequest req = CertRequest.selfSigned("localhost")
                .validity(Duration.ZERO);
        final KeyPair keyPair = CertificateGenerator.generate(req);

        final char[] privPass = "privPass".toCharArray();
        final char[] certPass = "certPass".toCharArray();
        final String alias = "data-node";
        

        doReturn(keyPair.privateKey()).when(privateKeyEncryptedStorage).readEncryptedKey(privPass);
        doReturn(true).when(keyPairChecker).matchingKeys(keyPair.privateKey(), keyPair.publicKey());

        final KeyStore merged = toTest.merge(keyPair.certificate(),
                privateKeyEncryptedStorage,
                privPass,
                certPass,
                alias);

        assertEquals(keyPair.privateKey(), merged.getKey(alias, certPass));
        assertEquals(keyPair.certificate(), merged.getCertificate(alias));

        final Enumeration<String> aliases = merged.aliases();
        assertEquals(alias, aliases.nextElement());
        assertFalse(aliases.hasMoreElements());
    }
}
