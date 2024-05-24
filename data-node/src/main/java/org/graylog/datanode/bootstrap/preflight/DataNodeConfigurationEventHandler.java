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
import org.graylog2.cluster.preflight.DataNodeProvisioningConfig;
import org.graylog2.cluster.preflight.DataNodeProvisioningService;
import org.graylog2.cluster.preflight.DataNodeProvisioningStateChangeEvent;
import org.graylog2.events.ClusterEventBus;
import org.graylog2.plugin.system.NodeId;
import org.graylog2.shared.SuppressForbidden;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetAddress;
import java.util.Collections;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Singleton
public class DataNodeConfigurationEventHandler {
    private static final Logger LOG = LoggerFactory.getLogger(DataNodeConfigurationEventHandler.class);

    private final DataNodeProvisioningService dataNodeProvisioningService;
    private final NodeService<DataNodeDto> nodeService;
    private final NodeId nodeId;

    private final DatanodeKeystore datanodeKeystore;
    private final ClusterEventBus clusterEventBus;

    @Inject
    public DataNodeConfigurationEventHandler(final DataNodeProvisioningService dataNodeProvisioningService,
                                             final NodeService<DataNodeDto> nodeService,
                                             final NodeId nodeId,
                                             final DatanodeKeystore datanodeKeystore,
                                             final ClusterEventBus clusterEventBus,
                                             final EventBus eventBus

    ) {
        this.dataNodeProvisioningService = dataNodeProvisioningService;
        this.nodeService = nodeService;
        this.nodeId = nodeId;
        this.datanodeKeystore = datanodeKeystore;
        this.clusterEventBus = clusterEventBus;
        eventBus.register(this);
        writeInitialConfig(dataNodeProvisioningService, nodeId);
    }

    private void writeInitialConfig(DataNodeProvisioningService dataNodeProvisioningService, NodeId nodeId) {
        final Optional<DataNodeProvisioningConfig> existingConfig = dataNodeProvisioningService.getPreflightConfigFor(nodeId.getNodeId());
        if (existingConfig.isEmpty()) {
            writeInitialProvisioningConfig();
        }
    }


    @Subscribe
    public void doRun(DataNodeProvisioningStateChangeEvent event) {
        if (nodeId.getNodeId().equals(event.nodeId())) {
            LOG.info("Received DataNodeProvisioningStateChangeEvent with state " + event.state());

            final DataNodeProvisioningConfig cfg = dataNodeProvisioningService.getPreflightConfigFor(nodeId.getNodeId()).orElseThrow(() -> new IllegalStateException("No preflight configuration found for node " + nodeId.getNodeId()));


            Optional.ofNullable(event.state()).ifPresent(state -> {
                switch (state) {
                    case CONFIGURED -> writeCsr(cfg);
                    case SIGNED -> LOG.warn("Ignoring state, will be handled by events");
                    case STARTUP_TRIGGER ->
                            dataNodeProvisioningService.changeState(nodeId.getNodeId(), DataNodeProvisioningConfig.State.STARTUP_REQUESTED);
                }
            });
        } else {
            LOG.debug("Ignoring DataNodeProvisioningStateChangeEvent, it's not intended for this datanode");
        }
    }

    @Subscribe
    public void certificateSignedListener(CertificateSignedEvent event) {

        if (!nodeId.getNodeId().equals(event.nodeId())) {
            // not for this datanode, ignoring
            return;
        }
        LOG.info("Received CertificateSignedEvent for node " + event.nodeId());
        try {
            datanodeKeystore.replaceCertificatesInKeystore(event.readCertChain());
            //should be in one transaction, but we miss transactions...
            dataNodeProvisioningService.changeState(nodeId.getNodeId(), DataNodeProvisioningConfig.State.STORED);
        } catch (Exception ex) {
            LOG.error("Config entry in signed state, but wrong certificate data present in Mongo", ex);
        }
    }

    private void writeCsr(DataNodeProvisioningConfig cfg) {
        try {
            final var hostname = nodeService.byNodeId(nodeId).getHostname();

            final var altNames = ImmutableList.<String>builder()
                    .addAll(Optional.ofNullable(cfg.altNames()).orElse(Collections.emptyList()))
                    .addAll(determineAltNames())
                    .build();

            final PKCS10CertificationRequest csr = datanodeKeystore.createCertificateSigningRequest(hostname, altNames);

            postCsrEvent(csr);
            LOG.info("created CSR for this node");
        } catch (CSRGenerationException | IOException | NodeNotFoundException | DatanodeKeystoreException ex) {
            LOG.error("error generating a CSR: " + ex.getMessage(), ex);
            dataNodeProvisioningService.save(cfg.asError(ex.getMessage()));
        }
    }

    private void postCsrEvent(PKCS10CertificationRequest csr) throws IOException {
        clusterEventBus.post(CertificateSigningRequestEvent.fromCsr(nodeId.getNodeId(), csr));
        LOG.info("Posted CertificateSigningRequestEvent for node " + nodeId.getNodeId());
    }

    private DataNodeProvisioningConfig writeInitialProvisioningConfig() {
        LOG.info("Writing initial provisioning config for datanode " + nodeId.getNodeId());
        return dataNodeProvisioningService.save(DataNodeProvisioningConfig.builder()
                .nodeId(nodeId.getNodeId())
                .state(DataNodeProvisioningConfig.State.UNCONFIGURED)
                .build());
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
