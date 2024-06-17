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
package org.graylog2.datanode;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import jakarta.inject.Inject;
import org.graylog2.cluster.NodeNotFoundException;
import org.graylog2.cluster.nodes.DataNodeDto;
import org.graylog2.cluster.nodes.DataNodeStatus;
import org.graylog2.cluster.nodes.NodeService;
import org.graylog2.events.ClusterEventBus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DataNodeCommandServiceImpl implements DataNodeCommandService {

    private static final Logger LOG = LoggerFactory.getLogger(DataNodeCommandServiceImpl.class);

    private final ClusterEventBus clusterEventBus;
    private final NodeService<DataNodeDto> nodeService;

    @Inject
    public DataNodeCommandServiceImpl(ClusterEventBus clusterEventBus, NodeService<DataNodeDto> nodeService, EventBus eventBus) {
        this.clusterEventBus = clusterEventBus;
        this.nodeService = nodeService;
        eventBus.register(this);
    }

    @Override
    public DataNodeDto removeNode(String nodeId) throws NodeNotFoundException {
        final DataNodeDto node = nodeService.byNodeId(nodeId);
        if (node.getDataNodeStatus() != DataNodeStatus.AVAILABLE) {
            throw new IllegalArgumentException("Only running data nodes can be removed from the cluster.");
        }
        if (nodeService.allActive().values().stream()
                .filter(n -> n.getDataNodeStatus() == DataNodeStatus.AVAILABLE && n.getActionQueue() == null)
                .count() <= 1) {
            throw new IllegalArgumentException("Cannot remove last data node in the cluster.");
        }
        DataNodeLifecycleTrigger trigger = DataNodeLifecycleTrigger.REMOVE;
        DataNodeStatus lockingStatus = DataNodeStatus.REMOVING;
        addToQueue(node, trigger, lockingStatus);
        return node;
    }

    @Override
    public DataNodeDto resetNode(String nodeId) throws NodeNotFoundException {
        final DataNodeDto node = nodeService.byNodeId(nodeId);
        if (node.getDataNodeStatus() != DataNodeStatus.REMOVED) {
            throw new IllegalArgumentException("Only previously removed data nodes can rejoin the cluster.");
        }
        DataNodeLifecycleEvent e = DataNodeLifecycleEvent.create(node.getNodeId(), DataNodeLifecycleTrigger.RESET);
        clusterEventBus.post(e);
        return node;
    }


    @Override
    public DataNodeDto triggerCertificateSigningRequest(String nodeId, DatanodeStartType startType) throws NodeNotFoundException {
        final DataNodeDto node = nodeService.byNodeId(nodeId);

        DataNodeLifecycleEvent e = switch (startType) {
            case AUTOMATICALLY -> DataNodeLifecycleEvent.create(node.getNodeId(), DataNodeLifecycleTrigger.REQUEST_CSR_WITH_AUTOSTART);
            case MANUALLY -> DataNodeLifecycleEvent.create(node.getNodeId(), DataNodeLifecycleTrigger.REQUEST_CSR);
        };

        clusterEventBus.post(e);
        return node;
    }

    @Override
    public DataNodeDto stopNode(String nodeId) throws NodeNotFoundException {
        final DataNodeDto node = nodeService.byNodeId(nodeId);
        if (node.getDataNodeStatus() != DataNodeStatus.AVAILABLE) {
            throw new IllegalArgumentException("Only running data nodes can be stopped.");
        }
        DataNodeLifecycleEvent e = DataNodeLifecycleEvent.create(node.getNodeId(), DataNodeLifecycleTrigger.STOP);
        clusterEventBus.post(e);
        return node;
    }

    @Override
    public DataNodeDto startNode(String nodeId) throws NodeNotFoundException {
        final DataNodeDto node = nodeService.byNodeId(nodeId);

        if (node.getDataNodeStatus() != DataNodeStatus.UNAVAILABLE && node.getDataNodeStatus() != DataNodeStatus.PREPARED) {
            throw new IllegalArgumentException("Only stopped data nodes can be started.");
        }
        DataNodeLifecycleEvent e = DataNodeLifecycleEvent.create(node.getNodeId(), DataNodeLifecycleTrigger.START);
        clusterEventBus.post(e);
        return node;
    }

    private void addToQueue(DataNodeDto node, DataNodeLifecycleTrigger trigger, DataNodeStatus lockingStatus) {
        nodeService.update(node.toBuilder()
                .setActionQueue(trigger)
                .build());
        if (!otherNodeHasStatus(node.getNodeId(), lockingStatus, trigger)) { // post event to bus if no other node is currently performing or waiting
            DataNodeLifecycleEvent e = DataNodeLifecycleEvent.create(node.getNodeId(), trigger);
            clusterEventBus.post(e);
        }
    }

    private boolean otherNodeHasStatus(String nodeId, DataNodeStatus status, DataNodeLifecycleTrigger trigger) {
        return nodeService.allActive().values().stream()
                .anyMatch(n ->
                        !n.getNodeId().equals(nodeId) &&
                                (n.getDataNodeStatus() == status || n.getActionQueue() == trigger)
                );
    }

    @Subscribe
    @SuppressWarnings("unused")
    public void handleDataNodeLifeCycleEvent(DataNodeLifecycleEvent event) {
        switch (event.trigger()) {
            case REMOVED -> handleNextNode(DataNodeLifecycleTrigger.REMOVE);
            case STOPPED -> handleNextNode(DataNodeLifecycleTrigger.STOP);
        }
    }

    private void handleNextNode(DataNodeLifecycleTrigger trigger) {
        nodeService.allActive().values().stream()
                .filter(node -> node.getActionQueue() == trigger)
                .findFirst().ifPresent(node -> {
                    clusterEventBus.post(DataNodeLifecycleEvent.create(node.getNodeId(), trigger));
                });
    }

}
