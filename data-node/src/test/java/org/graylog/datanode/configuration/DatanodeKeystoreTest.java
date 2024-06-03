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
package org.graylog.datanode.configuration;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import jakarta.annotation.Nonnull;
import org.assertj.core.api.Assertions;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;
import org.graylog.security.certutil.CertRequest;
import org.graylog.security.certutil.CertificateGenerator;
import org.graylog.security.certutil.KeyPair;
import org.graylog.security.certutil.cert.CertificateChain;
import org.graylog.security.certutil.csr.CsrSigner;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.util.LinkedList;
import java.util.List;

class DatanodeKeystoreTest {

    private EventBus eventBus;
    private final List<DatanodeKeystoreChangedEvent> receivedEvents = new LinkedList<>();

    @BeforeEach
    void setUp() {
        eventBus = new EventBus();
        eventBus.register(this);
    }

    @AfterEach
    void tearDown() {
        eventBus.unregister(this);
    }

    @Subscribe
    public void subscribe(DatanodeKeystoreChangedEvent event) {
        // remember received events so we can verify them later
        receivedEvents.add(event);
    }

    @Test
    void testCreateRead(@TempDir Path tempDir) throws Exception {
        final DatanodeKeystore datanodeKeystore = new DatanodeKeystore(new DatanodeDirectories(tempDir, tempDir, tempDir, tempDir), "foobar", this.eventBus);
        Assertions.assertThat(datanodeKeystore.exists()).isFalse();

        final KeyPair keyPair = generateKeyPair();

        datanodeKeystore.create(keyPair);
        Assertions.assertThat(datanodeKeystore.exists()).isTrue();

        Assertions.assertThat(datanodeKeystore.hasSignedCertificate()).isFalse();
        final PKCS10CertificationRequest csr = datanodeKeystore.createCertificateSigningRequest("my-hostname", List.of("second-hostname"));
        Assertions.assertThat(csr.getSubject().toString()).isEqualTo("CN=my-hostname");

        final CsrSigner signer = new CsrSigner();
        final KeyPair ca = CertificateGenerator.generate(CertRequest.selfSigned("Graylog CA").isCA(true).validity(Duration.ofDays(365)));
        final X509Certificate datanodeCert = signer.sign(ca.privateKey(), ca.certificate(), csr, 30);
        final CertificateChain certChain = new CertificateChain(datanodeCert, List.of(ca.certificate()));

        datanodeKeystore.replaceCertificatesInKeystore(certChain);

        Assertions.assertThat(this.receivedEvents).hasSize(1);

        Assertions.assertThat(datanodeKeystore.hasSignedCertificate()).isTrue();
    }

    @Nonnull
    private static KeyPair generateKeyPair() throws Exception {
        final CertRequest certRequest = CertRequest.selfSigned(DatanodeKeystore.DATANODE_KEY_ALIAS)
                .isCA(false)
                .validity(Duration.ofDays(31));
        return CertificateGenerator.generate(certRequest);
    }
}
