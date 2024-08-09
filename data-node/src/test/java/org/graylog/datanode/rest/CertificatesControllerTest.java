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
package org.graylog.datanode.rest;

import org.graylog.datanode.configuration.DatanodeKeystore;
import org.graylog.datanode.opensearch.OpensearchProcess;
import org.graylog.security.certutil.CertRequest;
import org.graylog.security.certutil.CertificateGenerator;
import org.graylog.security.certutil.KeyPair;
import org.graylog.security.certutil.KeyStoreDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.security.KeyStore;
import java.security.cert.Certificate;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.graylog.security.certutil.CertConstants.PKCS12;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class CertificatesControllerTest {

    @Mock
    private DatanodeKeystore datanodeKeystore;
    @Mock
    private OpensearchProcess opensearchProcess;

    private CertificatesController certificatesController;

    @BeforeEach
    public void setup() {
        certificatesController = new CertificatesController(datanodeKeystore, opensearchProcess);
    }

    @Test
    public void testOptionalSecurityConfiguration() throws Exception {
        when(opensearchProcess.getOpensearchConfiguration()).thenReturn(Optional.empty());
        when(datanodeKeystore.loadKeystore()).thenReturn(testKeyStore());
        Map<CertificatesController.Store, KeyStoreDto> certificates = certificatesController.getCertificates();
        assertThat(certificates).hasSize(4);
        assertThat(certificates.get(CertificatesController.Store.CONFIGURED).certificates()).hasSize(3);
        assertThat(certificates.get(CertificatesController.Store.CONFIGURED).certificates().get("ca")).hasSize(1);
        assertThat(certificates.get(CertificatesController.Store.CONFIGURED).certificates().get("host")).hasSize(2);
        assertThat(certificates.get(CertificatesController.Store.CONFIGURED).certificates().get("cert")).hasSize(1);
        assertThat(certificates.get(CertificatesController.Store.TRUSTSTORE).certificates()).hasSize(0);
    }

    private KeyStore testKeyStore() throws Exception {
        char[] pass = "dummy".toCharArray();
        KeyStore keystore = KeyStore.getInstance(PKCS12);
        keystore.load(null, null);
        final CertRequest certRequest = CertRequest.selfSigned("ca")
                .isCA(true)
                .validity(Duration.ofDays(1));
        KeyPair ca = CertificateGenerator.generate(certRequest);
        final CertRequest certRequest2 = CertRequest.signed("host", ca)
                .isCA(false)
                .withSubjectAlternativeName("altname")
                .validity(Duration.ofDays(1));
        KeyPair host = CertificateGenerator.generate(certRequest2);
        keystore.setKeyEntry("ca", ca.privateKey(), pass, new Certificate[]{ca.certificate()});
        keystore.setKeyEntry("host", host.privateKey(), pass, new Certificate[]{host.certificate(), ca.certificate()});
        keystore.setCertificateEntry("cert", ca.certificate());
        return keystore;
    }

}
