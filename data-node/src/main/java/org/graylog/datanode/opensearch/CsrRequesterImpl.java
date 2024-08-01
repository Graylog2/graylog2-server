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

import com.google.common.collect.ImmutableList;
import jakarta.inject.Inject;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;
import org.graylog.datanode.Configuration;
import org.graylog.datanode.configuration.DatanodeConfiguration;
import org.graylog.datanode.configuration.DatanodeKeystore;
import org.graylog.datanode.configuration.DatanodeKeystoreException;
import org.graylog.security.certutil.csr.exceptions.CSRGenerationException;
import org.graylog2.cluster.NodeNotFoundException;
import org.graylog2.cluster.certificates.CertificateExchange;
import org.graylog2.cluster.certificates.CertificateSigningRequest;
import org.graylog2.cluster.nodes.DataNodeDto;
import org.graylog2.cluster.nodes.NodeService;
import org.graylog2.plugin.system.NodeId;
import org.graylog2.shared.SuppressForbidden;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetAddress;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class CsrRequesterImpl implements CsrRequester {

    private static final Logger LOG = LoggerFactory.getLogger(CsrRequesterImpl.class);

    private final NodeId nodeId;

    private final DatanodeKeystore datanodeKeystore;

    private final CertificateExchange certificateExchange;
    private final String hostname;

    @Inject
    public CsrRequesterImpl(Configuration datanodeConfiguration, NodeId nodeId, DatanodeKeystore datanodeKeystore, CertificateExchange certificateExchange) {
        this.hostname = datanodeConfiguration.getHostname();
        this.nodeId = nodeId;
        this.datanodeKeystore = datanodeKeystore;
        this.certificateExchange = certificateExchange;
    }

    public void triggerCertificateSigningRequest() {
        try {
            final var altNames = ImmutableList.<String>builder()
                    .addAll(determineAltNames())
                    .build();
            final PKCS10CertificationRequest csr = datanodeKeystore.createCertificateSigningRequest(hostname, altNames);
            certificateExchange.requestCertificate(new CertificateSigningRequest(nodeId.getNodeId(), csr));
            LOG.info("Triggered certificate signing request for this datanode");
        } catch (CSRGenerationException | IOException | DatanodeKeystoreException ex) {
            throw new RuntimeException(ex);
        }
    }


    private Iterable<String> determineAltNames() {
        return Stream.of("127.0.0.1", "::1")
                .map(this::reverseLookup)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }

    @SuppressForbidden("Deliberate use of InetAddress#getHostName")
    private String reverseLookup(String ipAddress) {
        try {
            final var inetAddress = InetAddress.getByName(ipAddress);
            final var reverseLookup = inetAddress.getHostName();
            return reverseLookup.equals(ipAddress) ? null : reverseLookup;
        } catch (Exception e) {
            return null;
        }
    }
}
