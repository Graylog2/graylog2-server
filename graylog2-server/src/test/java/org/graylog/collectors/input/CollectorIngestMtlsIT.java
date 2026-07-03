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
package org.graylog.collectors.input;

import com.google.common.eventbus.EventBus;
import com.google.common.util.concurrent.MoreExecutors;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.MultiThreadIoEventLoopGroup;
import io.netty.channel.nio.NioIoHandler;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.ssl.SslContext;
import io.opentelemetry.proto.collector.logs.v1.ExportLogsServiceRequest;
import io.opentelemetry.proto.common.v1.AnyValue;
import io.opentelemetry.proto.logs.v1.LogRecord;
import io.opentelemetry.proto.logs.v1.ResourceLogs;
import io.opentelemetry.proto.logs.v1.ScopeLogs;
import org.bouncycastle.asn1.x509.KeyPurposeId;
import org.bouncycastle.asn1.x509.KeyUsage;
import org.graylog.collectors.CollectorCaCache;
import org.graylog.collectors.CollectorCaKeyManager;
import org.graylog.collectors.CollectorCaService;
import org.graylog.collectors.CollectorCaTrustManager;
import org.graylog.collectors.CollectorFingerprintCache;
import org.graylog.collectors.CollectorInstanceService;
import org.graylog.collectors.CollectorJournal;
import org.graylog.collectors.CollectorTLSUtils;
import org.graylog.collectors.CollectorsConfig;
import org.graylog.collectors.CollectorsConfigService;
import org.graylog.collectors.input.transport.AgentCertChannelHandler;
import org.graylog.collectors.input.transport.CollectorIngestHttpHandler;
import org.graylog.collectors.opamp.IssuedCertificate;
import org.graylog.security.pki.Algorithm;
import org.graylog.security.pki.CertificateBuilder;
import org.graylog.security.pki.CertificateEntry;
import org.graylog.security.pki.CertificateService;
import org.graylog.security.pki.PemUtils;
import org.graylog.testing.cluster.ClusterConfigServiceExtension;
import org.graylog.testing.mongodb.MongoDBExtension;
import org.graylog2.configuration.HttpConfiguration;
import org.graylog2.database.MongoCollections;
import org.graylog2.events.ClusterEventBus;
import org.graylog2.plugin.cluster.ClusterConfigService;
import org.graylog2.plugin.cluster.ClusterIdService;
import org.graylog2.plugin.inputs.MessageInput;
import org.graylog2.plugin.journal.RawMessage;
import org.graylog2.security.encryption.EncryptedValueService;
import org.graylog2.web.customization.CustomizationConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.threeten.extra.MutableClock;

import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLHandshakeException;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509ExtendedKeyManager;
import javax.net.ssl.X509ExtendedTrustManager;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.Principal;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.time.Clock;
import java.time.Duration;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * End-to-end integration test for the collector ingest mTLS path against the real production stack.
 * <p>
 * It initializes the collectors CA hierarchy, enrolls an agent, and wires the real
 * {@link CollectorCaKeyManager}, {@link CollectorCaTrustManager} (including its active-instance
 * binding), {@link CollectorFingerprintCache}, and {@link CollectorIngestHttpHandler} into a Netty
 * server. Client certificates are minted with the production {@link CertificateBuilder}
 * (clientAuth EKU, signed by the real signing cert), so they are cryptographically valid and only
 * the instance binding decides whether a handshake is trusted.
 * <p>
 * It verifies the security fix for the ingest mTLS path: a certificate is trusted only when it binds
 * to an active, non-deleted collector instance — not merely because it was signed by the CA. It also
 * covers the renewal model (the {@code next} certificate is accepted before activation; the superseded
 * certificate stays accepted for a grace window after activation, then loses access) and that a
 * foreign-CA certificate is rejected at the crypto gate. A trusted connection propagates the resolved
 * instance UID to the journal record.
 * <p>
 * The test client uses an {@link X509ExtendedTrustManager} that accepts any server certificate, which
 * also bypasses the JDK's hostname-verification wrapper — so the real OTLP server certificate is used
 * as-is without needing an IP SAN for {@code 127.0.0.1}.
 */
@ExtendWith(MongoDBExtension.class)
@ExtendWith(ClusterConfigServiceExtension.class)
class CollectorIngestMtlsIT {

    private static final String AGENT_INSTANCE_UID = "test-agent-42";
    private static final String UNENROLLED_INSTANCE_UID = "unenrolled-agent-99";
    private static final Duration CERT_VALIDITY = Duration.ofDays(1);

    private final EncryptedValueService encryptedValueService = new EncryptedValueService("1234567890abcdef");

    private CertificateBuilder certBuilder;
    private CertificateEntry signingCertEntry;
    private X509Certificate signingCert;

    // Enrolled agent: its certificate binds to an active instance.
    private PrivateKey agentKey;
    private X509Certificate agentCert;

    private MutableClock clock;
    private CollectorInstanceService instanceService;
    private CollectorFingerprintCache fingerprintCache;
    private CollectorTLSUtils tlsUtils;
    private CollectorCaCache caCache;
    private MessageInput input;

    private Channel serverChannel;
    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;

    @BeforeEach
    void setUp(MongoCollections mongoCollections, ClusterConfigService clusterConfigService) throws Exception {
        // The instance service and the fingerprint cache share a MutableClock so the renewal grace window
        // can be advanced deterministically; certificate crypto validity uses the real clock.
        clock = MutableClock.epochUTC();
        certBuilder = new CertificateBuilder(encryptedValueService, "Test", Clock.systemUTC());
        final var certService = new CertificateService(mongoCollections, encryptedValueService, CustomizationConfig.empty(), Clock.systemUTC());

        final var clusterIdService = mock(ClusterIdService.class);
        when(clusterIdService.getString()).thenReturn(UUID.randomUUID().toString());
        final var httpConfiguration = mock(HttpConfiguration.class);
        when(httpConfiguration.getHttpExternalUri()).thenReturn(URI.create("https://localhost:443/"));

        final var collectorsConfigService = new CollectorsConfigService(clusterConfigService, new ClusterEventBus(), httpConfiguration);
        final var caService = new CollectorCaService(certService, clusterIdService, collectorsConfigService, Clock.systemUTC());

        final var hierarchy = caService.initializeCa();
        collectorsConfigService.save(CollectorsConfig.createDefaultBuilder("localhost")
                .caCertId(hierarchy.caCert().id())
                .signingCertId(hierarchy.signingCert().id())
                .otlpServerCertId(hierarchy.otlpServerCert().id())
                .build());
        signingCertEntry = hierarchy.signingCert();
        signingCert = PemUtils.parseCertificate(signingCertEntry.certificate());

        // Mint and enroll the agent so its (active) fingerprint resolves to an active instance.
        final AgentCert agent = mintClientCert(AGENT_INSTANCE_UID, signingCertEntry);
        agentKey = agent.key();
        agentCert = agent.cert();

        instanceService = new CollectorInstanceService(mongoCollections, new ClusterEventBus(), clock);
        instanceService.enroll(AGENT_INSTANCE_UID, "000000000000000000000000",
                new IssuedCertificate(agent.entry().fingerprint(), agent.entry().certificate(),
                        agent.entry().notAfter(), signingCertEntry.id()),
                "000000000000000000000000");

        fingerprintCache = new CollectorFingerprintCache(collectorsConfigService, instanceService,
                new EventBus(), clock, MoreExecutors.directExecutor());

        caCache = new CollectorCaCache(caService, certService, encryptedValueService, new EventBus(), Clock.systemUTC());
        caCache.startAsync().awaitRunning();
        final var keyManager = new CollectorCaKeyManager(caCache);
        final var trustManager = new CollectorCaTrustManager(caCache, fingerprintCache, Clock.systemUTC());
        tlsUtils = new CollectorTLSUtils(keyManager, trustManager, MoreExecutors.directExecutor());

        input = mock(MessageInput.class);

        bossGroup = new MultiThreadIoEventLoopGroup(1, NioIoHandler.newFactory());
        workerGroup = new MultiThreadIoEventLoopGroup(2, NioIoHandler.newFactory());
    }

    @AfterEach
    void tearDown() throws Exception {
        caCache.stopAsync().awaitTerminated();
        if (serverChannel != null) {
            serverChannel.close().sync();
        }
        bossGroup.shutdownGracefully().sync();
        workerGroup.shutdownGracefully().sync();
    }

    @Test
    void enrolledAgentCompletesMtlsAndPropagatesInstanceUidToJournal() throws Exception {
        final int port = startServer();
        final HttpClient client = createMtlsClient(agentKey, agentCert);

        final HttpResponse<byte[]> response = postLogs(client, port);

        assertThat(response.statusCode()).isEqualTo(200);

        // The handshake completion event must fire before the HTTP request so AgentCertChannelHandler
        // can set the fingerprint attribute; reaching the journal with the right UID proves both the
        // ordering and the end-to-end binding fingerprint -> active instance UID.
        assertThat(capturedJournalRecord().getCollectorInstanceUid()).isEqualTo(AGENT_INSTANCE_UID);
    }

    @Test
    void rejectsConnectionWithoutClientCert() throws Exception {
        final int port = startServer();

        final SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(null, new TrustManager[]{new TrustAllManager()}, null);
        final HttpClient client = HttpClient.newBuilder().sslContext(sslContext).build();

        // The server requires client auth, so the handshake fails before any HTTP request is processed.
        assertThatThrownBy(() -> postLogs(client, port))
                .hasCauseInstanceOf(SSLHandshakeException.class);
    }

    @Test
    void rejectsCaSignedCertOfUnenrolledInstance() throws Exception {
        // The cert is signed by the CA and otherwise valid, but its instance was never enrolled, so the
        // trust manager's binding lookup finds no active instance and aborts the handshake. This is the
        // core of the fix: a CA signature alone is not enough to be trusted.
        final AgentCert unenrolled = mintClientCert(UNENROLLED_INSTANCE_UID, signingCertEntry);

        final int port = startServer();
        final HttpClient client = createMtlsClient(unenrolled.key(), unenrolled.cert());

        assertThatThrownBy(() -> postLogs(client, port))
                .hasCauseInstanceOf(SSLException.class);
    }

    @Test
    void rejectsCertOfDeletedInstance() throws Exception {
        // Delete the enrolled instance before the first handshake: its fingerprint was never cached, so
        // the binding lookup resolves straight from MongoDB and finds nothing — the cert is no longer
        // trusted. This is the revocation half of the fix (a removed instance loses ingest access).
        assertThat(instanceService.deleteByInstanceUid(AGENT_INSTANCE_UID)).isTrue();

        final int port = startServer();
        final HttpClient client = createMtlsClient(agentKey, agentCert);

        assertThatThrownBy(() -> postLogs(client, port))
                .hasCauseInstanceOf(SSLException.class);
    }

    @Test
    void acceptsNextCertificateDuringRenewal() throws Exception {
        // During renewal the agent may present its freshly issued "next" certificate before it is
        // activated. resolveCertBinding resolves the next fingerprint to the same instance, so the
        // handshake is trusted and the same instance UID reaches the journal.
        final AgentCert renewed = mintClientCert(AGENT_INSTANCE_UID, signingCertEntry);
        assertThat(instanceService.insertNextCertificate(AGENT_INSTANCE_UID, renewed.entry().fingerprint(),
                renewed.entry().certificate(), renewed.entry().notAfter())).isTrue();

        final int port = startServer();
        final HttpClient client = createMtlsClient(renewed.key(), renewed.cert());

        final HttpResponse<byte[]> response = postLogs(client, port);

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(capturedJournalRecord().getCollectorInstanceUid()).isEqualTo(AGENT_INSTANCE_UID);
    }

    @Test
    void acceptsSupersededCertificateWithinRenewalGraceWindow() throws Exception {
        // After activation the old certificate is demoted to the previous slot and stays accepted for the
        // grace window, so the collector's in-flight ingest connection isn't cut before its exporter has
        // switched to the new certificate.
        activateRenewedCertificate();

        final int port = startServer();
        final HttpClient client = createMtlsClient(agentKey, agentCert); // the now-superseded cert

        final HttpResponse<byte[]> response = postLogs(client, port);

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(capturedJournalRecord().getCollectorInstanceUid()).isEqualTo(AGENT_INSTANCE_UID);
    }

    @Test
    void rejectsSupersededCertificateAfterGraceWindowElapses() throws Exception {
        // Once the grace window has elapsed, the superseded certificate's binding is expired and a new
        // connection is rejected at the handshake — renewal rotates ingest access off the old cert.
        activateRenewedCertificate();
        clock.add(Duration.ofHours(1)); // well past the grace window

        final int port = startServer();
        final HttpClient client = createMtlsClient(agentKey, agentCert);

        assertThatThrownBy(() -> postLogs(client, port))
                .hasCauseInstanceOf(SSLException.class);
    }

    @Test
    void cutsEstablishedConnectionWhenGraceWindowElapses() throws Exception {
        // TLS validates the client certificate only at the handshake — an established connection is never
        // re-checked by the TLS layer. So a connection opened within the grace window must be cut by the
        // per-request binding check once the grace deadline passes: same connection (the HttpClient reuses
        // its pooled keep-alive connection, no new handshake), but now 401 instead of 200.
        activateRenewedCertificate();

        final int port = startServer();
        final HttpClient client = createMtlsClient(agentKey, agentCert); // the now-superseded cert

        assertThat(postLogs(client, port).statusCode()).isEqualTo(200); // within grace

        clock.add(Duration.ofMinutes(10)); // past the grace window, well within the cache's idle expiry

        assertThat(postLogs(client, port).statusCode()).isEqualTo(401);
    }

    /** Stages a fresh next certificate for the enrolled agent and activates it, demoting the old active cert. */
    private void activateRenewedCertificate() throws Exception {
        final AgentCert renewed = mintClientCert(AGENT_INSTANCE_UID, signingCertEntry);
        instanceService.insertNextCertificate(AGENT_INSTANCE_UID, renewed.entry().fingerprint(),
                renewed.entry().certificate(), renewed.entry().notAfter());
        final var instance = instanceService.findByInstanceUid(AGENT_INSTANCE_UID).orElseThrow();
        assertThat(instanceService.activateNextCertificate(instance)).isTrue();
    }

    @Test
    void rejectsCertSignedByForeignCa() throws Exception {
        // An attacker who knows the instance UID but signs with their own CA: the cert carries the right
        // CN but does not chain to the collectors root, so the trust manager rejects it at the crypto
        // gate before the binding is even consulted.
        final CertificateEntry foreignCa = certBuilder.createRootCa("Foreign CA", Algorithm.ED25519, CERT_VALIDITY);
        final AgentCert foreign = mintClientCert(AGENT_INSTANCE_UID, foreignCa);
        final X509Certificate foreignCaCert = PemUtils.parseCertificate(foreignCa.certificate());

        final int port = startServer();
        final HttpClient client = createMtlsClient(foreign.key(), foreign.cert(), foreignCaCert);

        assertThatThrownBy(() -> postLogs(client, port))
                .hasCauseInstanceOf(SSLException.class);
    }

    // ----- Helpers -----

    private int startServer() throws Exception {
        final SslContext sslContext = tlsUtils.newServerSslContextBuilder().build();

        final ServerBootstrap bootstrap = new ServerBootstrap()
                .group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel.class)
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) {
                        final ChannelPipeline pipeline = ch.pipeline();
                        pipeline.addLast("ssl", sslContext.newHandler(ch.alloc()));
                        pipeline.addLast("agent-cert-handler", new AgentCertChannelHandler());
                        pipeline.addLast("http-codec", new HttpServerCodec());
                        pipeline.addLast("http-aggregator", new HttpObjectAggregator(1024 * 1024));
                        pipeline.addLast("http-handler", new CollectorIngestHttpHandler(input, fingerprintCache));
                    }
                });

        serverChannel = bootstrap.bind("127.0.0.1", 0).sync().channel();
        return ((InetSocketAddress) serverChannel.localAddress()).getPort();
    }

    private HttpResponse<byte[]> postLogs(HttpClient client, int port) throws Exception {
        final ExportLogsServiceRequest request = createTestRequest();
        return client.send(
                HttpRequest.newBuilder()
                        .uri(URI.create("https://127.0.0.1:" + port + "/v1/logs"))
                        .header("Content-Type", "application/x-protobuf")
                        .POST(HttpRequest.BodyPublishers.ofByteArray(request.toByteArray()))
                        .build(),
                HttpResponse.BodyHandlers.ofByteArray());
    }

    private CollectorJournal.Record capturedJournalRecord() throws Exception {
        final ArgumentCaptor<RawMessage> captor = ArgumentCaptor.forClass(RawMessage.class);
        verify(input).processRawMessage(captor.capture());
        return CollectorJournal.Record.parseFrom(captor.getValue().getPayload());
    }

    private HttpClient createMtlsClient(PrivateKey clientKey, X509Certificate clientCert) throws Exception {
        return createMtlsClient(clientKey, clientCert, signingCert);
    }

    private HttpClient createMtlsClient(PrivateKey clientKey, X509Certificate clientCert, X509Certificate issuerCert) throws Exception {
        final X509ExtendedKeyManager km = new SimpleKeyManager(clientKey, clientCert, issuerCert);
        final SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(new KeyManager[]{km}, new TrustManager[]{new TrustAllManager()}, null);
        return HttpClient.newBuilder().sslContext(sslContext).build();
    }

    private ExportLogsServiceRequest createTestRequest() {
        return ExportLogsServiceRequest.newBuilder()
                .addResourceLogs(ResourceLogs.newBuilder()
                        .addScopeLogs(ScopeLogs.newBuilder()
                                .addLogRecords(LogRecord.newBuilder()
                                        .setBody(AnyValue.newBuilder().setStringValue("test log message"))
                                        .setTimeUnixNano(System.nanoTime())
                                        .setSeverityText("INFO"))))
                .build();
    }

    /**
     * Mints an Ed25519 client (clientAuth) end-entity certificate with the given CN, signed by the given
     * issuer, using the production {@link CertificateBuilder}.
     */
    private AgentCert mintClientCert(String cn, CertificateEntry issuer) throws Exception {
        final CertificateEntry entry = certBuilder.createEndEntityCert(
                cn, issuer, KeyUsage.digitalSignature, KeyPurposeId.id_kp_clientAuth, CERT_VALIDITY);
        return new AgentCert(
                PemUtils.parsePrivateKey(encryptedValueService.decrypt(entry.privateKey())),
                PemUtils.parseCertificate(entry.certificate()),
                entry);
    }

    private record AgentCert(PrivateKey key, X509Certificate cert, CertificateEntry entry) {}

    /**
     * A simple {@link X509ExtendedKeyManager} that returns a fixed client cert and key. This avoids the
     * PKCS12 keystore chain validation that fails for Ed25519 certs.
     */
    private static class SimpleKeyManager extends X509ExtendedKeyManager {
        private final PrivateKey privateKey;
        private final X509Certificate[] certChain;

        SimpleKeyManager(PrivateKey privateKey, X509Certificate clientCert, X509Certificate issuerCert) {
            this.privateKey = privateKey;
            this.certChain = new X509Certificate[]{clientCert, issuerCert};
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

    /**
     * Trust manager that accepts all server certificates and skips hostname verification.
     * <p>
     * Must extend {@link X509ExtendedTrustManager} (not just {@link javax.net.ssl.X509TrustManager})
     * because the JDK wraps a plain X509TrustManager in AbstractTrustManagerWrapper which adds
     * hostname/IP identity checks; X509ExtendedTrustManager is used directly, bypassing the wrapper.
     */
    private static class TrustAllManager extends X509ExtendedTrustManager {
        @Override
        public void checkClientTrusted(X509Certificate[] chain, String authType) {
        }

        @Override
        public void checkServerTrusted(X509Certificate[] chain, String authType) {
        }

        @Override
        public void checkClientTrusted(X509Certificate[] chain, String authType, Socket socket) {
        }

        @Override
        public void checkServerTrusted(X509Certificate[] chain, String authType, Socket socket) {
        }

        @Override
        public void checkClientTrusted(X509Certificate[] chain, String authType, SSLEngine engine) {
        }

        @Override
        public void checkServerTrusted(X509Certificate[] chain, String authType, SSLEngine engine) {
        }

        @Override
        public X509Certificate[] getAcceptedIssuers() {
            return new X509Certificate[0];
        }
    }
}
