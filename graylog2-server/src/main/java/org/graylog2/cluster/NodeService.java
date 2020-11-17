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
    String registerServer(String nodeId, boolean isMaster, URI httpPublishUri, String hostname);

    Node byNodeId(String nodeId) throws NodeNotFoundException;

    Node byNodeId(NodeId nodeId) throws NodeNotFoundException;

    Map<String, Node> allActive(Node.Type type);

    Map<String, Node> allActive();

    void dropOutdated();

    void markAsAlive(Node node, boolean isMaster, String restTransportAddress);

    void markAsAlive(Node node, boolean isMaster, URI restTransportAddress);

    boolean isOnlyMaster(NodeId nodeIde);

    boolean isAnyMasterPresent();
}
