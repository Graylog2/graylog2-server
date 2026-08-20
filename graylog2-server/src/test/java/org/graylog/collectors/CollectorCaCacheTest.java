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

import com.google.common.eventbus.EventBus;
import org.bouncycastle.asn1.x509.KeyPurposeId;
import org.bouncycastle.asn1.x509.KeyUsage;
import org.graylog.collectors.events.CollectorCaConfigUpdated;
import org.graylog.security.pki.Algorithm;
import org.graylog.security.pki.CertificateBuilder;
import org.graylog.security.pki.CertificateEntry;
import org.graylog.security.pki.CertificateService;
import org.graylog.security.pki.PemUtils;
import org.graylog.testing.TestClocks;
import org.graylog2.security.encryption.EncryptedValueService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CollectorCaCacheTest {

    private final EncryptedValueService encryptedValueService = new EncryptedValueService("1234567890abcdef");
    private final EventBus eventBus = new EventBus();

    private CollectorCaService caService;
    private CertificateService certService;
    private CollectorCaCache cache;

    private CertificateEntry caCert;
    private CertificateEntry signingCert;
    private CertificateEntry serverCert;

    @BeforeEach
    void setUp() throws Exception {
        caService = mock(CollectorCaService.class);
        certService = mock(CertificateService.class);

        final var certBuilder = new CertificateBuilder(encryptedValueService, "Test", TestClocks.fixedEpoch());

        caCert = certBuilder.createRootCa("Test CA", Algorithm.ED25519, Duration.ofDays(365));
        signingCert = certBuilder.createIntermediateCa("Test Signing", caCert, Duration.ofDays(365));
        serverCert = certBuilder.createEndEntityCert(
                "Test Server", signingCert,
                KeyUsage.digitalSignature, KeyPurposeId.id_kp_serverAuth,
                Duration.ofDays(365)
        );

        when(caService.getCaCert()).thenReturn(caCert);
        when(caService.getSigningCert()).thenReturn(signingCert);
        when(caService.getOtlpServerCert()).thenReturn(serverCert);

        cache = new CollectorCaCache(caService, certService, encryptedValueService, eventBus, TestClocks.fixedEpoch());
    }

    @Test
    void getReturnsCorrectCertificateForCaKey() {
        final var entry = cache.getCa();

        assertThat(entry).isNotNull();
        assertThat(entry.fingerprint()).isEqualTo(caCert.fingerprint());
        assertThat(entry.cert()).isNotNull();
        assertThat(entry.privateKey()).isNotNull();
    }

    @Test
    void getReturnsCorrectCertificateForSigningKey() {
        final var entry = cache.getSigning();

        assertThat(entry).isNotNull();
        assertThat(entry.fingerprint()).isEqualTo(signingCert.fingerprint());
    }

    @Test
    void getReturnsCorrectCertificateForServerKey() {
        final var entry = cache.getServer();

        assertThat(entry).isNotNull();
        assertThat(entry.fingerprint()).isEqualTo(serverCert.fingerprint());
    }

    @Test
    void getCachesResults() {
        final var first = cache.getCa();
        final var second = cache.getCa();

        assertThat(first).isSameAs(second);
        verify(caService, times(1)).getCaCert();
    }

    @Test
    void invalidateAllClearsCache() {
        cache.getCa();
        verify(caService, times(1)).getCaCert();

        cache.handleCollectorsConfigEvent(new CollectorCaConfigUpdated());

        cache.getCa();
        verify(caService, times(2)).getCaCert();
    }

    @Test
    void eventBusInvalidatesCache() throws Exception {
        cache.startAsync().awaitRunning();
        try {
            cache.getSigning();
            verify(caService, times(1)).getSigningCert();

            eventBus.post(new CollectorCaConfigUpdated());

            cache.getSigning();
            verify(caService, times(2)).getSigningCert();
        } finally {
            cache.stopAsync().awaitTerminated();
        }
    }

    @Test
    void serviceExceptionPropagates() {
        when(caService.getCaCert()).thenThrow(new IllegalStateException("CA not initialized"));

        assertThatThrownBy(() -> cache.getCa())
                .isInstanceOf(RuntimeException.class)
                .hasCauseInstanceOf(IllegalStateException.class);
    }

    @Test
    void eachKeyLoadsIndependently() {
        cache.getCa();
        cache.getSigning();
        cache.getServer();

        verify(caService, times(1)).getCaCert();
        verify(caService, times(1)).getSigningCert();
        verify(caService, times(1)).getOtlpServerCert();
    }

    @Test
    void invalidationReloadsUpdatedCert() throws Exception {
        cache.getServer();

        final var certBuilder = new CertificateBuilder(encryptedValueService, "Test", TestClocks.fixedEpoch());
        final var newServerCert = certBuilder.createEndEntityCert(
                "New Server", signingCert,
                KeyUsage.digitalSignature, KeyPurposeId.id_kp_serverAuth,
                Duration.ofDays(365)
        );
        when(caService.getOtlpServerCert()).thenReturn(newServerCert);

        cache.handleCollectorsConfigEvent(new CollectorCaConfigUpdated());

        final var entry = cache.getServer();
        assertThat(entry.fingerprint()).isEqualTo(newServerCert.fingerprint());
    }

    @Test
    void getBySubjectKeyIdentifier_returnsCacheEntryForKnownSubjectKeyIdentifier() throws Exception {
        when(certService.findBySubjectKeyIdentifier(serverCert.subjectKeyIdentifier())).thenReturn(Optional.of(serverCert));

        final var result = cache.getBySubjectKeyIdentifier(serverCert.subjectKeyIdentifier());

        assertThat(result).isPresent();
        assertThat(result.get().cert().getSerialNumber()).isEqualTo(PemUtils.parseCertificate(serverCert.certificate()).getSerialNumber());
        assertThat(result.get().privateKey()).isNotNull();
    }

    @Test
    void getBySubjectKeyIdentifier_returnsEmptyForUnknownSubjectKeyIdentifier() {
        when(certService.findBySubjectKeyIdentifier("unknown-ski")).thenReturn(Optional.empty());

        assertThat(cache.getBySubjectKeyIdentifier("unknown-ski")).isEmpty();
    }

    @Test
    void getBySubjectKeyIdentifier_rejectsBlankSubjectKeyIdentifier() {
        assertThatThrownBy(() -> cache.getBySubjectKeyIdentifier(""))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void getBySubjectKeyIdentifier_rejectsNullSubjectKeyIdentifier() {
        assertThatThrownBy(() -> cache.getBySubjectKeyIdentifier(null))
                .isInstanceOf(Exception.class);
    }

    @Test
    void startAndStopLifecycle() throws Exception {
        cache.startAsync().awaitRunning();
        assertThat(cache.isRunning()).isTrue();

        cache.stopAsync().awaitTerminated();
        assertThat(cache.isRunning()).isFalse();
    }
}
