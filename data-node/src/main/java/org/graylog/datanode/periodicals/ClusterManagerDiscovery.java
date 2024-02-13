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
import org.graylog.datanode.Configuration;
import org.graylog.datanode.configuration.DatanodeConfiguration;
import org.graylog.datanode.management.OpensearchProcess;
import org.graylog.datanode.process.ProcessState;
import org.graylog.shaded.opensearch2.org.opensearch.client.Request;
import org.graylog.shaded.opensearch2.org.opensearch.client.Response;
import org.graylog.shaded.opensearch2.org.opensearch.client.RestHighLevelClient;
import org.graylog2.plugin.periodical.Periodical;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import java.io.IOException;
import java.util.Optional;

@Singleton
public class ClusterManagerDiscovery extends Periodical {

    private static final Logger LOG = LoggerFactory.getLogger(ClusterManagerDiscovery.class);
    private final DatanodeConfiguration datanodeConfiguration;
    private final OpensearchProcess managedOpenSearch;
    private final ObjectMapper objectMapper;
    private final Configuration configuration;

    @Inject
    public ClusterManagerDiscovery(DatanodeConfiguration datanodeConfiguration, OpensearchProcess managedOpenSearch, ObjectMapper objectMapper, Configuration configuration) {
        this.datanodeConfiguration = datanodeConfiguration;
        this.managedOpenSearch = managedOpenSearch;
        this.objectMapper = objectMapper;
        this.configuration = configuration;
    }

    @Override
    // This method is "synchronized" because we are also calling it directly in AutomaticLeaderElectionService
    public synchronized void doRun() {
        if (managedOpenSearch.isInState(ProcessState.AVAILABLE)) {
            final Boolean isManagerNode = getClusterStateResponse(managedOpenSearch)
                    .map(r -> r.nodes().get(r.clusterManagerNode()))
                    .map(managerNode -> configuration.getDatanodeNodeName().equals(managerNode.name()))
                    .orElse(false);
            managedOpenSearch.setLeaderNode(isManagerNode);
        }
    }


    private Optional<ClusterStateResponse> getClusterStateResponse(OpensearchProcess process) {
        return process.restClient().flatMap(this::requestClusterState);
    }

    private Optional<ClusterStateResponse> requestClusterState(RestHighLevelClient client) {
        try {
            final Response response = client.getLowLevelClient().performRequest(new Request("GET", "_cluster/state/"));
            final ClusterStateResponse state = objectMapper.readValue(response.getEntity().getContent(), ClusterStateResponse.class);
            return Optional.of(state);
        } catch (IOException e) {
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
