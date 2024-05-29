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
package org.graylog.datanode.bootstrap.preflight;

import com.google.common.collect.ImmutableList;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;
import org.graylog.datanode.configuration.DatanodeKeystore;
import org.graylog.datanode.configuration.DatanodeKeystoreException;
import org.graylog.security.certutil.csr.exceptions.CSRGenerationException;
import org.graylog2.bootstrap.preflight.CertificateSignedEvent;
import org.graylog2.bootstrap.preflight.CertificateSigningRequestEvent;
import org.graylog2.cluster.NodeNotFoundException;
import org.graylog2.cluster.nodes.DataNodeDto;
import org.graylog2.cluster.nodes.NodeService;
import org.graylog2.datanode.DataNodeLifecycleEvent;
import org.graylog2.datanode.DataNodeLifecycleTrigger;
import org.graylog2.events.ClusterEventBus;
import org.graylog2.plugin.system.NodeId;
import org.graylog2.shared.SuppressForbidden;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetAddress;
import java.security.cert.CertificateException;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * This listener handles two types of events:
 * <ul>
 *     <li>{@link DataNodeLifecycleTrigger#REQUEST_CERTIFICATE} that will generate and send a certificate signing request</li>
 *     <li>{@link CertificateSignedEvent} that holds the already signed certificate chain which should be </li>
 * </ul>
 */
@Singleton
public class DatanodeCertificatesListener {
    private static final Logger LOG = LoggerFactory.getLogger(DatanodeCertificatesListener.class);

    private final NodeService<DataNodeDto> nodeService;
    private final NodeId nodeId;

    private final DatanodeKeystore datanodeKeystore;
    private final ClusterEventBus clusterEventBus;

    @Inject
    public DatanodeCertificatesListener(final NodeService<DataNodeDto> nodeService,
                                        final NodeId nodeId,
                                        final DatanodeKeystore datanodeKeystore,
                                        final ClusterEventBus clusterEventBus,
                                        final EventBus eventBus
    ) {
        this.nodeService = nodeService;
        this.nodeId = nodeId;
        this.datanodeKeystore = datanodeKeystore;
        this.clusterEventBus = clusterEventBus;
        eventBus.register(this);
    }


    @Subscribe
    public void certificateRequestedListener(DataNodeLifecycleEvent event) {
        if (nodeId.getNodeId().equals(event.nodeId())) {
            LOG.info("Received DataNodeLifecycleEvent with trigger " + event.trigger());
            if (Objects.requireNonNull(event.trigger()) == DataNodeLifecycleTrigger.REQUEST_CERTIFICATE) {
                triggerCertificateSigningRequest();
            }
        }
    }

    @Subscribe
    public void certificateSignedListener(CertificateSignedEvent event) {
        if (nodeId.getNodeId().equals(event.nodeId())) {
            LOG.info("Received signed certificates for datanode " + event.nodeId());
            try {
                datanodeKeystore.replaceCertificatesInKeystore(event.readCertChain());
            } catch (DatanodeKeystoreException | CertificateException | IOException ex) {
                LOG.error("Failed to replace certificate chain in datanode keystore", ex);
            }
        }
    }

    private void triggerCertificateSigningRequest() {
        try {
            final var hostname = nodeService.byNodeId(nodeId).getHostname();

            final var altNames = ImmutableList.<String>builder()
                    // TODO: we don't support any external alt names configuration now
                    //.addAll(Optional.ofNullable(cfg.altNames()).orElse(Collections.emptyList()))
                    .addAll(determineAltNames())
                    .build();

            final PKCS10CertificationRequest csr = datanodeKeystore.createCertificateSigningRequest(hostname, altNames);
            clusterEventBus.post(CertificateSigningRequestEvent.fromCsr(nodeId.getNodeId(), csr));
            LOG.info("Posted CertificateSigningRequestEvent for node " + nodeId.getNodeId());
        } catch (CSRGenerationException | IOException | NodeNotFoundException | DatanodeKeystoreException ex) {
            LOG.error("error generating a CSR: " + ex.getMessage(), ex);
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
