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

import com.google.common.collect.Maps;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import com.mongodb.WriteResult;
import org.bson.types.ObjectId;
import org.graylog2.Configuration;
import org.graylog2.database.MongoConnection;
import org.graylog2.database.PersistedServiceImpl;
import org.graylog2.plugin.Tools;
import org.graylog2.plugin.database.ValidationException;
import org.graylog2.plugin.system.NodeId;

import javax.inject.Inject;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class NodeServiceImpl extends PersistedServiceImpl implements NodeService {
    private final long pingTimeout;
    private final static Map<String, Object> lastSeenField = Map.of("last_seen", Map.of("$type", "timestamp"));

    @Inject
    public NodeServiceImpl(final MongoConnection mongoConnection, Configuration configuration) {
        this(mongoConnection, configuration.getStaleLeaderTimeout());
    }

    public NodeServiceImpl(final MongoConnection mongoConnection, final int staleLeaderTimeout) {
        super(mongoConnection);
        this.pingTimeout = TimeUnit.MILLISECONDS.toSeconds(staleLeaderTimeout);
    }

    public Node.Type type() {
        return Node.Type.SERVER;
    }

    @Override
    public boolean registerServer(String nodeId, boolean isLeader, URI httpPublishUri, String hostname) {
        final Map<String, Object> fields = Map.of(
                "$set", Map.of(
                        "node_id", nodeId,
                        "type", type().toString(),
                        "is_leader", isLeader,
                        "transport_address", httpPublishUri.toString(),
                        "hostname", hostname
                ),
                "$currentDate", lastSeenField
        );

        final WriteResult result = this.collection(NodeImpl.class).update(
                new BasicDBObject("node_id", nodeId),
                new BasicDBObject(fields),
                true,
                false
        );
        return result.getN() == 1;
    }

    @Override
    public Node byNodeId(String nodeId) throws NodeNotFoundException {
        DBObject query = new BasicDBObject("node_id", nodeId);
        DBObject o = findOne(NodeImpl.class, query);

        if (o == null || !o.containsField("node_id")) {
            throw new NodeNotFoundException("Unable to find node " + nodeId);
        }

        return new NodeImpl((ObjectId) o.get("_id"), o.toMap());
    }

    @Override
    public Node byNodeId(NodeId nodeId) throws NodeNotFoundException {
        return byNodeId(nodeId.getNodeId());
    }

    @Override
    public Map<String, Node> allActive(Node.Type type) {
        Map<String, Node> nodes = Maps.newHashMap();

        final BasicDBObject query = new BasicDBObject(Map.of(
                "$and", List.of(
                        Map.of("type", type.toString()),
                        recentHeartbeat()
                )));

        return query(NodeImpl.class, query)
                .stream()
                .collect(Collectors.toMap(obj -> (String)obj.get("node_id"), obj -> new NodeImpl((ObjectId) obj.get("_id"), obj.toMap())));
    }

    @Override
    public Map<String, Node> allActive() {
        Map<String, Node> nodes = Maps.newHashMap();

        for (Node.Type type : Node.Type.values()) {
            nodes.putAll(allActive(type));
        }

        return nodes;
    }

    @Override
    public void dropOutdated() {
        final BasicDBObject query = new BasicDBObject("$not", recentHeartbeat());

        destroyAll(NodeImpl.class, query);
    }

    /**
     * Mark this node as alive and probably update some settings that may have changed since last server boot.
     */
    public void markAsAlive(NodeId node, boolean isLeader, URI restTransportAddress) throws NodeNotFoundException {
        BasicDBObject query = new BasicDBObject("node_id", node.getNodeId());

        final BasicDBObject update = new BasicDBObject(Map.of(
                "$set", Map.of(
                        "is_leader", isLeader,
                        "transport_address", restTransportAddress.toString()
                ),
                "$currentDate", lastSeenField
        ));

        final WriteResult result = super.collection(NodeImpl.class).update(query, update);

        final int updatedDocumentsCount = result.getN();
        if (updatedDocumentsCount != 1) {
            throw new NodeNotFoundException("Unable to find node " + node.getNodeId());
        }
    }

    private Map<String, Object> recentHeartbeat() {
        return Map.of("$where", "this.last_seen >= Timestamp(new Date().getTime() / 1000 - " + pingTimeout + ", 1)");
    }

    @Override
    public boolean isOnlyLeader(NodeId nodeId) {
        final BasicDBObject query = new BasicDBObject(Map.of(
                "$and", List.of(
                        recentHeartbeat(),
                        Map.of(
                                "type", Node.Type.SERVER.toString(),
                                "node_id", new BasicDBObject("$ne", nodeId.getNodeId()),
                                "is_leader", true
                        )
                )
        ));

        return count(NodeImpl.class, query) == 0;
    }

    @Override
    public boolean isAnyLeaderPresent() {
        final BasicDBObject query = new BasicDBObject(Map.of(
                "$and", List.of(
                        recentHeartbeat(),
                        Map.of(
                                "type", Node.Type.SERVER.toString(),
                                "is_leader", true
                        )
                )
        ));

        return count(NodeImpl.class, query) > 0;
    }
}
