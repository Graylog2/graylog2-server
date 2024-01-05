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
package org.graylog2.cluster;


import org.graylog2.plugin.system.NodeId;

import java.net.URI;
import java.util.Collection;
import java.util.Map;

/**
 * @deprecated Please use the generic org.graylog2.cluster.nodes.NodeService specifying the type of node
 * to be returned (either server or data node).
 */
@Deprecated(since = "6.0")
public interface NodeService {

    boolean registerServer(String nodeId, boolean isLeader, URI httpPublishUri, String clusterUri, String hostname);

    default boolean registerServer(String nodeId, boolean isLeader, URI httpPublishUri, String hostname) {
        return registerServer(nodeId, isLeader, httpPublishUri, null, hostname);
    }

    Node byNodeId(String nodeId) throws NodeNotFoundException;

    Node byNodeId(NodeId nodeId) throws NodeNotFoundException;

    Map<String, Node> byNodeIds(Collection<String> nodeIds);

    Map<String, Node> allActive();

    boolean isAnyLeaderPresent();

}
