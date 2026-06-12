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
package org.graylog.datanode.shutdown;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.graylog2.cluster.NodeNotFoundException;
import org.graylog2.cluster.nodes.DataNodeDto;
import org.graylog2.cluster.nodes.NodeService;
import org.graylog2.plugin.system.NodeId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Marks this DataNode's row as offline in MongoDB during graceful shutdown.
 * <p>
 * This is a safeguard on top of {@code AbstractNodeService.dropOutdated()}: it flips the
 * {@code online} flag immediately so the cluster sees the node as offline without waiting for the
 * heartbeat timeout to expire.
 * <p>
 * In the DataNode shutdown sequence, {@link GracefulShutdownService} runs and awaits all registered
 * {@link GracefulShutdownHook}s before {@code ServiceManager} stops the {@code MongoConnection},
 * so the write here is reliable. Failures are swallowed: the {@code dropOutdated} mechanism is the
 * safety net, and we don't want to block shutdown.
 */
@Singleton
public class DataNodeShutdownListener implements GracefulShutdownHook {
    private static final Logger LOG = LoggerFactory.getLogger(DataNodeShutdownListener.class);

    private final NodeService<DataNodeDto> nodeService;
    private final NodeId nodeId;

    @Inject
    public DataNodeShutdownListener(GracefulShutdownService shutdownService,
                                    NodeService<DataNodeDto> nodeService,
                                    NodeId nodeId) {
        this.nodeService = nodeService;
        this.nodeId = nodeId;
        shutdownService.register(this);
    }

    @Override
    public void doGracefulShutdown() {
        try {
            final DataNodeDto current = nodeService.byNodeIdAnyState(nodeId.getNodeId())
                    .orElseThrow(() -> new NodeNotFoundException("Node " + nodeId.getNodeId() + " not registered."));
            nodeService.update(current.offline());
        } catch (NodeNotFoundException e) {
            LOG.debug("DataNode {} not registered in database during shutdown; skipping offline marker.", nodeId.getNodeId());
        } catch (Exception e) {
            LOG.warn("Failed to mark DataNode {} offline during shutdown.", nodeId.getNodeId(), e);
        }
    }
}
