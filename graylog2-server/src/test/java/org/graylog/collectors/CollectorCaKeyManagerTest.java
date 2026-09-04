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
package org.graylog.collectors;

import org.bouncycastle.asn1.x509.KeyPurposeId;
import org.bouncycastle.asn1.x509.KeyUsage;
import org.graylog.security.pki.Algorithm;
import org.graylog.security.pki.CertificateBuilder;
import org.graylog.security.pki.CertificateEntry;
import org.graylog.security.pki.PemUtils;
import org.graylog.testing.TestClocks;
import org.graylog2.security.encryption.EncryptedValueService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.security.cert.X509Certificate;
import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CollectorCaKeyManagerTest {

    private final EncryptedValueService encryptedValueService = new EncryptedValueService("1234567890abcdef");
    private final CertificateBuilder certBuilder = new CertificateBuilder(encryptedValueService, "Test", TestClocks.fixedEpoch());

    private CollectorCaKeyManager keyManager;
    private CollectorCaCache.CacheEntry serverEntry;
    private CollectorCaCache.CacheEntry signingEntry;

    @BeforeEach
    void setUp() throws Exception {
        final var caCertEntry = certBuilder.createRootCa("Test CA", Algorithm.ED25519, Duration.ofDays(365));
        final var signingCertEntry = certBuilder.createIntermediateCa("Test Signing", caCertEntry, Duration.ofDays(365));
        final var serverCertEntry = certBuilder.createEndEntityCert(
                "Test Server", signingCertEntry, KeyUsage.digitalSignature | KeyUsage.keyEncipherment,
                KeyPurposeId.id_kp_serverAuth, Duration.ofDays(30), List.of("cluster-id"));

        serverEntry = cacheEntry(serverCertEntry);
        signingEntry = cacheEntry(signingCertEntry);

        final var caCache = mock(CollectorCaCache.class);
        when(caCache.getServer()).thenReturn(serverEntry);
        when(caCache.getSigning()).thenReturn(signingEntry);

        keyManager = new CollectorCaKeyManager(caCache);
    }

    @Test
    void chooseServerAlias_returnsAliasForEdDSA() {
        assertThat(keyManager.chooseServerAlias("EdDSA", null, null)).isEqualTo("server");
    }

    @Test
    void chooseServerAlias_returnsAliasForEd25519() {
        assertThat(keyManager.chooseServerAlias("Ed25519", null, null)).isEqualTo("server");
    }

    @Test
    void chooseServerAlias_returnsNullForRSA() {
        assertThat(keyManager.chooseServerAlias("RSA", null, null)).isNull();
    }

    @Test
    void chooseServerAlias_returnsNullForEC() {
        assertThat(keyManager.chooseServerAlias("EC", null, null)).isNull();
    }

    @Test
    void getCertificateChain_returnsServerAndSigningCertsForServerAlias() {
        final X509Certificate[] chain = keyManager.getCertificateChain("server");

        assertThat(chain).hasSize(2);
        assertThat(chain[0]).isEqualTo(serverEntry.cert());
        assertThat(chain[1]).isEqualTo(signingEntry.cert());
    }

    @Test
    void getCertificateChain_returnsNullForUnknownAlias() {
        assertThat(keyManager.getCertificateChain("unknown")).isNull();
    }

    @Test
    void getPrivateKey_returnsKeyForServerAlias() {
        assertThat(keyManager.getPrivateKey("server")).isEqualTo(serverEntry.privateKey());
    }

    @Test
    void getPrivateKey_returnsNullForUnknownAlias() {
        assertThat(keyManager.getPrivateKey("unknown")).isNull();
    }

    @Test
    void chooseEngineServerAlias_returnsAliasForEdDSA() {
        assertThat(keyManager.chooseEngineServerAlias("EdDSA", null, null)).isEqualTo("server");
    }

    @Test
    void chooseEngineServerAlias_returnsAliasForEd25519() {
        assertThat(keyManager.chooseEngineServerAlias("Ed25519", null, null)).isEqualTo("server");
    }

    @Test
    void chooseEngineServerAlias_returnsNullForRSA() {
        assertThat(keyManager.chooseEngineServerAlias("RSA", null, null)).isNull();
    }

    @Test
    void getClientAliases_returnsNull() {
        assertThat(keyManager.getClientAliases("EdDSA", null)).isNull();
    }

    @Test
    void chooseClientAlias_returnsNull() {
        assertThat(keyManager.chooseClientAlias(new String[]{"EdDSA"}, null, null)).isNull();
    }

    @Test
    void getServerAliases_returnsNull() {
        assertThat(keyManager.getServerAliases("EdDSA", null)).isNull();
    }

    private CollectorCaCache.CacheEntry cacheEntry(CertificateEntry entry) throws Exception {
        final var cert = PemUtils.parseCertificate(entry.certificate());
        final var privateKey = PemUtils.parsePrivateKey(encryptedValueService.decrypt(entry.privateKey()));
        return new CollectorCaCache.CacheEntry(privateKey, cert, entry.fingerprint());
    }
}
