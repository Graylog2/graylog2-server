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
package org.graylog.collectors;

import com.google.common.eventbus.EventBus;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.MultiThreadIoEventLoopGroup;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioIoHandler;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.ssl.SslContext;
import org.bouncycastle.asn1.x509.KeyPurposeId;
import org.bouncycastle.asn1.x509.KeyUsage;
import org.graylog.collectors.input.transport.AgentCertChannelHandler;
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
import org.graylog2.security.TrustAllX509TrustManager;
import org.graylog2.security.encryption.EncryptedValueService;
import org.graylog2.web.customization.CustomizationConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
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
import java.nio.charset.StandardCharsets;
import java.security.Principal;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.time.Clock;
import java.time.Duration;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Integration test for {@link CollectorTLSUtils} with a real Netty server.
 * <p>
 * The test creates a three-level CA hierarchy (root CA → signing cert → server cert),
 * wires it through mocked {@link CollectorCaService} into a real {@link CollectorCaKeyManager} and
 * {@link CollectorCaTrustManager}, then verifies Ed25519 mTLS handshakes succeed end-to-end.
 */
@ExtendWith(MongoDBExtension.class)
@ExtendWith(ClusterConfigServiceExtension.class)
class CollectorTLSUtilsIT {

    private static final String AGENT_INSTANCE_UID = "test-agent-42";
    private static final Duration CERT_VALIDITY = Duration.ofDays(1);

    private PrivateKey agentKey;
    private X509Certificate agentCert;
    private X509Certificate signingCert;

    private final EncryptedValueService encryptedValueService = new EncryptedValueService("1234567890abcdef");

    private CollectorCaCache caCache;
    private CollectorTLSUtils tlsUtils;
    private Channel serverChannel;
    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;

    @BeforeEach
    void setUp(MongoCollections mongoCollections, ClusterConfigService clusterConfigService) throws Exception {
        final var certBuilder = new CertificateBuilder(encryptedValueService, "Test", Clock.systemUTC());

        final var certService = new CertificateService(mongoCollections, encryptedValueService, CustomizationConfig.empty(), Clock.systemUTC());
        final var clusterIdService = mock(ClusterIdService.class);
        final var httpConfiguration = mock(HttpConfiguration.class);
        when(httpConfiguration.getHttpExternalUri()).thenReturn(java.net.URI.create("https://localhost:443/"));
        final var collectorsConfigService = new CollectorsConfigService(clusterConfigService, new ClusterEventBus(), httpConfiguration);
        final var caService = new CollectorCaService(certService, clusterIdService, collectorsConfigService, Clock.systemUTC());

        when(clusterIdService.getString()).thenReturn(UUID.randomUUID().toString());

        final var hierarchy = caService.initializeCa();

        collectorsConfigService.save(CollectorsConfig.createDefaultBuilder("localhost")
                .caCertId(hierarchy.caCert().id())
                .signingCertId(hierarchy.signingCert().id())
                .otlpServerCertId(hierarchy.otlpServerCert().id())
                .build());

        final CertificateEntry agentCertEntry = certBuilder.createEndEntityCert(
                AGENT_INSTANCE_UID,
                hierarchy.signingCert(),
                KeyUsage.digitalSignature,
                KeyPurposeId.id_kp_clientAuth,
                CERT_VALIDITY
        );

        signingCert = PemUtils.parseCertificate(hierarchy.signingCert().certificate());
        agentKey = PemUtils.parsePrivateKey(encryptedValueService.decrypt(agentCertEntry.privateKey()));
        agentCert = PemUtils.parseCertificate(agentCertEntry.certificate());

        caCache = new CollectorCaCache(caService, certService, encryptedValueService, new EventBus(), Clock.systemUTC());
        caCache.startAsync().awaitRunning();
        final var keyManager = new CollectorCaKeyManager(caCache);
        final var trustManager = new CollectorCaTrustManager(caCache, Clock.systemUTC());
        tlsUtils = new CollectorTLSUtils(keyManager, trustManager);

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
    void mtlsHandshakeSucceedsWithKeyManagerProvidedCerts() throws Exception {
        final int port = startServer();
        final HttpClient client = createMtlsClient(agentKey, agentCert);

        final HttpResponse<String> response = client.send(
                HttpRequest.newBuilder()
                        .uri(URI.create("https://127.0.0.1:" + port + "/test"))
                        .GET()
                        .build(),
                HttpResponse.BodyHandlers.ofString()
        );

        assertThat(response.statusCode()).isEqualTo(200);
        // AgentCertChannelHandler extracts the CN from the client cert
        assertThat(response.body()).isEqualTo(AGENT_INSTANCE_UID);
    }

    @Test
    void mtlsRejectsConnectionWithoutClientCert() throws Exception {
        final int port = startServer();

        final SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(null, new TrustManager[]{new TrustAllX509TrustManager()}, null);
        final HttpClient client = HttpClient.newBuilder().sslContext(sslContext).build();

        assertThatThrownBy(() -> client.send(
                HttpRequest.newBuilder()
                        .uri(URI.create("https://127.0.0.1:" + port + "/test"))
                        .GET()
                        .build(),
                HttpResponse.BodyHandlers.ofString()
        )).hasCauseInstanceOf(SSLHandshakeException.class);
    }

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
                        pipeline.addLast("http-aggregator", new HttpObjectAggregator(64 * 1024));
                        pipeline.addLast("handler", new EchoAgentUidHandler());
                    }
                });

        serverChannel = bootstrap.bind("127.0.0.1", 0).sync().channel();
        return ((InetSocketAddress) serverChannel.localAddress()).getPort();
    }

    private HttpClient createMtlsClient(PrivateKey clientKey, X509Certificate clientCert) throws Exception {
        final X509ExtendedKeyManager km = new SimpleKeyManager(clientKey, clientCert, signingCert);
        final SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(new KeyManager[]{km}, new TrustManager[]{new TrustAllManager()}, null);
        return HttpClient.newBuilder().sslContext(sslContext).build();
    }

    /**
     * Simple HTTP handler that echoes the agent instance UID extracted by {@link AgentCertChannelHandler},
     * or "ok" if no UID path is requested at {@code /test}.
     */
    private static class EchoAgentUidHandler extends SimpleChannelInboundHandler<FullHttpRequest> {
        @Override
        protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest request) {
            final String uid = ctx.channel().attr(AgentCertChannelHandler.AGENT_INSTANCE_UID).get();
            final byte[] body = (uid != null ? uid : "ok").getBytes(StandardCharsets.UTF_8);

            final DefaultFullHttpResponse response = new DefaultFullHttpResponse(
                    HttpVersion.HTTP_1_1, HttpResponseStatus.OK, Unpooled.wrappedBuffer(body));
            response.headers().set(HttpHeaderNames.CONTENT_LENGTH, body.length);
            ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
        }
    }

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
     * Trust manager that accepts all certificates and skips hostname verification.
     * <p>
     * Must extend {@link X509ExtendedTrustManager} (not just {@link javax.net.ssl.X509TrustManager})
     * because the JDK wraps plain X509TrustManager in AbstractTrustManagerWrapper which adds
     * hostname/IP identity checks. X509ExtendedTrustManager is used directly, bypassing the wrapper.
     */
    private static class TrustAllManager extends X509ExtendedTrustManager {
        @Override
        public void checkClientTrusted(X509Certificate[] chain, String authType) {
        }

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
