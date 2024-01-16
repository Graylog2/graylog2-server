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

import org.graylog2.cluster.NodeNotFoundException;
import org.graylog2.cluster.nodes.DataNodeDto;
import org.graylog2.cluster.nodes.DataNodeStatus;
import org.graylog2.cluster.nodes.NodeService;
import org.graylog2.events.ClusterEventBus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.inject.Inject;

public class DataNodeServiceImpl implements DataNodeService {

    private static final Logger LOG = LoggerFactory.getLogger(DataNodeServiceImpl.class);

    private final ClusterEventBus clusterEventBus;
    private final NodeService<DataNodeDto> nodeService;

    @Inject
    public DataNodeServiceImpl(ClusterEventBus clusterEventBus, NodeService<DataNodeDto> nodeService) {
        this.clusterEventBus = clusterEventBus;
        this.nodeService = nodeService;
    }

    @Override
    public DataNodeDto removeNode(String nodeId) throws NodeNotFoundException {
        final DataNodeDto node = nodeService.byNodeId(nodeId);
        if (nodeService.allActive().size() <= 1) {
            throw new IllegalArgumentException("Cannot remove last data node in the cluster.");
        }
        if (nodeService.allActive().values().stream().anyMatch(n -> n.getDataNodeStatus() == DataNodeStatus.REMOVING)) {
            throw new IllegalArgumentException("Only one data node can be removed at a time.");
        }
        if (node.getDataNodeStatus() != DataNodeStatus.AVAILABLE) {
            throw new IllegalArgumentException("Only running data nodes can be removed from the cluster.");
        }
        DataNodeLifecycleEvent e = DataNodeLifecycleEvent.create(node.getNodeId(), DataNodeLifecycleTrigger.REMOVE);
        clusterEventBus.post(e);
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
        if (node.getDataNodeStatus() != DataNodeStatus.UNAVAILABLE) {
            throw new IllegalArgumentException("Only stopped data nodes can be started.");
        }
        DataNodeLifecycleEvent e = DataNodeLifecycleEvent.create(node.getNodeId(), DataNodeLifecycleTrigger.START);
        clusterEventBus.post(e);
        return node;
    }

}
