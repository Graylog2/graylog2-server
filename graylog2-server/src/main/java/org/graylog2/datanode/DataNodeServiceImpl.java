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

import org.graylog2.cluster.Node;
import org.graylog2.cluster.NodeNotFoundException;
import org.graylog2.cluster.nodes.DataNodeDto;
import org.graylog2.cluster.nodes.NodeService;
import org.graylog2.events.ClusterEventBus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;

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
    public void removeNode(String nodeId) throws NodeNotFoundException {
        final Node node = nodeService.byNodeId(nodeId);
        DataNodeLifecycleEvent e = DataNodeLifecycleEvent.create(node.getNodeId(), DataNodeLifecycleTrigger.REMOVE);
        clusterEventBus.post(e);
    }

    @Override
    public void resetNode(String nodeId) throws NodeNotFoundException {
        final Node node = nodeService.byNodeId(nodeId);
        DataNodeLifecycleEvent e = DataNodeLifecycleEvent.create(node.getNodeId(), DataNodeLifecycleTrigger.RESET);
        clusterEventBus.post(e);
    }

}
