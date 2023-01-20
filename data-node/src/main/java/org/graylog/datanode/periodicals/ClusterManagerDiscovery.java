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

import com.fasterxml.jackson.databind.ObjectMapper;
import org.graylog.datanode.management.ManagedNodes;
import org.graylog.datanode.process.OpensearchProcess;
import org.graylog.datanode.process.ProcessState;
import org.graylog2.plugin.periodical.Periodical;
import org.opensearch.client.Request;
import org.opensearch.client.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Optional;

@Singleton
public class ClusterManagerDiscovery extends Periodical {

    private static final Logger LOG = LoggerFactory.getLogger(ClusterManagerDiscovery.class);
    private final ManagedNodes managedOpenSearch;
    private final ObjectMapper objectMapper;

    @Inject
    public ClusterManagerDiscovery(ManagedNodes managedOpenSearch, ObjectMapper objectMapper) {
        this.managedOpenSearch = managedOpenSearch;
        this.objectMapper = objectMapper;
    }

    @Override
    // This method is "synchronized" because we are also calling it directly in AutomaticLeaderElectionService
    public synchronized void doRun() {

        managedOpenSearch.getProcesses()
                .stream()
                .filter(p -> p.getStatus() == ProcessState.AVAILABLE)
                .flatMap(p -> getClusterStateResponse(p).stream())
                .findFirst()
                .ifPresent(clusterStateResponse -> {
                    final ClusterStateResponse.NodeState managerNode = clusterStateResponse.nodes().get(clusterStateResponse.clusterManagerNode());
                    managedOpenSearch.getProcesses().forEach(p -> p.setLeaderNode(p.getNodeName().equals(managerNode.name())));
                });

    }


    private Optional<ClusterStateResponse> getClusterStateResponse(OpensearchProcess process) {
        try {
            final Response response = process.getRestClient()
                    .getLowLevelClient()
                    .performRequest(new Request("GET", "_cluster/state/"));

            final ClusterStateResponse clusterState = objectMapper.readValue(response.getEntity().getContent(), ClusterStateResponse.class);
            return Optional.of(clusterState);
        } catch (Exception e) {
            LOG.warn("Failed to obtain cluster state response", e);
            return Optional.empty();
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
        return 10;
    }
}
