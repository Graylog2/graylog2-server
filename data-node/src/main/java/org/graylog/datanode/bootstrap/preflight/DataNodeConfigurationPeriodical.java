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
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;
import org.graylog.datanode.configuration.DatanodeKeystore;
import org.graylog.datanode.configuration.DatanodeKeystoreException;
import org.graylog.security.certutil.cert.CertificateChain;
import org.graylog.security.certutil.csr.exceptions.CSRGenerationException;
import org.graylog2.cluster.NodeNotFoundException;
import org.graylog2.cluster.certificates.CertificateExchange;
import org.graylog2.cluster.certificates.CertificateSigningRequest;
import org.graylog2.cluster.nodes.DataNodeDto;
import org.graylog2.cluster.nodes.NodeService;
import org.graylog2.cluster.preflight.DataNodeProvisioningConfig;
import org.graylog2.cluster.preflight.DataNodeProvisioningService;
import org.graylog2.plugin.periodical.Periodical;
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
public class DataNodeConfigurationPeriodical extends Periodical {
    private static final Logger LOG = LoggerFactory.getLogger(DataNodeConfigurationPeriodical.class);

    private final DataNodeProvisioningService dataNodeProvisioningService;
    private final NodeService<DataNodeDto> nodeService;
    private final NodeId nodeId;

    private final CertificateExchange certificateExchange;
    private final DatanodeKeystore datanodeKeystore;

    @Inject
    public DataNodeConfigurationPeriodical(final DataNodeProvisioningService dataNodeProvisioningService,
                                           final NodeService<DataNodeDto> nodeService,
                                           final NodeId nodeId,
                                           CertificateExchange certificateExchange,
                                           DatanodeKeystore datanodeKeystore) {
        this.dataNodeProvisioningService = dataNodeProvisioningService;
        this.nodeService = nodeService;
        this.nodeId = nodeId;
        this.certificateExchange = certificateExchange;
        this.datanodeKeystore = datanodeKeystore;
    }

    @Override
    public void doRun() {
        LOG.debug("checking if this DataNode is supposed to take configuration steps.");
        var cfg = dataNodeProvisioningService.getPreflightConfigFor(nodeId.getNodeId());
        if (cfg.isEmpty()) {
            // write default config if none exists for this node
            writeInitialProvisioningConfig();
            return;
        }
        cfg.ifPresent(c -> {
            final var state = c.state();
            if (state == null) {
                return;
            }

            // always check if there are any certificates that we can accept
            certificateExchange.pollCertificate(nodeId.getNodeId(), this::processCertificateChain);

            switch (state) {
                case CONFIGURED -> writeCsr(c);
                case STARTUP_TRIGGER ->
                        dataNodeProvisioningService.changeState(nodeId.getNodeId(), DataNodeProvisioningConfig.State.STARTUP_REQUESTED);
            }
        });
    }

    private void processCertificateChain(CertificateChain certificateChain) {
        try {
            datanodeKeystore.replaceCertificatesInKeystore(certificateChain);
            dataNodeProvisioningService.changeState(nodeId.getNodeId(), DataNodeProvisioningConfig.State.STORED);
        } catch (DatanodeKeystoreException e) {
            throw new RuntimeException(e);
        }
    }

    private void writeCsr(DataNodeProvisioningConfig cfg) {
        try {
            final var node = nodeService.byNodeId(nodeId);
            final var altNames = ImmutableList.<String>builder()
                    .addAll(Optional.ofNullable(cfg.altNames()).orElse(Collections.emptyList()))
                    .addAll(determineAltNames())
                    .build();
            final PKCS10CertificationRequest csr = datanodeKeystore.createCertificateSigningRequest(node.getHostname(), altNames);
            certificateExchange.requestCertificate(new CertificateSigningRequest(nodeId.getNodeId(), csr));
            LOG.info("created CSR for this node");
        } catch (CSRGenerationException | IOException | NodeNotFoundException | DatanodeKeystoreException ex) {
            LOG.error("error generating a CSR: " + ex.getMessage(), ex);
            dataNodeProvisioningService.save(cfg.asError(ex.getMessage()));
        }
    }

    private void writeInitialProvisioningConfig() {
        dataNodeProvisioningService.save(DataNodeProvisioningConfig.builder()
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

    @Override
    protected Logger getLogger() {
        return LOG;
    }

    @Override
    public boolean runsForever() {
        return false;
    }

    @Override
    public boolean stopOnGracefulShutdown() {
        return true;
    }

    @Override
    public boolean leaderOnly() {
        return false;
    }

    @Override
    public boolean startOnThisNode() {
        return true;
    }

    @Override
    public boolean isDaemon() {
        return true;
    }

    @Override
    public int getInitialDelaySeconds() {
        return 0;
    }

    @Override
    public int getPeriodSeconds() {
        return 2;
    }
}
