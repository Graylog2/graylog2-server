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

import com.google.common.eventbus.EventBus;
import jakarta.annotation.Nonnull;
import org.apache.commons.lang3.RandomStringUtils;
import org.assertj.core.api.Assertions;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;
import org.graylog.datanode.configuration.DatanodeDirectories;
import org.graylog.datanode.configuration.DatanodeKeystore;
import org.graylog.datanode.configuration.OpensearchKeystoreProvider;
import org.graylog.security.certutil.CertRequest;
import org.graylog.security.certutil.CertificateDto;
import org.graylog.security.certutil.CertificateGenerator;
import org.graylog.security.certutil.KeyPair;
import org.graylog.security.certutil.KeyStoreDto;
import org.graylog.security.certutil.cert.CertificateChain;
import org.graylog.security.certutil.csr.CsrSigner;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.file.Path;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
public class CertificatesControllerTest {


    private CertificatesController certificatesController;

    @BeforeEach
    public void setup(@TempDir Path tempDir) throws Exception {

        certificatesController = new CertificatesController(testKeyStore(tempDir), Map.of(
                OpensearchKeystoreProvider.Store.TRUSTSTORE, KeyStoreDto.empty(),
                OpensearchKeystoreProvider.Store.HTTP, KeyStoreDto.empty(),
                OpensearchKeystoreProvider.Store.TRANSPORT, KeyStoreDto.empty())
        );
    }

    @Test
    public void testOptionalSecurityConfiguration() {
        Map<OpensearchKeystoreProvider.Store, KeyStoreDto> certificates = certificatesController.getCertificates();
        assertThat(certificates).hasSize(4);
        assertThat(certificates.get(OpensearchKeystoreProvider.Store.CONFIGURED).certificates())
                .hasSize(1)
                .hasEntrySatisfying("datanode", datanodeCerts -> {
                    Assertions.assertThat(datanodeCerts)
                            .hasSize(4)
                            .map(CertificateDto::subject)
                            .contains("CN=my-hostname", "CN=server", "CN=intermediate", "CN=root");
                });


        assertThat(certificates.get(OpensearchKeystoreProvider.Store.TRUSTSTORE).certificates()).hasSize(0);
    }

    private DatanodeKeystore testKeyStore(Path tempDir) throws Exception {
        final String keystorePass = RandomStringUtils.secure().next(96);
        final DatanodeKeystore datanodeKeystore = new DatanodeKeystore(new DatanodeDirectories(tempDir, tempDir, tempDir, tempDir), keystorePass, new EventBus());
        datanodeKeystore.create(generateKeyPair());


        final KeyPair rootCa = CertificateGenerator.generate(CertRequest.selfSigned("root")
                .isCA(true)
                .validity(Duration.ofDays(365)));

        final KeyPair intermediate = CertificateGenerator.generate(CertRequest.signed("intermediate", rootCa)
                .isCA(true)
                .validity(Duration.ofDays(365)));

        final KeyPair server = CertificateGenerator.generate(CertRequest.signed("server", intermediate)
                .isCA(false)
                .validity(Duration.ofDays(365)));

        final PKCS10CertificationRequest csr = datanodeKeystore.createCertificateSigningRequest("my-hostname", List.of("second-hostname"));

        final CsrSigner signer = new CsrSigner();
        final X509Certificate datanodeCert = signer.sign(server.privateKey(), server.certificate(), csr, 30);
        final CertificateChain certChain = new CertificateChain(datanodeCert, List.of(server.certificate(), intermediate.certificate(), rootCa.certificate()));

        datanodeKeystore.replaceCertificatesInKeystore(certChain);
        return datanodeKeystore;
    }

    @Nonnull
    private static KeyPair generateKeyPair() throws Exception {
        final CertRequest certRequest = CertRequest.selfSigned(DatanodeKeystore.DATANODE_KEY_ALIAS)
                .isCA(false)
                .validity(Duration.ofDays(31));
        return CertificateGenerator.generate(certRequest);
    }
}
