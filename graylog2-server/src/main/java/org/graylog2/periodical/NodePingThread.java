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
package org.graylog2.periodical;

import org.graylog2.cluster.leader.LeaderElectionService;
import org.graylog2.cluster.nodes.NodeService;
import org.graylog2.cluster.nodes.ServerNodeDto;
import org.graylog2.configuration.HttpConfiguration;
import org.graylog2.plugin.ServerStatus;
import org.graylog2.plugin.Tools;
import org.graylog2.plugin.periodical.Periodical;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

@Singleton
public class NodePingThread extends Periodical {

    private static final Logger LOG = LoggerFactory.getLogger(NodePingThread.class);
    private final NodeService<ServerNodeDto> nodeService;
    private final HttpConfiguration httpConfiguration;
    private final ServerStatus serverStatus;
    private final LeaderElectionService leaderElectionService;

    @Inject
    public NodePingThread(NodeService<ServerNodeDto> nodeService,
                          HttpConfiguration httpConfiguration,
                          ServerStatus serverStatus, LeaderElectionService leaderElectionService) {
        this.nodeService = nodeService;
        this.httpConfiguration = httpConfiguration;
        this.serverStatus = serverStatus;
        this.leaderElectionService = leaderElectionService;
    }

    @Override
    // This method is "synchronized" because we are also calling it directly in AutomaticLeaderElectionService
    public synchronized void doRun() {
        final boolean isLeader = leaderElectionService.isLeader();
        ServerNodeDto dto = ServerNodeDto.Builder.builder()
                .setId(serverStatus.getNodeId().getNodeId())
                .setLeader(isLeader)
                .setTransportAddress(httpConfiguration.getHttpPublishUri().resolve(HttpConfiguration.PATH_API).toString())
                .setHostname(Tools.getLocalCanonicalHostname())
                .build();
        nodeService.ping(dto);
    }

    @Override
    @Nonnull
    protected Logger getLogger() {
        return LOG;
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
        return 1;
    }
}
