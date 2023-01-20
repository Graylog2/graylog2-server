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
package org.graylog.datanode.process;

import org.graylog.datanode.management.ManagedNodes;
import org.graylog2.plugin.periodical.Periodical;
import org.opensearch.action.admin.cluster.health.ClusterHealthRequest;
import org.opensearch.action.admin.cluster.health.ClusterHealthResponse;
import org.opensearch.client.RequestOptions;
import org.opensearch.cluster.health.ClusterHealthStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.IOException;

@Singleton
public class OpensearchHeartbeat extends Periodical {

    private static final Logger LOG = LoggerFactory.getLogger(OpensearchHeartbeat.class);
    private final ManagedNodes managedOpenSearch;

    @Inject
    public OpensearchHeartbeat(ManagedNodes managedOpenSearch) {
        this.managedOpenSearch = managedOpenSearch;
    }

    private void onClusterStatus(OpensearchProcess process, ClusterHealthResponse health) {
        final ClusterHealthStatus status = health.getStatus();
        switch (status) {
            case GREEN -> process.onEvent(ProcessEvent.HEALTH_CHECK_OK);
            case YELLOW -> process.onEvent(ProcessEvent.HEALTH_CHECK_OK);
            case RED -> process.onEvent(ProcessEvent.HEALTH_CHECK_FAILED);
        }
        process.setLeaderNode(health.hasDiscoveredClusterManager());
    }

    private void onRestError(OpensearchProcess process, IOException e) {
        process.onEvent(ProcessEvent.HEALTH_CHECK_FAILED);
        LOG.warn("Opensearch REST api of process {} unavailable. Cause: {}", process.getProcessInfo().pid(), e.getMessage());
    }

    @Override
    // This method is "synchronized" because we are also calling it directly in AutomaticLeaderElectionService
    public synchronized void doRun() {
        managedOpenSearch.getProcesses()
                .stream()
                .filter(p -> p.getStatus() != ProcessState.TERMINATED)
                .forEach(process -> {
                    try {
                        final ClusterHealthRequest req = new ClusterHealthRequest();
                        final ClusterHealthResponse health = process.getRestClient()
                                .cluster()
                                .health(req, RequestOptions.DEFAULT);
                        onClusterStatus(process, health);
                    } catch (IOException e) {
                        onRestError(process, e);
                    }
                });
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
