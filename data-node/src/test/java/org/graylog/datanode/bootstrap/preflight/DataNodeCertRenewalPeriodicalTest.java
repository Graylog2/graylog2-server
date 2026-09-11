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
package org.graylog.datanode.bootstrap.preflight;

import com.google.common.eventbus.EventBus;
import jakarta.annotation.Nonnull;
import org.assertj.core.api.Assertions;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;
import org.graylog.datanode.DatanodeTestUtils;
import org.graylog.datanode.configuration.DatanodeKeystore;
import org.graylog.datanode.opensearch.CsrRequester;
import org.graylog.security.certutil.CertRequest;
import org.graylog.security.certutil.CertificateGenerator;
import org.graylog.security.certutil.KeyPair;
import org.graylog.security.certutil.cert.CertificateChain;
import org.graylog.security.certutil.csr.CsrSigner;
import org.graylog2.plugin.certificates.RenewalPolicy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mockito;

import java.nio.file.Path;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.util.List;
import java.util.function.Supplier;

class DataNodeCertRenewalPeriodicalTest {

    private static final CertificateGenerator CERTIFICATE_GENERATOR = new CertificateGenerator(1024);

    private Path tempDir;

    @BeforeEach
    void setUp(@TempDir Path tempDir) {
        this.tempDir = tempDir;
    }

    @Test
    void testAlreadyExpired() throws Exception {
        final DatanodeKeystore datanodeKeystore = datanodeKeystore(Duration.ofNanos(1));
        final CsrRequester csrRequester = Mockito.mock(CsrRequester.class);
        final DataNodeCertRenewalPeriodical periodical = new DataNodeCertRenewalPeriodical(
                datanodeKeystore,
                autoRenewalPolicy("PT1M"),
                csrRequester,
                () -> false,
                 "my-hostname",
                 "my-hostname"
        );
        periodical.doRun();
        Mockito.verify(csrRequester, Mockito.times(1)).triggerCertificateSigningRequest();
    }


    @Test
    void testExpiringSoon() throws Exception {
        final DatanodeKeystore datanodeKeystore = datanodeKeystore(Duration.ofMinutes(1));
        final CsrRequester csrRequester = Mockito.mock(CsrRequester.class);
        final DataNodeCertRenewalPeriodical periodical = new DataNodeCertRenewalPeriodical(
                datanodeKeystore,
                autoRenewalPolicy("PT1M"),
                csrRequester,
                () -> false,
                "my-hostname",
                "my-hostname");
        periodical.doRun();
        Mockito.verify(csrRequester, Mockito.times(1)).triggerCertificateSigningRequest();
    }

    @Test
    void testHostnameChanged() throws Exception {
        final DatanodeKeystore datanodeKeystore = datanodeKeystore(Duration.ofDays(30));
        final CsrRequester csrRequester = Mockito.mock(CsrRequester.class);
        final DataNodeCertRenewalPeriodical periodical = new DataNodeCertRenewalPeriodical(
                datanodeKeystore,
                autoRenewalPolicy("PT1M"),
                csrRequester,
                () -> false,
                "my-new-unexpected-hostname",
                "my-new-unexpected-hostname");

        periodical.doRun();
        Mockito.verify(csrRequester, Mockito.times(1)).triggerCertificateSigningRequest();
    }

    @Test
    void testPublishHostChanged() throws Exception {
        final DatanodeKeystore datanodeKeystore = datanodeKeystore(Duration.ofDays(30));
        final CsrRequester csrRequester = Mockito.mock(CsrRequester.class);
        // hostname is unchanged and still matches an existing SAN, but the publish host was newly
        // set/changed to something not covered by the currently signed certificate.
        final DataNodeCertRenewalPeriodical periodical = new DataNodeCertRenewalPeriodical(
                datanodeKeystore,
                autoRenewalPolicy("PT1M"),
                csrRequester,
                () -> false,
                "my-hostname",
                "10.100.100.93");

        periodical.doRun();
        Mockito.verify(csrRequester, Mockito.times(1)).triggerCertificateSigningRequest();
    }

    @Test
    void testPublishHostIPv6NotChanged() throws Exception {
        // configured using the compressed literal, as an operator would write it
        final String configuredPublishHost = "2001:db8::1";
        final DatanodeKeystore datanodeKeystore = datanodeKeystore(Duration.ofDays(30), List.of(configuredPublishHost));
        final CsrRequester csrRequester = Mockito.mock(CsrRequester.class);

        // the certificate's SAN is decoded by the JVM into its expanded form, e.g. "2001:db8:0:0:0:0:0:1",
        // which must still be recognised as the same address as the configured compressed literal above.
        Assertions.assertThat(datanodeKeystore.getSubjectAlternativeNames()).doesNotContain(configuredPublishHost);

        final DataNodeCertRenewalPeriodical periodical = new DataNodeCertRenewalPeriodical(
                datanodeKeystore,
                autoRenewalPolicy("PT1M"),
                csrRequester,
                () -> false,
                "my-hostname",
                configuredPublishHost);

        periodical.doRun();
        Mockito.verify(csrRequester, Mockito.never()).triggerCertificateSigningRequest();
    }


    @Test
    void testExpiringInFarFuture() throws Exception {
        final DatanodeKeystore datanodeKeystore = datanodeKeystore(Duration.ofDays(30));
        final CsrRequester csrRequester = Mockito.mock(CsrRequester.class);
        final DataNodeCertRenewalPeriodical periodical = new DataNodeCertRenewalPeriodical(datanodeKeystore, autoRenewalPolicy("P3M"), csrRequester, () -> false, "my-hostname", "my-hostname");
        periodical.doRun();
        Mockito.verify(csrRequester, Mockito.never()).triggerCertificateSigningRequest();
    }

    @Nonnull
    private static Supplier<RenewalPolicy> autoRenewalPolicy(String duration) {
        return () -> new RenewalPolicy(RenewalPolicy.Mode.AUTOMATIC, duration);
    }


    private DatanodeKeystore datanodeKeystore(Duration certValidity) throws Exception {
        return datanodeKeystore(certValidity, List.of("second-hostname"));
    }

    private DatanodeKeystore datanodeKeystore(Duration certValidity, List<String> altNames) throws Exception {
        final DatanodeKeystore datanodeKeystore = new DatanodeKeystore(DatanodeTestUtils.tempDirectories(tempDir), "foobar", new EventBus());
        datanodeKeystore.create(DatanodeTestUtils.generateKeyPair(certValidity));

        final PKCS10CertificationRequest csr = datanodeKeystore.createCertificateSigningRequest("my-hostname", altNames);
        Assertions.assertThat(csr.getSubject().toString()).isEqualTo("CN=my-hostname");

        final CsrSigner signer = new CsrSigner();
        final KeyPair ca = CERTIFICATE_GENERATOR.generateKeyPair(CertRequest.selfSigned("Graylog CA").isCA(true).validity(Duration.ofDays(365)));
        final X509Certificate datanodeCert = signer.sign(ca.privateKey(), ca.certificate(), csr, (int) certValidity.toDays());
        final CertificateChain certChain = new CertificateChain(datanodeCert, List.of(ca.certificate()));

        datanodeKeystore.replaceCertificatesInKeystore(certChain);

        return datanodeKeystore;
    }
}
