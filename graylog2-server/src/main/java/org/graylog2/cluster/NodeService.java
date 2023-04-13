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

import org.graylog2.plugin.database.PersistedService;
import org.graylog2.plugin.system.NodeId;

import java.net.URI;
import java.util.Map;

public interface NodeService extends PersistedService {
    Node.Type type();

    boolean registerServer(String nodeId, boolean isLeader, URI httpPublishUri, String hostname);

    Node byNodeId(String nodeId) throws NodeNotFoundException;

    Node byNodeId(NodeId nodeId) throws NodeNotFoundException;

    Map<String, Node> allActive(Node.Type type);

    /**
     * Please use the {@link #allActive(Node.Type)} method and provide explicit type of the node. Otherwise,
     * the implementation will fall back to {@link #type()} and provide only nodes of this type.
     */
    @Deprecated
    Map<String, Node> allActive();

    void dropOutdated();

    void markAsAlive(NodeId node, boolean isLeader, URI restTransportAddress) throws NodeNotFoundException;

    boolean isOnlyLeader(NodeId nodeIde);

    boolean isAnyLeaderPresent();
}
