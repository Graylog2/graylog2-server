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
import io.grpc.ServerTransportFilter;
import org.graylog.security.pki.PemUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLSession;
import java.security.cert.X509Certificate;

/**
 * A gRPC {@link ServerTransportFilter} that extracts the agent's instance_uid from the
 * client certificate CN during TLS handshake and stores it in transport {@link Attributes}.
 * <p>
 * This runs once per connection (not per RPC), making it efficient for extracting identity
 * from mTLS client certificates. The extracted UID is stored under {@link #AGENT_INSTANCE_UID_KEY}
 * and can be read by interceptors (see {@link AgentCertAuthInterceptor}).
 */
public class AgentCertTransportFilter extends ServerTransportFilter {
    private static final Logger LOG = LoggerFactory.getLogger(AgentCertTransportFilter.class);

    public static final Attributes.Key<String> AGENT_INSTANCE_UID_KEY =
            Attributes.Key.create("agent-instance-uid");

    @Override
    public Attributes transportReady(Attributes transportAttrs) {
        final SSLSession session = transportAttrs.get(Grpc.TRANSPORT_ATTR_SSL_SESSION);
        if (session != null) {
            try {
                final X509Certificate cert = (X509Certificate) session.getPeerCertificates()[0];
                final String instanceUid = PemUtils.extractCn(cert);
                return transportAttrs.toBuilder()
                        .set(AGENT_INSTANCE_UID_KEY, instanceUid)
                        .build();
            } catch (Exception e) {
                LOG.warn("Failed to extract agent identity from client certificate", e);
            }
        }
        return transportAttrs;
    }
}
