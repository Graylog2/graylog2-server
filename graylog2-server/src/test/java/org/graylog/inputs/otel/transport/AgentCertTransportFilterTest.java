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
package org.graylog.inputs.otel.transport;

import io.grpc.Attributes;
import io.grpc.Grpc;
import org.graylog.security.pki.Algorithm;
import org.graylog.security.pki.CertificateBuilder;
import org.graylog.security.pki.CertificateEntry;
import org.graylog.security.pki.PemUtils;
import org.graylog2.security.encryption.EncryptedValueService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.net.ssl.SSLPeerUnverifiedException;
import javax.net.ssl.SSLSession;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AgentCertTransportFilterTest {

    private AgentCertTransportFilter filter;
    private CertificateBuilder builder;

    @BeforeEach
    void setUp() {
        filter = new AgentCertTransportFilter();
        final EncryptedValueService evs = new EncryptedValueService("1234567890abcdef");
        builder = new CertificateBuilder(evs, "Test");
    }

    @Test
    void transportReadyExtractsCnFromClientCertificate() throws Exception {
        final CertificateEntry rootCa = builder.createRootCa("Root", Algorithm.ED25519, Duration.ofDays(1));
        final CertificateEntry agentCert = builder.createEndEntityCert(
                "test-agent-uid", rootCa,
                org.bouncycastle.asn1.x509.KeyUsage.digitalSignature,
                Duration.ofDays(1));
        final X509Certificate x509 = PemUtils.parseCertificate(agentCert.certificate());

        final SSLSession session = mock(SSLSession.class);
        when(session.getPeerCertificates()).thenReturn(new Certificate[]{x509});

        final Attributes attrs = Attributes.newBuilder()
                .set(Grpc.TRANSPORT_ATTR_SSL_SESSION, session)
                .build();

        final Attributes result = filter.transportReady(attrs);

        assertThat(result.get(AgentCertTransportFilter.AGENT_INSTANCE_UID_KEY))
                .isEqualTo("test-agent-uid");
    }

    @Test
    void transportReadyReturnsUnchangedAttributesWhenNoSslSession() {
        final Attributes attrs = Attributes.newBuilder().build();

        final Attributes result = filter.transportReady(attrs);

        assertThat(result.get(AgentCertTransportFilter.AGENT_INSTANCE_UID_KEY)).isNull();
    }

    @Test
    void transportReadyReturnsUnchangedAttributesWhenPeerCertificatesFails() throws Exception {
        final SSLSession session = mock(SSLSession.class);
        when(session.getPeerCertificates()).thenThrow(new SSLPeerUnverifiedException("no peer certs"));

        final Attributes attrs = Attributes.newBuilder()
                .set(Grpc.TRANSPORT_ATTR_SSL_SESSION, session)
                .build();

        final Attributes result = filter.transportReady(attrs);

        assertThat(result.get(AgentCertTransportFilter.AGENT_INSTANCE_UID_KEY)).isNull();
    }
}
