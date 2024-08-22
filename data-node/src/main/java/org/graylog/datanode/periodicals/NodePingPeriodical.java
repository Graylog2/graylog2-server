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
package org.graylog.datanode.periodicals;

import jakarta.inject.Inject;
import org.graylog.datanode.Configuration;
import org.graylog.datanode.configuration.DatanodeKeystore;
import org.graylog.datanode.opensearch.OpensearchProcess;
import org.graylog.datanode.opensearch.statemachine.OpensearchState;
import org.graylog2.cluster.nodes.DataNodeDto;
import org.graylog2.cluster.nodes.DataNodeStatus;
import org.graylog2.cluster.nodes.NodeService;
import org.graylog2.plugin.Version;
import org.graylog2.plugin.periodical.Periodical;
import org.graylog2.plugin.system.NodeId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.net.URI;
import java.util.Date;
import java.util.function.Supplier;

public class NodePingPeriodical extends Periodical {

    private static final Logger LOG = LoggerFactory.getLogger(NodePingPeriodical.class);
    private final NodeService<DataNodeDto> nodeService;
    private final NodeId nodeId;
    private final Supplier<URI> opensearchBaseUri;
    private final Supplier<String> opensearchClusterUri;
    private final Supplier<String> datanodeRestApiUri;
    private final Configuration configuration;
    private final Supplier<OpensearchState> processState;

    private final Supplier<Date> certValidUntil;

    private final Version version = Version.CURRENT_CLASSPATH;


    @Inject
    public NodePingPeriodical(NodeService<DataNodeDto> nodeService, NodeId nodeId, Configuration configuration, OpensearchProcess managedOpenSearch, DatanodeKeystore datanodeKeystore) {
        this(
                nodeService,
                nodeId,
                configuration,
                managedOpenSearch::getOpensearchBaseUrl,
                managedOpenSearch::getOpensearchClusterUrl,
                managedOpenSearch::getDatanodeRestApiUrl,
                () -> managedOpenSearch.processInfo().state(),
                datanodeKeystore::getCertificateExpiration
        );
    }

    NodePingPeriodical(
            NodeService<DataNodeDto> nodeService,
            NodeId nodeId,
            Configuration configuration,
            Supplier<URI> opensearchBaseUri,
            Supplier<String> opensearchClusterUri,
            Supplier<String> datanodeRestApiUri,
            Supplier<OpensearchState> processState,
            Supplier<Date> certValidUntil
    ) {
        this.nodeService = nodeService;
        this.nodeId = nodeId;
        this.opensearchBaseUri = opensearchBaseUri;
        this.opensearchClusterUri = opensearchClusterUri;
        this.datanodeRestApiUri = datanodeRestApiUri;
        this.configuration = configuration;
        this.processState = processState;
        this.certValidUntil = certValidUntil;
    }

    @Override
    public boolean runsForever() {
        return false;
    }

    @Override
    public boolean stopOnGracefulShutdown() {
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
        return 1;
    }

    @Nonnull
    @Override
    protected Logger getLogger() {
        return LOG;
    }

    @Override
    public void initialize() {
        registerServer();
    }

    @Override
    public void doRun() {
        final DataNodeDto dto = DataNodeDto.Builder.builder()
                .setId(nodeId.getNodeId())
                .setTransportAddress(opensearchBaseUri.get().toString())
                .setClusterAddress(opensearchClusterUri.get())
                .setDataNodeStatus(processState.get().getDataNodeStatus())
                .setHostname(configuration.getHostname())
                .setRestApiAddress(datanodeRestApiUri.get())
                .setCertValidUntil(certValidUntil.get())
                .setDatanodeVersion(version.getVersion().toString())
                .build();

        nodeService.ping(dto);

    }

    private void registerServer() {
        final boolean registrationSucceeded = nodeService.registerServer(DataNodeDto.Builder.builder()
                .setId(nodeId.getNodeId())
                .setTransportAddress(opensearchBaseUri.get().toString())
                .setClusterAddress(opensearchClusterUri.get())
                .setHostname(configuration.getHostname())
                .setDataNodeStatus(DataNodeStatus.STARTING)
                .setCertValidUntil(certValidUntil.get())
                .setDatanodeVersion(version.getVersion().toString())
                .build());

        if (!registrationSucceeded) {
            LOG.error("Failed to register node {} for heartbeats.", nodeId.getNodeId());
        }
    }
}
