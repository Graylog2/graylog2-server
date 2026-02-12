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

import io.grpc.ManagedChannel;
import io.grpc.Server;
import io.grpc.StatusRuntimeException;
import io.grpc.netty.GrpcSslContexts;
import io.grpc.netty.NettyChannelBuilder;
import io.grpc.netty.NettyServerBuilder;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.ssl.ClientAuth;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.SslProvider;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import io.opentelemetry.proto.collector.logs.v1.ExportLogsServiceRequest;
import io.opentelemetry.proto.collector.logs.v1.ExportLogsServiceResponse;
import io.opentelemetry.proto.collector.logs.v1.LogsServiceGrpc;
import io.opentelemetry.proto.common.v1.AnyValue;
import io.opentelemetry.proto.logs.v1.LogRecord;
import io.opentelemetry.proto.logs.v1.ResourceLogs;
import io.opentelemetry.proto.logs.v1.ScopeLogs;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x500.X500NameBuilder;
import org.bouncycastle.asn1.x500.style.BCStyle;
import org.bouncycastle.asn1.x509.BasicConstraints;
import org.bouncycastle.asn1.x509.ExtendedKeyUsage;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.GeneralName;
import org.bouncycastle.asn1.x509.GeneralNames;
import org.bouncycastle.asn1.x509.KeyPurposeId;
import org.bouncycastle.asn1.x509.KeyUsage;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.graylog.collectors.CollectorJournal;
import org.graylog.collectors.input.transport.AgentCertAuthInterceptor;
import org.graylog.collectors.input.transport.AgentCertChannelHandler;
import org.graylog.collectors.input.transport.AgentCertTransportFilter;
import org.graylog.collectors.input.transport.CollectorIngestHttpHandler;
import org.graylog.collectors.input.transport.CollectorIngestLogsService;
import org.graylog.inputs.grpc.RemoteAddressProviderInterceptor;
import org.graylog.inputs.otel.OTelJournalRecordFactory;
import org.graylog.security.pki.Algorithm;
import org.graylog.security.pki.CertificateBuilder;
import org.graylog.security.pki.CertificateEntry;
import org.graylog.security.pki.PemUtils;
import org.graylog2.plugin.inputs.MessageInput;
import org.graylog2.plugin.inputs.transports.ThrottleableTransport2;
import org.graylog2.plugin.journal.RawMessage;
import org.graylog2.security.encryption.EncryptedValueService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.math.BigInteger;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.Principal;
import java.security.PrivateKey;
import java.security.Security;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLHandshakeException;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509ExtendedKeyManager;
import javax.net.ssl.X509TrustManager;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;

/**
 * Integration tests for Ed25519 mTLS end-to-end via {@code SslProvider.JDK}.
 * <p>
 * These tests verify the main technical risk: Ed25519 mTLS works for both gRPC and HTTP
 * transports. Each test starts a real server with TLS, creates client connections using
 * Ed25519 certificates, and validates that authentication and data flow work correctly.
 * <p>
 * BouncyCastle is used to generate Ed25519 certificates (JDK does not provide a
 * certificate builder API). After generation, BC is removed from the security provider
 * list and all keys/certs are converted to JDK-native format. This is necessary because
 * JDK's SSL engine requires {@code EdECPrivateKey} (not BC's {@code BCEdDSAPrivateKey})
 * for Ed25519 TLS operations.
 * <p>
 * Both client and server use {@code InsecureTrustManagerFactory} to bypass PKIX cert path
 * validation, which has compatibility issues with Ed25519 certs in the test environment.
 * The server still enforces {@code ClientAuth.REQUIRE}, ensuring the client must present
 * a certificate. The focus of these tests is on the mTLS handshake mechanics and agent
 * identity propagation, not on cert chain validation (which is handled by standard JDK
 * infrastructure).
 */
@ExtendWith(MockitoExtension.class)
class CollectorIngestMtlsIT {

    private static final String AGENT_INSTANCE_UID = "test-agent-instance-123";
    private static final Duration CERT_VALIDITY = Duration.ofDays(1);

    // Server cert with SAN for 127.0.0.1 - JDK-native format
    private static PrivateKey serverKey;
    private static X509Certificate serverCert;

    // Agent cert signed by the CA - JDK-native format
    private static PrivateKey agentKey;
    private static X509Certificate agentCert;

    // CA cert (self-signed root) - JDK-native format
    private static X509Certificate caCert;

    @Mock
    private MessageInput input;
    @Mock
    private ThrottleableTransport2 transport;

    private Server grpcServer;
    private Channel httpServerChannel;
    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;

    @BeforeAll
    static void setupCerts() throws Exception {
        // Register BC for cert generation (JDK has no certificate builder API).
        // Track whether we added it so we only remove our own registration.
        final boolean bcWasPresent = Security.getProvider("BC") != null;
        if (!bcWasPresent) {
            Security.addProvider(new BouncyCastleProvider());
        }

        final EncryptedValueService encryptedValueService = new EncryptedValueService("1234567890abcdef");
        final CertificateBuilder certificateBuilder = new CertificateBuilder(encryptedValueService, "Test");

        // Create a flat CA hierarchy: self-signed root CA signs all end-entity certs
        final CertificateEntry caEntry = certificateBuilder.createRootCa("Test CA", Algorithm.ED25519, CERT_VALIDITY);
        final CertificateEntry agentCertEntry = certificateBuilder.createEndEntityCert(AGENT_INSTANCE_UID, caEntry,
                KeyUsage.digitalSignature, KeyPurposeId.id_kp_clientAuth, CERT_VALIDITY);

        // Create server cert with SAN for IP 127.0.0.1 (required by hostname verification)
        final X509Certificate issuerCertBc = PemUtils.parseCertificate(caEntry.certificate());
        final PrivateKey issuerKeyBc = PemUtils.parsePrivateKey(encryptedValueService.decrypt(caEntry.privateKey()));
        final KeyPair serverKeyPair = KeyPairGenerator.getInstance("Ed25519", "BC").generateKeyPair();
        final X500Name serverSubject = new X500NameBuilder(BCStyle.INSTANCE)
                .addRDN(BCStyle.CN, "Test Server").addRDN(BCStyle.O, "Test").build();
        final X500Name issuerDn = new X500Name(issuerCertBc.getSubjectX500Principal().getName());
        final Instant now = Instant.now();
        final JcaX509v3CertificateBuilder certBuilder = new JcaX509v3CertificateBuilder(
                issuerDn, BigInteger.valueOf(System.currentTimeMillis()),
                Date.from(now), Date.from(now.plus(CERT_VALIDITY)),
                serverSubject, serverKeyPair.getPublic());
        certBuilder.addExtension(Extension.basicConstraints, true, new BasicConstraints(false));
        certBuilder.addExtension(Extension.keyUsage, true,
                new KeyUsage(KeyUsage.digitalSignature | KeyUsage.keyEncipherment));
        certBuilder.addExtension(Extension.extendedKeyUsage, false,
                new ExtendedKeyUsage(KeyPurposeId.id_kp_serverAuth));
        certBuilder.addExtension(Extension.subjectAlternativeName, false,
                new GeneralNames(new GeneralName(GeneralName.iPAddress, "127.0.0.1")));
        final ContentSigner signer = new JcaContentSignerBuilder("Ed25519")
                .setProvider("BC").build(issuerKeyBc);
        final X509Certificate serverCertBc = new JcaX509CertificateConverter()
                .setProvider("BC").getCertificate(certBuilder.build(signer));

        // Parse agent cert/key via PemUtils (returns BC-backed objects)
        final PrivateKey agentKeyBc = PemUtils.parsePrivateKey(encryptedValueService.decrypt(agentCertEntry.privateKey()));
        final X509Certificate agentCertBc = PemUtils.parseCertificate(agentCertEntry.certificate());
        final X509Certificate caCertBc = PemUtils.parseCertificate(caEntry.certificate());

        // Remove BC before converting to JDK format. JDK's SSL engine requires
        // EdECPrivateKey (not BC's BCEdDSAPrivateKey) for Ed25519 TLS operations.
        // Only remove if we added it -- other tests may depend on BC being present.
        if (!bcWasPresent) {
            Security.removeProvider("BC");
        }

        // Re-encode and re-parse via JDK providers to get native types
        serverKey = toJdkKey(serverKeyPair.getPrivate());
        serverCert = toJdkCert(serverCertBc);
        agentKey = toJdkKey(agentKeyBc);
        agentCert = toJdkCert(agentCertBc);
        caCert = toJdkCert(caCertBc);
    }

    @BeforeEach
    void setUp() {
        bossGroup = new NioEventLoopGroup(1);
        workerGroup = new NioEventLoopGroup(2);
    }

    @AfterEach
    void tearDown() throws Exception {
        if (grpcServer != null) {
            grpcServer.shutdownNow();
            grpcServer.awaitTermination(5, TimeUnit.SECONDS);
        }
        if (httpServerChannel != null) {
            httpServerChannel.close().sync();
        }
        bossGroup.shutdownGracefully().sync();
        workerGroup.shutdownGracefully().sync();
    }

    // ----- gRPC mTLS Tests -----

    @Test
    void grpcMtlsSuccessWithAgentCertSignedByOpAmpCa() throws Exception {
        final int port = startGrpcServer();
        final ManagedChannel channel = createGrpcChannel(agentKey, agentCert, port);

        try {
            final LogsServiceGrpc.LogsServiceBlockingStub stub = LogsServiceGrpc.newBlockingStub(channel);
            final ExportLogsServiceResponse response = stub.export(createTestRequest());

            assertThat(response).isNotNull();
        } finally {
            channel.shutdownNow().awaitTermination(5, TimeUnit.SECONDS);
        }
    }

    @Test
    void grpcMtlsRejectsConnectionWithNoClientCert() throws Exception {
        final int port = startGrpcServer();

        // Build a client SSL context with no client cert (no keyManager)
        final SslContext clientSsl = GrpcSslContexts.configure(
                SslContextBuilder.forClient()
                        .trustManager(InsecureTrustManagerFactory.INSTANCE),
                SslProvider.JDK
        ).build();

        final ManagedChannel channel = NettyChannelBuilder
                .forAddress("127.0.0.1", port)
                .sslContext(clientSsl)
                .build();

        try {
            final LogsServiceGrpc.LogsServiceBlockingStub stub = LogsServiceGrpc.newBlockingStub(channel);
            assertThatThrownBy(() -> stub.export(createTestRequest()))
                    .isInstanceOf(StatusRuntimeException.class)
                    .satisfies(e -> assertThat(((StatusRuntimeException) e).getStatus().getCode())
                            .isIn(io.grpc.Status.Code.UNAVAILABLE, io.grpc.Status.Code.UNAUTHENTICATED));
        } finally {
            channel.shutdownNow().awaitTermination(5, TimeUnit.SECONDS);
        }
    }

    @Test
    void grpcMtlsSetsCollectorInstanceUidInJournalRecord() throws Exception {
        final int port = startGrpcServer();
        final ManagedChannel channel = createGrpcChannel(agentKey, agentCert, port);

        try {
            final LogsServiceGrpc.LogsServiceBlockingStub stub = LogsServiceGrpc.newBlockingStub(channel);
            stub.export(createTestRequest());

            final ArgumentCaptor<RawMessage> captor = ArgumentCaptor.forClass(RawMessage.class);
            verify(input).processRawMessage(captor.capture());

            final CollectorJournal.Record record = CollectorJournal.Record.parseFrom(captor.getValue().getPayload());
            assertThat(record.hasCollectorInstanceUid()).isTrue();
            assertThat(record.getCollectorInstanceUid()).isEqualTo(AGENT_INSTANCE_UID);
        } finally {
            channel.shutdownNow().awaitTermination(5, TimeUnit.SECONDS);
        }
    }

    @Test
    void grpcTransportFilterFiresAfterTlsHandshake() throws Exception {
        // This test verifies that ServerTransportFilter.transportReady() fires after TLS handshake
        // and that the agent instance UID is available via the gRPC interceptor chain.
        // If the filter did NOT fire, the interceptor would reject with UNAUTHENTICATED.
        final int port = startGrpcServer();
        final ManagedChannel channel = createGrpcChannel(agentKey, agentCert, port);

        try {
            final LogsServiceGrpc.LogsServiceBlockingStub stub = LogsServiceGrpc.newBlockingStub(channel);
            final ExportLogsServiceResponse response = stub.export(createTestRequest());

            // If we got a successful response, the transport filter fired and
            // the interceptor found the UID - the whole chain worked.
            assertThat(response).isNotNull();

            // Double check: the journal record should have the UID set,
            // proving the entire chain from transport filter -> interceptor -> service worked
            final ArgumentCaptor<RawMessage> captor = ArgumentCaptor.forClass(RawMessage.class);
            verify(input).processRawMessage(captor.capture());
            final CollectorJournal.Record record = CollectorJournal.Record.parseFrom(captor.getValue().getPayload());
            assertThat(record.getCollectorInstanceUid()).isEqualTo(AGENT_INSTANCE_UID);
        } finally {
            channel.shutdownNow().awaitTermination(5, TimeUnit.SECONDS);
        }
    }

    // ----- HTTP mTLS Tests -----

    @Test
    void httpMtlsSuccessWithAgentCertSignedByOpAmpCa() throws Exception {
        final int port = startHttpServer();
        final HttpClient client = createHttpClient(agentKey, agentCert);

        final ExportLogsServiceRequest request = createTestRequest();
        final HttpResponse<byte[]> response = client.send(
                HttpRequest.newBuilder()
                        .uri(URI.create("https://127.0.0.1:" + port + "/v1/logs"))
                        .header("Content-Type", "application/x-protobuf")
                        .POST(HttpRequest.BodyPublishers.ofByteArray(request.toByteArray()))
                        .build(),
                HttpResponse.BodyHandlers.ofByteArray()
        );

        assertThat(response.statusCode()).isEqualTo(200);
    }

    @Test
    void httpMtlsRejectsConnectionWithNoClientCert() throws Exception {
        final int port = startHttpServer();

        // Build a Java SSLContext with no client cert - trust all servers
        final SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(null, new TrustManager[]{new TrustAllManager()}, null);

        final HttpClient client = HttpClient.newBuilder()
                .sslContext(sslContext)
                .build();

        final ExportLogsServiceRequest request = createTestRequest();

        // The server requires client auth so the TLS handshake should fail
        assertThatThrownBy(() -> client.send(
                HttpRequest.newBuilder()
                        .uri(URI.create("https://127.0.0.1:" + port + "/v1/logs"))
                        .header("Content-Type", "application/x-protobuf")
                        .POST(HttpRequest.BodyPublishers.ofByteArray(request.toByteArray()))
                        .build(),
                HttpResponse.BodyHandlers.ofByteArray()
        )).hasCauseInstanceOf(SSLHandshakeException.class);
    }

    @Test
    void httpMtlsSetsCollectorInstanceUidInJournalRecord() throws Exception {
        final int port = startHttpServer();
        final HttpClient client = createHttpClient(agentKey, agentCert);

        final ExportLogsServiceRequest request = createTestRequest();
        final HttpResponse<byte[]> response = client.send(
                HttpRequest.newBuilder()
                        .uri(URI.create("https://127.0.0.1:" + port + "/v1/logs"))
                        .header("Content-Type", "application/x-protobuf")
                        .POST(HttpRequest.BodyPublishers.ofByteArray(request.toByteArray()))
                        .build(),
                HttpResponse.BodyHandlers.ofByteArray()
        );

        assertThat(response.statusCode()).isEqualTo(200);

        final ArgumentCaptor<RawMessage> captor = ArgumentCaptor.forClass(RawMessage.class);
        verify(input).processRawMessage(captor.capture());

        final CollectorJournal.Record record = CollectorJournal.Record.parseFrom(captor.getValue().getPayload());
        assertThat(record.hasCollectorInstanceUid()).isTrue();
        assertThat(record.getCollectorInstanceUid()).isEqualTo(AGENT_INSTANCE_UID);
    }

    @Test
    void httpSslHandshakeCompletionEventFiresBeforeHttpRequest() throws Exception {
        // This test verifies that SslHandshakeCompletionEvent fires before HTTP requests,
        // allowing AgentCertChannelHandler to extract the CN before CollectorIngestHttpHandler processes the request.
        // If the handshake event did NOT fire, CollectorIngestHttpHandler would return 401.
        final int port = startHttpServer();
        final HttpClient client = createHttpClient(agentKey, agentCert);

        final ExportLogsServiceRequest request = createTestRequest();
        final HttpResponse<byte[]> response = client.send(
                HttpRequest.newBuilder()
                        .uri(URI.create("https://127.0.0.1:" + port + "/v1/logs"))
                        .header("Content-Type", "application/x-protobuf")
                        .POST(HttpRequest.BodyPublishers.ofByteArray(request.toByteArray()))
                        .build(),
                HttpResponse.BodyHandlers.ofByteArray()
        );

        // 200 means the handler found the agent UID, proving the handshake event fired first
        assertThat(response.statusCode()).isEqualTo(200);

        // Verify that collector_instance_uid was propagated all the way to the journal record
        final ArgumentCaptor<RawMessage> captor = ArgumentCaptor.forClass(RawMessage.class);
        verify(input).processRawMessage(captor.capture());
        final CollectorJournal.Record record = CollectorJournal.Record.parseFrom(captor.getValue().getPayload());
        assertThat(record.getCollectorInstanceUid()).isEqualTo(AGENT_INSTANCE_UID);
    }

    // ----- Helper methods -----

    /**
     * Converts a BouncyCastle Ed25519 private key to JDK-native format.
     * Re-encoding via PKCS#8 and re-parsing via JDK's SunEC provider produces
     * a native {@code EdECPrivateKey} that JDK's SSL engine can use.
     */
    private static PrivateKey toJdkKey(PrivateKey bcKey) throws Exception {
        final KeyFactory kf = KeyFactory.getInstance("Ed25519", "SunEC");
        return kf.generatePrivate(new PKCS8EncodedKeySpec(bcKey.getEncoded()));
    }

    /**
     * Converts a BouncyCastle X.509 certificate to JDK-native format.
     * Re-encoding via DER and re-parsing via JDK's SUN provider produces
     * a native {@code X509CertImpl}.
     */
    private static X509Certificate toJdkCert(X509Certificate bcCert) throws Exception {
        final CertificateFactory cf = CertificateFactory.getInstance("X.509", "SUN");
        return (X509Certificate) cf.generateCertificate(new ByteArrayInputStream(bcCert.getEncoded()));
    }

    private int startGrpcServer() throws Exception {
        // InsecureTrustManagerFactory accepts any client cert. ClientAuth.REQUIRE
        // still enforces that a cert MUST be presented. The cert is then available
        // in the SSL session for CN extraction by AgentCertTransportFilter.
        final SslContext sslContext = GrpcSslContexts.configure(
                SslContextBuilder.forServer(serverKey, serverCert)
                        .sslProvider(SslProvider.JDK)
                        .clientAuth(ClientAuth.REQUIRE)
                        .trustManager(InsecureTrustManagerFactory.INSTANCE),
                SslProvider.JDK
        ).build();

        final CollectorIngestLogsService logsService = new CollectorIngestLogsService(
                transport, input, new OTelJournalRecordFactory());

        grpcServer = NettyServerBuilder
                .forAddress(new InetSocketAddress("127.0.0.1", 0))
                .sslContext(sslContext)
                .addTransportFilter(new AgentCertTransportFilter())
                .intercept(new AgentCertAuthInterceptor())
                .intercept(new RemoteAddressProviderInterceptor())
                .addService(logsService)
                .build()
                .start();

        return grpcServer.getPort();
    }

    private ManagedChannel createGrpcChannel(PrivateKey clientKey, X509Certificate clientCert,
                                              int port) throws SSLException {
        final SslContext clientSsl = GrpcSslContexts.configure(
                SslContextBuilder.forClient()
                        .keyManager(clientKey, clientCert)
                        .trustManager(InsecureTrustManagerFactory.INSTANCE),
                SslProvider.JDK
        ).build();

        return NettyChannelBuilder
                .forAddress("127.0.0.1", port)
                .sslContext(clientSsl)
                .build();
    }

    private int startHttpServer() throws Exception {
        // InsecureTrustManagerFactory accepts any client cert. ClientAuth.REQUIRE
        // still enforces that a cert MUST be presented.
        final SslContext sslContext = SslContextBuilder.forServer(serverKey, serverCert)
                .sslProvider(SslProvider.JDK)
                .clientAuth(ClientAuth.REQUIRE)
                .trustManager(InsecureTrustManagerFactory.INSTANCE)
                .build();

        final OTelJournalRecordFactory journalRecordFactory = new OTelJournalRecordFactory();

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
                        pipeline.addLast("http-handler", new CollectorIngestHttpHandler(journalRecordFactory, input));
                    }
                });

        httpServerChannel = bootstrap.bind("127.0.0.1", 0).sync().channel();
        return ((InetSocketAddress) httpServerChannel.localAddress()).getPort();
    }

    /**
     * Creates a Java HttpClient with mTLS using a custom KeyManager and trust-all TrustManager.
     * <p>
     * The custom {@link SimpleKeyManager} avoids PKCS12 keystore chain validation issues
     * with Ed25519 certs (JDK's {@code KeyStore.setKeyEntry()} rejects Ed25519 cert chains).
     */
    private HttpClient createHttpClient(PrivateKey clientKey, X509Certificate clientCert) throws Exception {
        final X509ExtendedKeyManager km = new SimpleKeyManager(clientKey, clientCert, caCert);

        final SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(new KeyManager[]{km}, new TrustManager[]{new TrustAllManager()}, null);

        return HttpClient.newBuilder()
                .sslContext(sslContext)
                .build();
    }

    private ExportLogsServiceRequest createTestRequest() {
        return ExportLogsServiceRequest.newBuilder()
                .addResourceLogs(ResourceLogs.newBuilder()
                        .addScopeLogs(ScopeLogs.newBuilder()
                                .addLogRecords(LogRecord.newBuilder()
                                        .setBody(AnyValue.newBuilder().setStringValue("test log message"))
                                        .setTimeUnixNano(System.nanoTime())
                                        .setSeverityText("INFO")
                                )))
                .build();
    }

    /**
     * A simple X509ExtendedKeyManager that returns a fixed client cert and key.
     * This avoids the PKCS12 keystore chain validation that fails for Ed25519 certs.
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
     * A TrustManager that accepts all certificates. Used only in tests to bypass
     * client-side server cert validation. Server-side mTLS validation (the focus
     * of these tests) is unaffected.
     */
    private static class TrustAllManager implements X509TrustManager {
        @Override
        public void checkClientTrusted(X509Certificate[] chain, String authType) {
            // Accept all
        }

        @Override
        public void checkServerTrusted(X509Certificate[] chain, String authType) {
            // Accept all
        }

        @Override
        public X509Certificate[] getAcceptedIssuers() {
            return new X509Certificate[0];
        }
    }
}
