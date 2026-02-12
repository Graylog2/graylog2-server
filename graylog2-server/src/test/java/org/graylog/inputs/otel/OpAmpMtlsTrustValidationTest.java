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
package org.graylog.inputs.otel;

import io.netty.buffer.UnpooledByteBufAllocator;
import io.netty.handler.ssl.ClientAuth;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.SslProvider;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import org.bouncycastle.asn1.x509.KeyPurposeId;
import org.bouncycastle.asn1.x509.KeyUsage;
import org.graylog.security.pki.Algorithm;
import org.graylog.security.pki.CertificateBuilder;
import org.graylog.security.pki.CertificateEntry;
import org.graylog.security.pki.PemUtils;
import org.graylog2.security.encryption.EncryptedValueService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.Socket;
import java.nio.ByteBuffer;
import java.security.Principal;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.time.Duration;

import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLEngineResult;
import javax.net.ssl.SSLException;
import javax.net.ssl.X509ExtendedKeyManager;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests that the OpAMP CA hierarchy produces Ed25519 certificates whose trust chain
 * can be validated by JDK's PKIX trust manager during a real TLS handshake.
 * <p>
 * Unlike {@link org.graylog.collectors.input.CollectorIngestMtlsIT} (which uses {@code InsecureTrustManagerFactory} and
 * focuses on mTLS handshake mechanics), this test exercises <b>actual PKIX chain
 * validation</b>: the server's trust manager is configured with only the OpAMP CA
 * (intermediate) certificate, and it must validate the agent's end-entity certificate.
 * <p>
 * The handshake is performed entirely in-memory using {@link SSLEngine#wrap} /
 * {@link SSLEngine#unwrap} — no TCP sockets or network I/O.
 * <p>
 * The client side uses a custom {@link X509ExtendedKeyManager} to bypass JDK's PKCS12
 * keystore, which cannot validate Ed25519 certificate chains. This is consistent with
 * how real OpAMP agents present their certificates (they don't go through PKCS12 either).
 */
class OpAmpMtlsTrustValidationTest {

    private static final Duration CERT_VALIDITY = Duration.ofDays(1);

    private CertificateBuilder builder;
    private EncryptedValueService encryptedValueService;

    @BeforeEach
    void setUp() {
        encryptedValueService = new EncryptedValueService("1234567890abcdef");
        builder = new CertificateBuilder(encryptedValueService, "Graylog");
    }

    /**
     * Replicates the production CA hierarchy (Root CA → OpAMP CA → agent/server certs)
     * and verifies the server's PKIX trust manager accepts the agent certificate.
     * <p>
     * This is the exact same trust configuration as {@code OpAmpCaService.newServerSslContextBuilder()}:
     * the server trusts only the OpAMP CA (intermediate), and the client presents only its
     * end-entity agent certificate.
     */
    @Test
    void serverAcceptsAgentCertSignedByIntermediateOpAmpCa() throws Exception {
        final CertificateEntry rootCa = builder.createRootCa("Test Root CA", Algorithm.ED25519, CERT_VALIDITY);
        final CertificateEntry opAmpCa = builder.createIntermediateCa("Test OpAMP CA", rootCa, CERT_VALIDITY);
        final CertificateEntry serverCertEntry = builder.createEndEntityCert("Test Server", opAmpCa,
                KeyUsage.digitalSignature | KeyUsage.keyEncipherment, KeyPurposeId.id_kp_serverAuth, CERT_VALIDITY);
        final CertificateEntry agentCertEntry = builder.createEndEntityCert("test-agent-uid", opAmpCa,
                KeyUsage.digitalSignature, KeyPurposeId.id_kp_clientAuth, CERT_VALIDITY);

        // Round-trip through PEM → PemUtils.parse (same path as production: MongoDB → PemUtils)
        final PrivateKey serverKey = PemUtils.parsePrivateKey(decrypt(serverCertEntry));
        final X509Certificate serverCert = PemUtils.parseCertificate(serverCertEntry.certificate());
        final X509Certificate opAmpCaCert = PemUtils.parseCertificate(opAmpCa.certificate());
        final PrivateKey agentKey = PemUtils.parsePrivateKey(decrypt(agentCertEntry));
        final X509Certificate agentCert = PemUtils.parseCertificate(agentCertEntry.certificate());

        // Server: Netty SslContext — same as OpAmpCaService.newServerSslContextBuilder()
        final SslContext serverSsl = SslContextBuilder.forServer(serverKey, serverCert)
                .sslProvider(SslProvider.JDK)
                .clientAuth(ClientAuth.REQUIRE)
                .trustManager(opAmpCaCert)
                .build();

        // Client: JDK SSLContext with custom KeyManager (bypasses PKCS12 Ed25519 limitation)
        final SSLContext clientSslCtx = SSLContext.getInstance("TLS");
        clientSslCtx.init(
                new KeyManager[]{new FixedKeyManager(agentKey, new X509Certificate[]{agentCert})},
                InsecureTrustManagerFactory.INSTANCE.getTrustManagers(),
                null);

        // In-memory TLS handshake — no network
        final SSLEngine serverEngine = serverSsl.newEngine(UnpooledByteBufAllocator.DEFAULT);
        final SSLEngine clientEngine = clientSslCtx.createSSLEngine("localhost", 4318);
        clientEngine.setUseClientMode(true);

        performHandshake(clientEngine, serverEngine);

        // Verify the server can see the agent's identity from the validated client cert
        final X509Certificate peerCert = (X509Certificate) serverEngine.getSession().getPeerCertificates()[0];
        assertThat(PemUtils.extractCn(peerCert)).isEqualTo("test-agent-uid");
    }

    /**
     * Same as above but the client also sends the intermediate CA cert in its chain,
     * which some TLS implementations require for path building.
     */
    @Test
    void serverAcceptsAgentCertWithIntermediateInChain() throws Exception {
        final CertificateEntry rootCa = builder.createRootCa("Test Root CA", Algorithm.ED25519, CERT_VALIDITY);
        final CertificateEntry opAmpCa = builder.createIntermediateCa("Test OpAMP CA", rootCa, CERT_VALIDITY);
        final CertificateEntry serverCertEntry = builder.createEndEntityCert("Test Server", opAmpCa,
                KeyUsage.digitalSignature | KeyUsage.keyEncipherment, KeyPurposeId.id_kp_serverAuth, CERT_VALIDITY);
        final CertificateEntry agentCertEntry = builder.createEndEntityCert("test-agent-uid", opAmpCa,
                KeyUsage.digitalSignature, KeyPurposeId.id_kp_clientAuth, CERT_VALIDITY);

        final PrivateKey serverKey = PemUtils.parsePrivateKey(decrypt(serverCertEntry));
        final X509Certificate serverCert = PemUtils.parseCertificate(serverCertEntry.certificate());
        final X509Certificate opAmpCaCert = PemUtils.parseCertificate(opAmpCa.certificate());
        final PrivateKey agentKey = PemUtils.parsePrivateKey(decrypt(agentCertEntry));
        final X509Certificate agentCert = PemUtils.parseCertificate(agentCertEntry.certificate());

        final SslContext serverSsl = SslContextBuilder.forServer(serverKey, serverCert)
                .sslProvider(SslProvider.JDK)
                .clientAuth(ClientAuth.REQUIRE)
                .trustManager(opAmpCaCert)
                .build();

        // Client sends end-entity cert + intermediate CA cert in chain
        final SSLContext clientSslCtx = SSLContext.getInstance("TLS");
        clientSslCtx.init(
                new KeyManager[]{new FixedKeyManager(agentKey, new X509Certificate[]{agentCert, opAmpCaCert})},
                InsecureTrustManagerFactory.INSTANCE.getTrustManagers(),
                null);

        final SSLEngine serverEngine = serverSsl.newEngine(UnpooledByteBufAllocator.DEFAULT);
        final SSLEngine clientEngine = clientSslCtx.createSSLEngine("localhost", 4318);
        clientEngine.setUseClientMode(true);

        performHandshake(clientEngine, serverEngine);

        final X509Certificate peerCert = (X509Certificate) serverEngine.getSession().getPeerCertificates()[0];
        assertThat(PemUtils.extractCn(peerCert)).isEqualTo("test-agent-uid");
    }

    /**
     * Verifies that a self-signed agent cert (not signed by the OpAMP CA) is rejected.
     */
    @Test
    void serverRejectsAgentCertNotSignedByOpAmpCa() throws Exception {
        final CertificateEntry rootCa = builder.createRootCa("Test Root CA", Algorithm.ED25519, CERT_VALIDITY);
        final CertificateEntry opAmpCa = builder.createIntermediateCa("Test OpAMP CA", rootCa, CERT_VALIDITY);
        final CertificateEntry serverCertEntry = builder.createEndEntityCert("Test Server", opAmpCa,
                KeyUsage.digitalSignature | KeyUsage.keyEncipherment, KeyPurposeId.id_kp_serverAuth, CERT_VALIDITY);

        // Create agent cert signed by a DIFFERENT CA (not the OpAMP CA)
        final CertificateEntry rogueRootCa = builder.createRootCa("Rogue CA", Algorithm.ED25519, CERT_VALIDITY);
        final CertificateEntry rogueCertEntry = builder.createEndEntityCert("rogue-agent", rogueRootCa,
                KeyUsage.digitalSignature, KeyPurposeId.id_kp_clientAuth, CERT_VALIDITY);

        final PrivateKey serverKey = PemUtils.parsePrivateKey(decrypt(serverCertEntry));
        final X509Certificate serverCert = PemUtils.parseCertificate(serverCertEntry.certificate());
        final X509Certificate opAmpCaCert = PemUtils.parseCertificate(opAmpCa.certificate());
        final PrivateKey rogueKey = PemUtils.parsePrivateKey(decrypt(rogueCertEntry));
        final X509Certificate rogueCert = PemUtils.parseCertificate(rogueCertEntry.certificate());

        final SslContext serverSsl = SslContextBuilder.forServer(serverKey, serverCert)
                .sslProvider(SslProvider.JDK)
                .clientAuth(ClientAuth.REQUIRE)
                .trustManager(opAmpCaCert)
                .build();

        final SSLContext clientSslCtx = SSLContext.getInstance("TLS");
        clientSslCtx.init(
                new KeyManager[]{new FixedKeyManager(rogueKey, new X509Certificate[]{rogueCert})},
                InsecureTrustManagerFactory.INSTANCE.getTrustManagers(),
                null);

        final SSLEngine serverEngine = serverSsl.newEngine(UnpooledByteBufAllocator.DEFAULT);
        final SSLEngine clientEngine = clientSslCtx.createSSLEngine("localhost", 4318);
        clientEngine.setUseClientMode(true);

        assertThatThrownBy(() -> performHandshake(clientEngine, serverEngine))
                .isInstanceOf(SSLException.class);
    }

    /**
     * Verifies the same trust validation works with RSA certificates.
     */
    @Test
    void serverAcceptsRsaAgentCertSignedByIntermediateCa() throws Exception {
        final CertificateEntry rootCa = builder.createRootCa("Test Root CA", Algorithm.RSA_4096, CERT_VALIDITY);
        final CertificateEntry opAmpCa = builder.createIntermediateCa("Test OpAMP CA", rootCa, CERT_VALIDITY);
        final CertificateEntry serverCertEntry = builder.createEndEntityCert("Test Server", opAmpCa,
                KeyUsage.digitalSignature | KeyUsage.keyEncipherment, KeyPurposeId.id_kp_serverAuth, CERT_VALIDITY);
        final CertificateEntry agentCertEntry = builder.createEndEntityCert("test-agent-uid", opAmpCa,
                KeyUsage.digitalSignature, KeyPurposeId.id_kp_clientAuth, CERT_VALIDITY);

        final PrivateKey serverKey = PemUtils.parsePrivateKey(decrypt(serverCertEntry));
        final X509Certificate serverCert = PemUtils.parseCertificate(serverCertEntry.certificate());
        final X509Certificate opAmpCaCert = PemUtils.parseCertificate(opAmpCa.certificate());
        final PrivateKey agentKey = PemUtils.parsePrivateKey(decrypt(agentCertEntry));
        final X509Certificate agentCert = PemUtils.parseCertificate(agentCertEntry.certificate());

        final SslContext serverSsl = SslContextBuilder.forServer(serverKey, serverCert)
                .sslProvider(SslProvider.JDK)
                .clientAuth(ClientAuth.REQUIRE)
                .trustManager(opAmpCaCert)
                .build();

        final SSLContext clientSslCtx = SSLContext.getInstance("TLS");
        clientSslCtx.init(
                new KeyManager[]{new FixedKeyManager(agentKey, new X509Certificate[]{agentCert})},
                InsecureTrustManagerFactory.INSTANCE.getTrustManagers(),
                null);

        final SSLEngine serverEngine = serverSsl.newEngine(UnpooledByteBufAllocator.DEFAULT);
        final SSLEngine clientEngine = clientSslCtx.createSSLEngine("localhost", 4318);
        clientEngine.setUseClientMode(true);

        performHandshake(clientEngine, serverEngine);

        final X509Certificate peerCert = (X509Certificate) serverEngine.getSession().getPeerCertificates()[0];
        assertThat(PemUtils.extractCn(peerCert)).isEqualTo("test-agent-uid");
    }

    // ----- Helpers -----

    private String decrypt(CertificateEntry entry) {
        return encryptedValueService.decrypt(entry.privateKey());
    }

    /**
     * Performs a TLS handshake between two SSLEngines entirely in-memory.
     * Shuttles ByteBuffers between client and server until both report NOT_HANDSHAKING.
     *
     * @throws SSLException if the handshake fails (e.g., certificate validation error)
     */
    private static void performHandshake(SSLEngine client, SSLEngine server) throws Exception {
        final int netBufSize = Math.max(
                client.getSession().getPacketBufferSize(),
                server.getSession().getPacketBufferSize());

        final ByteBuffer cToS = ByteBuffer.allocate(netBufSize);
        final ByteBuffer sToC = ByteBuffer.allocate(netBufSize);
        final ByteBuffer appBuf = ByteBuffer.allocate(netBufSize);
        final ByteBuffer empty = ByteBuffer.allocate(0);

        cToS.flip();
        sToC.flip();

        client.beginHandshake();
        server.beginHandshake();

        for (int i = 0; i < 100; i++) {
            final boolean c = stepEngine(client, sToC, cToS, appBuf, empty);
            final boolean s = stepEngine(server, cToS, sToC, appBuf, empty);

            if (client.getHandshakeStatus() == SSLEngineResult.HandshakeStatus.NOT_HANDSHAKING &&
                    server.getHandshakeStatus() == SSLEngineResult.HandshakeStatus.NOT_HANDSHAKING) {
                return;
            }

            if (!c && !s) {
                throw new SSLException("Handshake stalled: client=" + client.getHandshakeStatus() +
                        ", server=" + server.getHandshakeStatus());
            }
        }
        throw new SSLException("Handshake did not complete within 100 iterations");
    }

    private static boolean stepEngine(SSLEngine engine, ByteBuffer in, ByteBuffer out,
                                      ByteBuffer app, ByteBuffer empty) throws SSLException {
        boolean progress = false;

        if (engine.getHandshakeStatus() == SSLEngineResult.HandshakeStatus.NEED_TASK) {
            Runnable task;
            while ((task = engine.getDelegatedTask()) != null) {
                task.run();
            }
            progress = true;
        }

        if (engine.getHandshakeStatus() == SSLEngineResult.HandshakeStatus.NEED_WRAP) {
            out.compact();
            final SSLEngineResult result = engine.wrap(empty, out);
            out.flip();
            if (result.bytesProduced() > 0) {
                progress = true;
            }
        }

        if (engine.getHandshakeStatus() == SSLEngineResult.HandshakeStatus.NEED_UNWRAP && in.hasRemaining()) {
            app.clear();
            final SSLEngineResult result = engine.unwrap(in, app);
            if (result.bytesConsumed() > 0) {
                progress = true;
            }
        }

        if (engine.getHandshakeStatus() == SSLEngineResult.HandshakeStatus.NEED_TASK) {
            Runnable task;
            while ((task = engine.getDelegatedTask()) != null) {
                task.run();
            }
            progress = true;
        }

        return progress;
    }

    /**
     * A simple KeyManager that always returns a fixed certificate chain and private key.
     * Bypasses JDK's PKCS12 keystore which cannot validate Ed25519 certificate chains.
     */
    private static class FixedKeyManager extends X509ExtendedKeyManager {
        private final PrivateKey privateKey;
        private final X509Certificate[] certChain;

        FixedKeyManager(PrivateKey privateKey, X509Certificate[] certChain) {
            this.privateKey = privateKey;
            this.certChain = certChain;
        }

        @Override
        public String[] getClientAliases(String keyType, Principal[] issuers) {
            return new String[]{"agent"};
        }

        @Override
        public String chooseClientAlias(String[] keyType, Principal[] issuers, Socket socket) {
            return "agent";
        }

        @Override
        public String chooseEngineClientAlias(String[] keyType, Principal[] issuers, SSLEngine engine) {
            return "agent";
        }

        @Override
        public String[] getServerAliases(String keyType, Principal[] issuers) {
            return null;
        }

        @Override
        public String chooseServerAlias(String keyType, Principal[] issuers, Socket socket) {
            return null;
        }

        @Override
        public X509Certificate[] getCertificateChain(String alias) {
            return certChain;
        }

        @Override
        public PrivateKey getPrivateKey(String alias) {
            return privateKey;
        }
    }
}
