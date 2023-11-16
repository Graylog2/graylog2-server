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

import org.graylog.datanode.Configuration;
import org.graylog.datanode.management.OpensearchProcess;
import org.graylog.datanode.process.ProcessState;
import org.graylog2.cluster.NodeNotFoundException;
import org.graylog2.cluster.NodeService;
import org.graylog2.plugin.periodical.Periodical;
import org.graylog2.plugin.system.NodeId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import java.net.URI;
import java.util.function.Supplier;

public class NodePingPeriodical extends Periodical {

    private static final Logger LOG = LoggerFactory.getLogger(NodePingPeriodical.class);
    private final NodeService nodeService;
    private final NodeId nodeId;
    private final Supplier<URI> opensearchBaseUri;
    private final Supplier<String> opensearchClusterUri;
    private final Supplier<Boolean> isLeader;
    private final Configuration configuration;
    private final Supplier<ProcessState> processState;


    @Inject
    public NodePingPeriodical(NodeService nodeService, NodeId nodeId, Configuration configuration, OpensearchProcess managedOpenSearch) {
        this(nodeService, nodeId, configuration, managedOpenSearch::getOpensearchBaseUrl, managedOpenSearch::getOpensearchClusterUrl, managedOpenSearch::isLeaderNode, () -> managedOpenSearch.processInfo().state());
    }

    NodePingPeriodical(
            NodeService nodeService,
            NodeId nodeId,
            Configuration configuration,
            Supplier<URI> opensearchBaseUri,
            Supplier<String> opensearchClusterUri,
            Supplier<Boolean> isLeader,
            Supplier<ProcessState> processState
    ) {
        this.nodeService = nodeService;
        this.nodeId = nodeId;
        this.opensearchBaseUri = opensearchBaseUri;
        this.opensearchClusterUri = opensearchClusterUri;
        this.isLeader = isLeader;
        this.configuration = configuration;
        this.processState = processState;
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
        try {
            nodeService.markAsAlive(nodeId, isLeader.get(), opensearchBaseUri.get(), opensearchClusterUri.get(), processState.get().getDataNodeStatus());
        } catch (NodeNotFoundException e) {
            LOG.warn("Did not find meta info of this node. Re-registering.");
            registerServer();
        }
    }

    private void registerServer() {
        final boolean registrationSucceeded = nodeService.registerServer(nodeId.getNodeId(),
                isLeader.get(),
                opensearchBaseUri.get(),
                opensearchClusterUri.get(),
                configuration.getHostname());

        if (!registrationSucceeded) {
            LOG.error("Failed to register node {} for heartbeats.", nodeId.getNodeId());
        }
    }
}
