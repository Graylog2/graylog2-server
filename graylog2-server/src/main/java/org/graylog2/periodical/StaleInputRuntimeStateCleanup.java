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

import jakarta.inject.Inject;
import org.graylog2.cluster.NodeService;
import org.graylog2.inputs.persistence.InputStateService;
import org.graylog2.plugin.periodical.Periodical;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;

/**
 * Periodically removes runtime state documents for nodes that are no longer active in the cluster.
 * Runs on the leader node only.
 */
public class StaleInputRuntimeStateCleanup extends Periodical {
    private static final Logger LOG = LoggerFactory.getLogger(StaleInputRuntimeStateCleanup.class);

    private final InputStateService runtimeStateService;
    private final NodeService nodeService;

    @Inject
    public StaleInputRuntimeStateCleanup(InputStateService runtimeStateService,
                                         NodeService nodeService) {
        this.runtimeStateService = runtimeStateService;
        this.nodeService = nodeService;
    }

    @Override
    public void doRun() {
        try {
            final Set<String> activeNodeIds = nodeService.allActive().keySet();
            final Set<String> stateNodeIds = runtimeStateService.getDistinctNodeIds();

            for (String nodeId : stateNodeIds) {
                if (!activeNodeIds.contains(nodeId)) {
                    LOG.debug("Cleaning up stale runtime state documents for inactive node {}", nodeId);
                    runtimeStateService.removeAllForNode(nodeId);
                }
            }
        } catch (Exception e) {
            LOG.warn("Error during stale input runtime state cleanup: {}", e.getMessage());
            LOG.debug("Exception details:", e);
        }
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
        return true;
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
        return 60;
    }

    @Override
    public int getPeriodSeconds() {
        return 60;
    }

    @Override
    protected Logger getLogger() {
        return LOG;
    }
}
