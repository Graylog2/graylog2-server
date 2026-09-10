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
package org.graylog.datanode.opensearch;

import com.google.common.eventbus.EventBus;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;
import org.graylog.datanode.DatanodeTestUtils;
import org.graylog.datanode.configuration.DatanodeKeystore;
import org.graylog.security.certutil.KeyPair;
import org.graylog.security.certutil.cert.CertificateChain;
import org.graylog.security.certutil.csr.CsrGenerator;
import org.graylog.security.certutil.csr.CsrSigner;
import org.graylog.security.certutil.csr.InMemoryKeystoreInformation;
import org.graylog2.events.ClusterEventBus;
import org.graylog2.notifications.Notification;
import org.graylog2.plugin.system.NodeId;
import org.graylog2.plugin.system.SimpleNodeId;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;

class CertificateReloadVerifierTest {

    private static final char[] KEYSTORE_PASSWORD = "changeit".toCharArray();

    // kept short so the tests don't have to wait out the real production timeouts
    private static final Duration VERIFICATION_TIMEOUT = Duration.ofMillis(200);
    private static final Duration RETRY_INTERVAL = Duration.ofMillis(20);
    private static final Duration SOCKET_TIMEOUT = Duration.ofSeconds(1);

    private final ClusterEventBus clusterEventBus = new ClusterEventBus();


    private final NodeId nodeId = new SimpleNodeId("test-node-id");

    private TlsTestServer server;
    private DatanodeNotificationsReceiver notificationsReceiver;

    @BeforeEach
    void setUp() {
        this.notificationsReceiver = new DatanodeNotificationsReceiver(clusterEventBus);
    }

    @AfterEach
    void tearDown() throws IOException {
        if (server != null) {
            server.close();
        }
    }

    private CertificateReloadVerifier newVerifier(DatanodeKeystore datanodeKeystore) {
        return new CertificateReloadVerifier(datanodeKeystore, clusterEventBus, nodeId,
                VERIFICATION_TIMEOUT, RETRY_INTERVAL, SOCKET_TIMEOUT);
    }

    // generous upper bound the verifier should never actually need given VERIFICATION_TIMEOUT/RETRY_INTERVAL above -
    // awaitCompletion() returns as soon as the verifier is actually done, so tests don't sit around waiting for it.
    private static final Duration AWAIT_COMPLETION_TIMEOUT = Duration.ofSeconds(2);

    @Test
    void doesNotEscalateWhenLiveCertificateMatchesExpectation(@TempDir Path tempDir) throws Exception {
        final SignedCertificate served = generateSignedCertificate();
        server = new TlsTestServer(served.toServerKeyStore(), KEYSTORE_PASSWORD);

        final DatanodeKeystore datanodeKeystore = datanodeKeystoreWithSignedCertificate(tempDir, served);
        final CertificateReloadVerifier verifier = newVerifier(datanodeKeystore);

        verifier.start(httpBaseUrlOf(server.port()));

        assertThat(verifier.awaitCompletion(AWAIT_COMPLETION_TIMEOUT)).isTrue();
        assertThat(notificationsReceiver.getNotifications()).isEmpty();
    }

    @Test
    void escalatesWhenLiveCertificateNeverMatchesExpectation(@TempDir Path tempDir) throws Exception {
        final SignedCertificate served = generateSignedCertificate();
        server = new TlsTestServer(served.toServerKeyStore(), KEYSTORE_PASSWORD);

        // datanode keystore expects a *different* signed certificate than what the server actually presents
        final SignedCertificate expected = generateSignedCertificate();
        final DatanodeKeystore datanodeKeystore = datanodeKeystoreWithSignedCertificate(tempDir, expected);
        final CertificateReloadVerifier verifier = newVerifier(datanodeKeystore);

        verifier.start(httpBaseUrlOf(server.port()));

        assertThat(verifier.awaitCompletion(AWAIT_COMPLETION_TIMEOUT)).isTrue();
        // it escalated exactly once (not repeatedly) and any notification it sent already arrived by now
        assertThat(notificationsReceiver.getNotifications())
                .hasSize(1)
                .anySatisfy(notification -> assertThat(notification.notificationType()).isEqualTo(Notification.Type.CERTIFICATE_NEEDS_RENEWAL));
    }

    @Test
    void escalatesWhenOpensearchHttpListenerIsNeverReachable(@TempDir Path tempDir) throws Exception {
        final SignedCertificate expected = generateSignedCertificate();
        final DatanodeKeystore datanodeKeystore = datanodeKeystoreWithSignedCertificate(tempDir, expected);
        final CertificateReloadVerifier verifier = newVerifier(datanodeKeystore);

        verifier.start(Optional::empty);

        assertThat(verifier.awaitCompletion(AWAIT_COMPLETION_TIMEOUT)).isTrue();
        assertThat(notificationsReceiver.getNotifications()).hasSize(1);
    }

    @Test
    void doesNothingWhenKeystoreHasNoSignedCertificateYet(@TempDir Path tempDir) throws Exception {
        final DatanodeKeystore datanodeKeystore = new DatanodeKeystore(DatanodeTestUtils.tempDirectories(tempDir), "secret", new EventBus());
        datanodeKeystore.initWithSelfSignedCertificate();

        final Supplier<Optional<URI>> shouldNeverBeCalled = () -> {
            throw new AssertionError("httpBaseUrlSupplier should never be consulted without an expected certificate");
        };
        final CertificateReloadVerifier verifier = newVerifier(datanodeKeystore);

        verifier.start(shouldNeverBeCalled);

        assertThat(verifier.awaitCompletion(AWAIT_COMPLETION_TIMEOUT)).isTrue();
        assertThat(notificationsReceiver.getNotifications()).isEmpty();
    }

    private static Supplier<Optional<URI>> httpBaseUrlOf(int port) {
        final URI uri = URI.create("https://127.0.0.1:" + port);
        return () -> Optional.of(uri);
    }

    /**
     * Builds a datanode keystore holding the given CA-signed certificate (and its matching private key) - i.e. the
     * exact state {@link DatanodeKeystore} is in right after a real certificate renewal completed.
     */
    private static DatanodeKeystore datanodeKeystoreWithSignedCertificate(Path tempDir, SignedCertificate signedCertificate) throws Exception {
        final DatanodeKeystore datanodeKeystore = new DatanodeKeystore(DatanodeTestUtils.tempDirectories(tempDir), "secret", new EventBus());
        datanodeKeystore.create(signedCertificate.keyPair());
        datanodeKeystore.replaceCertificatesInKeystore(signedCertificate.toCertificateChain());
        return datanodeKeystore;
    }

    private static SignedCertificate generateSignedCertificate() throws Exception {
        final KeyPair keyPair = DatanodeTestUtils.generateKeyPair(Duration.ofDays(30));
        final KeyPair ca = DatanodeTestUtils.generateCAKeyPair(Duration.ofDays(365));

        final InMemoryKeystoreInformation keystoreInformation = new InMemoryKeystoreInformation(
                keyPair.toKeystore(DatanodeKeystore.DATANODE_KEY_ALIAS, KEYSTORE_PASSWORD), KEYSTORE_PASSWORD);
        final PKCS10CertificationRequest csr = CsrGenerator.generateCSR(keystoreInformation, DatanodeKeystore.DATANODE_KEY_ALIAS, "test-host", List.of());

        final X509Certificate signedCertificate = new CsrSigner().sign(ca.privateKey(), ca.certificate(), csr, 30);
        return new SignedCertificate(keyPair, signedCertificate, ca.certificate());
    }

    /**
     * A CA-signed leaf certificate together with the private key it belongs to - everything needed to either put
     * it into a {@link DatanodeKeystore} or present it from a {@link TlsTestServer}.
     */
    private record SignedCertificate(KeyPair keyPair, X509Certificate leafCertificate, X509Certificate caCertificate) {

        CertificateChain toCertificateChain() {
            return new CertificateChain(leafCertificate, List.of(caCertificate));
        }

        KeyStore toServerKeyStore() throws Exception {
            final KeyStore keyStore = KeyStore.getInstance("PKCS12");
            keyStore.load(null, null);
            keyStore.setKeyEntry("server", keyPair.privateKey(), KEYSTORE_PASSWORD, new Certificate[]{leafCertificate, caCertificate});
            return keyStore;
        }
    }
}
