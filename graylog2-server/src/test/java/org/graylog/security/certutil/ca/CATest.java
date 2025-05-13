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
package org.graylog.security.certutil.ca;

import org.graylog.security.certutil.CertRequest;
import org.graylog.security.certutil.CertificateGenerator;
import org.graylog.security.certutil.KeyPair;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class CATest {

    public static final Duration CERTIFICATE_VALIDITY = Duration.ofDays(365);

    final KeyPair rootCA = CertificateGenerator.generate(
            CertRequest.selfSigned("Graylog, Inc.")
                    .isCA(true)
                    .validity(CERTIFICATE_VALIDITY));

    final KeyPair intermediateCA = CertificateGenerator.generate(
            CertRequest.signed("Intermediate", rootCA)
                    .isCA(true)
                    .validity(CERTIFICATE_VALIDITY)
    );

    final KeyPair ca = CertificateGenerator.generate(
            CertRequest.signed("CA", intermediateCA)
                    .isCA(true)
                    .validity(CERTIFICATE_VALIDITY)
    );

    public CATest() throws Exception {
    }

    @Test
    void selfSignedCAValid() {
        CA testCA = new CA(List.of(rootCA.certificate()), rootCA.privateKey());
        assertThat(testCA.getCertificates()).hasSize(1);
    }

    @Test
    void certChainValid() {
        CA testCA = new CA(List.of(rootCA.certificate(), intermediateCA.certificate(), ca.certificate()), ca.privateKey());
        assertThat(testCA.getCertificates()).hasSize(3);
    }

    @Test
    void unOrderedCertChainValid() {
        CA testCA = new CA(List.of(intermediateCA.certificate(), ca.certificate(), rootCA.certificate()), ca.privateKey());
        assertThat(testCA.getCertificates()).hasSize(3);
    }

    @Test
    void throwsExceptionIfCertificateChainIsNull() {
        assertThatThrownBy(() -> new CA(null, rootCA.privateKey()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Certificate list is empty");
    }

    @Test
    void throwsExceptionIfCertificateChainIsEmpty() {
        assertThatThrownBy(() -> new CA(List.of(), rootCA.privateKey()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Certificate list is empty");
    }

    @Test
    void throwsExceptionIfCertificateNotACA() throws Exception {
        final KeyPair rootCA = CertificateGenerator.generate(
                CertRequest.selfSigned("Graylog, Inc.")
                        .isCA(false)
                        .validity(CERTIFICATE_VALIDITY)
        );
        assertThatThrownBy(() -> new CA(List.of(rootCA.certificate()), rootCA.privateKey()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("First certificate in certificate chain is no CA. Please make sure that your bundle only contains the CA and necessary intermediate/root certificates");
    }

    @Test
    void throwsExceptionIfInvalidPrivateKey() {
        assertThatThrownBy(() -> new CA(
                List.of(rootCA.certificate(), intermediateCA.certificate(), ca.certificate()),
                rootCA.privateKey()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Provided CA private key doesn't correspond to provided CA certificate");
    }

    @Test
    void throwsExceptionIfCertChainBranches() throws Exception {
        final KeyPair intermediateCA2 = CertificateGenerator.generate(
                CertRequest.signed("Intermediate", rootCA)
                        .isCA(true)
                        .validity(CERTIFICATE_VALIDITY)
        );
        assertThatThrownBy(() -> new CA(
                List.of(rootCA.certificate(), intermediateCA.certificate(), intermediateCA2.certificate(), ca.certificate()),
                rootCA.privateKey()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Corrupt certificate chain. Please make sure that your bundle only contains the CA and necessary intermediate/root certificates");
    }


}
