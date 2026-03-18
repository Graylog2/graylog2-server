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
import org.graylog.collectors.input.transport.AgentCertChannelHandler;
import org.graylog.collectors.input.transport.CollectorIngestHttpHandler;
import org.graylog.security.pki.Algorithm;
import org.graylog.security.pki.CertificateBuilder;
import org.graylog.security.pki.CertificateEntry;
import org.graylog.security.pki.PemUtils;
import org.graylog.testing.TestClocks;
import org.graylog2.plugin.inputs.MessageInput;
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

import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLHandshakeException;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509ExtendedKeyManager;
import javax.net.ssl.X509TrustManager;
import java.math.BigInteger;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.Principal;
import java.security.PrivateKey;
import java.security.Security;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;

/**
 * Integration tests for Ed25519 mTLS over the managed HTTP ingest path.
 * <p>
 * These tests verify the remaining technical risk after removing the unpublished gRPC path:
 * Ed25519 mTLS still works end-to-end for the HTTP OTLP transport. Each test starts a real
 * server with TLS, creates client connections using Ed25519 certificates, and validates
 * authentication and data flow.
 * <p>
 * BouncyCastle is used to generate Ed25519 certificates (JDK does not provide a
 * certificate builder API). All keys and certs are parsed through {@link PemUtils}
 * (same code path as production) which returns JDK-native types via SunEC.
 * <p>
 * Both client and server use {@code InsecureTrustManagerFactory} to bypass PKIX cert path
 * validation. The server still enforces {@code ClientAuth.REQUIRE}, ensuring the client
 * must present a certificate. The focus of these tests is on the mTLS handshake mechanics
 * and agent identity propagation, not on cert chain validation.
 */
@ExtendWith(MockitoExtension.class)
class CollectorIngestMtlsIT {

    private static final String AGENT_INSTANCE_UID = "test-agent-instance-123";
    private static final Duration CERT_VALIDITY = Duration.ofDays(1);

    // Server cert with SAN for 127.0.0.1
    private static PrivateKey serverKey;
    private static X509Certificate serverCert;

    // Agent cert signed by the CA
    private static PrivateKey agentKey;
    private static X509Certificate agentCert;

    // CA cert (self-signed root)
    private static X509Certificate caCert;

    @Mock
    private MessageInput input;

    private Channel httpServerChannel;
    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;


    @BeforeAll
    static void setupCerts() throws Exception {
        // Register BC for cert generation (JDK has no certificate builder API).
        if (Security.getProvider("BC") == null) {
            Security.addProvider(new BouncyCastleProvider());
        }

        final EncryptedValueService encryptedValueService = new EncryptedValueService("1234567890abcdef");
        final CertificateBuilder certificateBuilder = new CertificateBuilder(encryptedValueService, "Test", TestClocks.fixedEpoch());

        // Create a flat CA hierarchy: self-signed root CA signs all end-entity certs
        final CertificateEntry caEntry = certificateBuilder.createRootCa("Test CA", Algorithm.ED25519, CERT_VALIDITY);
        final CertificateEntry agentCertEntry = certificateBuilder.createEndEntityCert(AGENT_INSTANCE_UID, caEntry,
                KeyUsage.digitalSignature, KeyPurposeId.id_kp_clientAuth, CERT_VALIDITY);

        // Parse CA and agent certs via PemUtils — same code path as production
        // (OpAmpCaService.newServerSslContextBuilder())
        caCert = PemUtils.parseCertificate(caEntry.certificate());
        agentKey = PemUtils.parsePrivateKey(encryptedValueService.decrypt(agentCertEntry.privateKey()));
        agentCert = PemUtils.parseCertificate(agentCertEntry.certificate());

        // Create server cert with SAN for IP 127.0.0.1 (required by hostname verification).
        // CertificateBuilder only supports DNS SANs, so we build this one manually with BC.
        final PrivateKey issuerKey = PemUtils.parsePrivateKey(encryptedValueService.decrypt(caEntry.privateKey()));
        final KeyPair serverKeyPair = KeyPairGenerator.getInstance("Ed25519").generateKeyPair();
        final X500Name serverSubject = new X500NameBuilder(BCStyle.INSTANCE)
                .addRDN(BCStyle.CN, "Test Server").addRDN(BCStyle.O, "Test").build();
        final X500Name issuerDn = new X500Name(caCert.getSubjectX500Principal().getName());
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
                .setProvider("BC").build(issuerKey);
        serverCert = new JcaX509CertificateConverter()
                .getCertificate(certBuilder.build(signer));
        serverKey = serverKeyPair.getPrivate();
    }

    @BeforeEach
    void setUp() {
        bossGroup = new NioEventLoopGroup(1);
        workerGroup = new NioEventLoopGroup(2);
    }

    @AfterEach
    void tearDown() throws Exception {
        if (httpServerChannel != null) {
            httpServerChannel.close().sync();
        }
        bossGroup.shutdownGracefully().sync();
        workerGroup.shutdownGracefully().sync();
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

    private int startHttpServer() throws Exception {
        // InsecureTrustManagerFactory accepts any client cert. ClientAuth.REQUIRE
        // still enforces that a cert MUST be presented.
        final SslContext sslContext = SslContextBuilder.forServer(serverKey, serverCert)
                .sslProvider(SslProvider.JDK)
                .clientAuth(ClientAuth.REQUIRE)
                .trustManager(InsecureTrustManagerFactory.INSTANCE)
                .build();

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
                        pipeline.addLast("http-handler", new CollectorIngestHttpHandler(input));
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
