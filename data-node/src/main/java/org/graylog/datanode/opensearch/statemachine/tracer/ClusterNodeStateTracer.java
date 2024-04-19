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
package org.graylog.datanode.opensearch.statemachine.tracer;

import com.google.inject.Inject;
import org.graylog.datanode.opensearch.statemachine.OpensearchEvent;
import org.graylog.datanode.opensearch.statemachine.OpensearchState;
import org.graylog2.cluster.NodeNotFoundException;
import org.graylog2.cluster.nodes.DataNodeDto;
import org.graylog2.cluster.nodes.NodeService;
import org.graylog2.datanode.DataNodeLifecycleTrigger;
import org.graylog2.plugin.system.NodeId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ClusterNodeStateTracer implements StateMachineTracer {

    private final Logger log = LoggerFactory.getLogger(ClusterNodeStateTracer.class);

    private final NodeService<DataNodeDto> nodeService;
    private final NodeId nodeId;

    @Inject
    public ClusterNodeStateTracer(NodeService<DataNodeDto> nodeService, NodeId nodeId) {
        this.nodeService = nodeService;
        this.nodeId = nodeId;
    }

    @Override
    public void trigger(OpensearchEvent processEvent) {
    }

    @Override
    public void transition(OpensearchEvent processEvent, OpensearchState source, OpensearchState destination) {
        try {
            if (!source.equals(destination)) {
                log.info("Updating cluster node {} from {} to {} (reason: {})", nodeId.getNodeId(),
                        source.getDataNodeStatus(), destination.getDataNodeStatus(), processEvent.name());
                DataNodeDto node = nodeService.byNodeId(nodeId);
                nodeService.update(node.toBuilder()
                        .setDataNodeStatus(destination.getDataNodeStatus())
                        .setActionQueue(DataNodeLifecycleTrigger.CLEAR)
                        .build());
            }
        } catch (NodeNotFoundException e) {
            throw new RuntimeException("Node not registered, this should not happen.");
        }
    }
}
