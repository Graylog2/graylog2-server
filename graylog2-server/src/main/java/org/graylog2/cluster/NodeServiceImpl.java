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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import com.mongodb.AggregationOptions;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import com.mongodb.WriteResult;
import org.bson.types.ObjectId;
import org.graylog2.Configuration;
import org.graylog2.database.MongoConnection;
import org.graylog2.database.PersistedServiceImpl;
import org.graylog2.plugin.system.NodeId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.net.URI;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class NodeServiceImpl extends PersistedServiceImpl implements NodeService {

    private static final Logger LOG = LoggerFactory.getLogger(NodeServiceImpl.class);

    public static final String LAST_SEEN_FIELD = "$last_seen";
    private final long pingTimeout;
    private final static Map<String, Object> lastSeenFieldDefinition = Map.of("last_seen", Map.of("$type", "timestamp"));
    private final static DBObject addLastSeenFieldAsDate = new BasicDBObject("$addFields", Map.of("last_seen_date", Map.of("$cond",
            Map.of(
                    "if", Map.of("$isNumber", LAST_SEEN_FIELD),
                    "then", Map.of("$toDate", Map.of("$toLong", LAST_SEEN_FIELD)),
                    "else", Map.of("$toDate", Map.of("$dateToString", Map.of("date", LAST_SEEN_FIELD)))
            )
    )));

    @Inject
    public NodeServiceImpl(final MongoConnection mongoConnection, Configuration configuration) {
        this(mongoConnection, configuration.getStaleLeaderTimeout());
    }

    public NodeServiceImpl(final MongoConnection mongoConnection, final int staleLeaderTimeout) {
        super(mongoConnection);
        this.pingTimeout = staleLeaderTimeout;
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
                "$currentDate", lastSeenFieldDefinition
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

    private Stream<DBObject> aggregate(List<? extends DBObject> pipeline) {
        return cursorToStream(this.collection(NodeImpl.class).aggregate(pipeline, AggregationOptions.builder().build()));
    }

    @Override
    public Map<String, Node> allActive(Node.Type type) {
        return aggregate(recentHeartbeat(List.of(Map.of("type", type.toString()))))
                .collect(Collectors.toMap(obj -> (String) obj.get("node_id"), obj -> new NodeImpl((ObjectId) obj.get("_id"), obj.toMap())));
    }

    @Deprecated
    @Override
    public Map<String, Node> allActive() {
        return allActive(type());
    }

    private List<? extends DBObject> recentHeartbeat(List<? extends Map<String, Object>> additionalMatches) {
        var match = ImmutableList.builder()
                .add(Map.of("$expr", Map.of("$gte", List.of("$last_seen_date", Map.of("$subtract", List.of("$$NOW", this.pingTimeout))))))
                .addAll(additionalMatches)
                .build();
        return List.of(
                addLastSeenFieldAsDate,
                new BasicDBObject("$match", Map.of("$and", match)),
                new BasicDBObject("$unset", "last_seen_date")
        );
    }

    @Override
    public void dropOutdated() {
        var outdatedIds = aggregate(List.of(
                addLastSeenFieldAsDate,
                new BasicDBObject("$match", Map.of("$expr", Map.of("$lt", List.of("$last_seen_date", Map.of("$subtract", List.of("$$NOW", this.pingTimeout)))))),
                new BasicDBObject("$project", Map.of("_id", "$_id"))
        ))
                .map(obj -> obj.get("_id"))
                .toList();

        if (!outdatedIds.isEmpty()) {
            final BasicDBObject query = new BasicDBObject("_id", Map.of("$in", outdatedIds));
            destroyAll(NodeImpl.class, query);
        }
    }

    private Stream<DBObject> cursorToStream(Iterator<DBObject> cursor) {
        return StreamSupport.stream(Spliterators.spliteratorUnknownSize(cursor, Spliterator.ORDERED), false);
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
                "$currentDate", lastSeenFieldDefinition
        ));

        final WriteResult result = super.collection(NodeImpl.class).update(query, update);

        final int updatedDocumentsCount = result.getN();
        if (updatedDocumentsCount != 1) {
            throw new NodeNotFoundException("Unable to find node " + node.getNodeId());
        }
    }

    @Override
    public boolean isOnlyLeader(NodeId nodeId) {

        if(type() != Node.Type.SERVER) {
            LOG.warn("Caution, isOnlyLeader called in the {} context, but returning only results of type {}", type(), Node.Type.SERVER);
        }

        return aggregate(recentHeartbeat(List.of(
                Map.of(
                        "type", Node.Type.SERVER.toString(),
                        "node_id", new BasicDBObject("$ne", nodeId.getNodeId()),
                        "is_leader", true
                )
        ))).findAny().isEmpty();
    }

    @Override
    public boolean isAnyLeaderPresent() {

        if(type() != Node.Type.SERVER) {
            LOG.warn("Caution, isOnlyLeader called in the {} context, but returning only results of type {}", type(), Node.Type.SERVER);
        }

        return aggregate(recentHeartbeat(List.of(
                Map.of(
                        "type", Node.Type.SERVER.toString(),
                        "is_leader", true
                )
        ))).findAny().isPresent();
    }
}
