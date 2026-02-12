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
package org.graylog.collectors.input.transport;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.ssl.SslHandler;
import io.netty.handler.ssl.SslHandshakeCompletionEvent;
import org.graylog.security.pki.Algorithm;
import org.graylog.security.pki.CertificateBuilder;
import org.graylog.security.pki.CertificateEntry;
import org.graylog.security.pki.PemUtils;
import org.graylog2.security.encryption.EncryptedValueService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLSession;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AgentCertChannelHandlerTest {

    private CertificateBuilder builder;

    @BeforeEach
    void setUp() {
        final EncryptedValueService evs = new EncryptedValueService("1234567890abcdef");
        builder = new CertificateBuilder(evs, "Test");
    }

    @Test
    void setsChannelAttributeOnSuccessfulHandshake() throws Exception {
        final CertificateEntry rootCa = builder.createRootCa("Root", Algorithm.ED25519, Duration.ofDays(1));
        final CertificateEntry agentCert = builder.createEndEntityCert(
                "my-agent-uid", rootCa,
                org.bouncycastle.asn1.x509.KeyUsage.digitalSignature,
                Duration.ofDays(1));
        final X509Certificate x509 = PemUtils.parseCertificate(agentCert.certificate());

        final SSLSession session = mock(SSLSession.class);
        when(session.getPeerCertificates()).thenReturn(new Certificate[]{x509});

        final SSLEngine engine = mock(SSLEngine.class);
        when(engine.getSession()).thenReturn(session);

        final SslHandler sslHandler = mock(SslHandler.class);
        when(sslHandler.engine()).thenReturn(engine);

        // Add AgentCertChannelHandler first so the event reaches it before the mock SslHandler.
        // The mock SslHandler is added after so that pipeline.get(SslHandler.class) still finds it.
        final EmbeddedChannel channel = new EmbeddedChannel(new AgentCertChannelHandler());
        channel.pipeline().addLast(sslHandler);

        channel.pipeline().fireUserEventTriggered(SslHandshakeCompletionEvent.SUCCESS);

        final String uid = channel.attr(AgentCertChannelHandler.AGENT_INSTANCE_UID).get();
        assertThat(uid).isEqualTo("my-agent-uid");

        channel.close();
    }

    @Test
    void doesNotSetAttributeOnFailedHandshake() {
        final SslHandler sslHandler = mock(SslHandler.class);

        final EmbeddedChannel channel = new EmbeddedChannel(new AgentCertChannelHandler());
        channel.pipeline().addLast(sslHandler);

        final SslHandshakeCompletionEvent failEvent =
                new SslHandshakeCompletionEvent(new RuntimeException("handshake failed"));
        channel.pipeline().fireUserEventTriggered(failEvent);

        final String uid = channel.attr(AgentCertChannelHandler.AGENT_INSTANCE_UID).get();
        assertThat(uid).isNull();

        channel.close();
    }

    @Test
    void propagatesEventToNextHandler() throws Exception {
        final CertificateEntry rootCa = builder.createRootCa("Root", Algorithm.ED25519, Duration.ofDays(1));
        final CertificateEntry agentCert = builder.createEndEntityCert(
                "propagation-test", rootCa,
                org.bouncycastle.asn1.x509.KeyUsage.digitalSignature,
                Duration.ofDays(1));
        final X509Certificate x509 = PemUtils.parseCertificate(agentCert.certificate());

        final SSLSession session = mock(SSLSession.class);
        when(session.getPeerCertificates()).thenReturn(new Certificate[]{x509});

        final SSLEngine engine = mock(SSLEngine.class);
        when(engine.getSession()).thenReturn(session);

        final SslHandler sslHandler = mock(SslHandler.class);
        when(sslHandler.engine()).thenReturn(engine);

        // Track if the event was propagated to the handler after AgentCertChannelHandler
        final boolean[] eventReceived = new boolean[1];
        final ChannelInboundHandlerAdapter tracker =
                new ChannelInboundHandlerAdapter() {
                    @Override
                    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) {
                        if (evt instanceof SslHandshakeCompletionEvent) {
                            eventReceived[0] = true;
                        }
                    }
                };

        // Pipeline order: AgentCertChannelHandler -> tracker -> sslHandler(mock)
        final EmbeddedChannel channel = new EmbeddedChannel(new AgentCertChannelHandler(), tracker);
        channel.pipeline().addLast(sslHandler);

        channel.pipeline().fireUserEventTriggered(SslHandshakeCompletionEvent.SUCCESS);

        assertThat(eventReceived[0]).isTrue();

        channel.close();
    }
}
