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

public class CsrRequester {

    private static final Logger LOG = LoggerFactory.getLogger(OpensearchProcessService.class);

    private final NodeService<DataNodeDto> nodeService;
    private final NodeId nodeId;

    private final DatanodeKeystore datanodeKeystore;

    private final CertificateExchange certificateExchange;

    @Inject
    public CsrRequester(NodeService<DataNodeDto> nodeService, NodeId nodeId, DatanodeKeystore datanodeKeystore, CertificateExchange certificateExchange) {
        this.nodeService = nodeService;
        this.nodeId = nodeId;
        this.datanodeKeystore = datanodeKeystore;
        this.certificateExchange = certificateExchange;
    }

    public void triggerCsr() {
        try {
            final var node = nodeService.byNodeId(nodeId);
            final var altNames = ImmutableList.<String>builder()
                    .addAll(determineAltNames())
                    .build();
            final PKCS10CertificationRequest csr = datanodeKeystore.createCertificateSigningRequest(node.getHostname(), altNames);
            certificateExchange.requestCertificate(new CertificateSigningRequest(nodeId.getNodeId(), csr));
            LOG.info("created CSR for this node");
        } catch (CSRGenerationException | IOException | NodeNotFoundException | DatanodeKeystoreException ex) {
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
