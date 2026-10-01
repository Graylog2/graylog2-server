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
import jakarta.inject.Singleton;
import org.graylog.datanode.opensearch.OpensearchProcess;
import org.graylog.datanode.opensearch.bootstrap.ClusterBootstrapService;
import org.graylog.datanode.opensearch.statemachine.OpensearchEvent;
import org.graylog.datanode.opensearch.statemachine.OpensearchState;
import org.graylog2.plugin.periodical.Periodical;
import org.opensearch.client.opensearch.core.InfoResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.Set;

@Singleton
public class OpensearchNodeHeartbeat extends Periodical {

    private static final Logger LOG = LoggerFactory.getLogger(OpensearchNodeHeartbeat.class);
    /**
     * States in which the opensearch process is running, even if its REST API isn't responding
     */
    private static final Set<OpensearchState> PROCESS_RUNNING_STATES = Set.of(
            OpensearchState.STARTING, OpensearchState.AVAILABLE, OpensearchState.NOT_RESPONDING, OpensearchState.FAILED);

    private final OpensearchProcess process;
    private final ClusterBootstrapService clusterBootstrapService;

    @Inject
    public OpensearchNodeHeartbeat(OpensearchProcess process, ClusterBootstrapService clusterBootstrapService) {
        this.process = process;
        this.clusterBootstrapService = clusterBootstrapService;
    }

    @Override
    // This method is "synchronized" because we are also calling it directly in AutomaticLeaderElectionService
    public synchronized void doRun() {
        if (!process.isInState(OpensearchState.TERMINATED) && !process.isInState(OpensearchState.WAITING_FOR_CONFIGURATION)
                && !process.isInState(OpensearchState.REMOVED)) {

            renewBootstrapClaim();
            process.openSearchClient().ifPresent(client -> {
                try {
                    final InfoResponse info = client.syncWithoutErrorMapping().info();
                    onNodeResponse(process);
                    recordClusterUuid(info);
                } catch (IOException e) {
                    onRestError(process, e);
                }
            });
        }
    }

    private void onNodeResponse(OpensearchProcess process) {
        process.onEvent(OpensearchEvent.HEALTH_CHECK_OK);
    }

    /**
     * Renewed before the REST call, a slow or unresponsive opensearch may still form the cluster as long as the process runs.
     */
    private void renewBootstrapClaim() {
        if (PROCESS_RUNNING_STATES.stream().anyMatch(process::isInState)) {
            try {
                clusterBootstrapService.renewClaim();
            } catch (Exception e) {
                LOG.warn("Failed to renew opensearch cluster bootstrap claim: {}", e.getMessage());
            }
        }
    }

    private void recordClusterUuid(InfoResponse info) {
        try {
            clusterBootstrapService.recordClusterUuid(info.clusterUuid());
        } catch (Exception e) {
            LOG.warn("Failed to record opensearch cluster UUID {}: {}", info.clusterUuid(), e.getMessage());
        }
    }

    private void onRestError(OpensearchProcess process, Exception e) {
        process.onEvent(OpensearchEvent.HEALTH_CHECK_FAILED);
        LOG.warn("Opensearch REST api of process {} unavailable. Cause: {}", process.processInfo().process().pid(), e.getMessage());
    }

    @Nonnull
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
