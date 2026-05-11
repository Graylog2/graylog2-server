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
package org.graylog2.cluster.nodes;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import jakarta.inject.Inject;
import org.graylog2.cluster.NodeNotFoundException;
import org.graylog2.plugin.lifecycles.Lifecycle;
import org.graylog2.plugin.system.NodeId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Marks this node's row as offline in MongoDB when a {@link Lifecycle#HALTING} event is published.
 * This is a safeguard on top of {@link AbstractNodeService#dropOutdated()}: a graceful shutdown
 * flips the {@code online} flag immediately, so the cluster sees the node as offline without
 * waiting for the heartbeat timeout to expire.
 *
 * <p>The {@code HALTING} event is fired during {@code GracefulShutdown.doRun()} well before the
 * MongoDB connection is shut down, so the write is reliable. Any failure is swallowed: if Mongo
 * is unreachable for any reason, we don't want to block shutdown.</p>
 */
public class ServerNodeShutdownListener {
    private static final Logger LOG = LoggerFactory.getLogger(ServerNodeShutdownListener.class);

    private final NodeService<ServerNodeDto> nodeService;
    private final NodeId nodeId;

    @Inject
    public ServerNodeShutdownListener(EventBus eventBus,
                                      NodeService<ServerNodeDto> nodeService,
                                      NodeId nodeId) {
        this.nodeService = nodeService;
        this.nodeId = nodeId;
        eventBus.register(this);
    }

    @Subscribe
    public void onLifecycle(Lifecycle lifecycle) {
        if (lifecycle != Lifecycle.HALTING) {
            return;
        }
        try {
            final ServerNodeDto current = nodeService.byNodeIdAnyState(nodeId.getNodeId())
                    .orElseThrow(() -> new NodeNotFoundException("Node " + nodeId.getNodeId() + " not registered."));
            nodeService.update(current.offline());
        } catch (NodeNotFoundException e) {
            LOG.debug("Node {} not registered in database during shutdown; skipping offline marker.", nodeId.getNodeId());
        } catch (Exception e) {
            LOG.warn("Failed to mark node {} offline during shutdown.", nodeId.getNodeId(), e);
        }
    }
}
