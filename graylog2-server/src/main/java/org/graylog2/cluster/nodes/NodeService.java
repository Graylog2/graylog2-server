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

import org.graylog2.cluster.Node;
import org.graylog2.cluster.NodeNotFoundException;
import org.graylog2.plugin.system.NodeId;

import java.net.URI;
import java.util.Collection;
import java.util.Map;

public interface NodeService<T extends Node> {
    
    boolean registerServer(String nodeId, boolean isLeader, URI httpPublishUri, String clusterUri, String hostname);

    default boolean registerServer(String nodeId, boolean isLeader, URI httpPublishUri, String hostname) {
        return registerServer(nodeId, isLeader, httpPublishUri, null, hostname);
    }

    T byNodeId(String nodeId) throws NodeNotFoundException;

    T byNodeId(NodeId nodeId) throws NodeNotFoundException;

    Map<String, T> byNodeIds(Collection<String> nodeIds);

    Map<String, T> allActive();

    void dropOutdated();

    void markAsAlive(NodeId node, boolean isLeader, URI restTransportAddress, String clusterAddress, DataNodeStatus dataNodeStatus) throws NodeNotFoundException;

    default void markAsAlive(NodeId node, boolean isLeader, URI restTransportAddress) throws NodeNotFoundException {
        markAsAlive(node, isLeader, restTransportAddress, null, null);
    }

    boolean isOnlyLeader(NodeId nodeIde);

    boolean isAnyLeaderPresent();
}
