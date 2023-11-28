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

import com.google.common.collect.ImmutableList;
import com.mongodb.AggregationOptions;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import com.mongodb.WriteResult;
import org.bson.types.ObjectId;
import org.graylog2.Configuration;
import org.graylog2.cluster.Node;
import org.graylog2.cluster.NodeNotFoundException;
import org.graylog2.database.MongoConnection;
import org.graylog2.database.PersistedServiceImpl;
import org.graylog2.plugin.system.NodeId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public abstract class AbstractNodeService<T extends AbstractNode<? extends NodeDto>, DTO extends NodeDto> extends PersistedServiceImpl implements NodeService<DTO> {

    private static final Logger LOG = LoggerFactory.getLogger(AbstractNodeService.class);

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

    private final Class<T> nodeClass;

    @Inject
    public AbstractNodeService(final MongoConnection mongoConnection, Configuration configuration, Class<T> nodeClass) {
        this(mongoConnection, configuration.getStaleLeaderTimeout(), nodeClass);
    }

    private AbstractNodeService(final MongoConnection mongoConnection, final int staleLeaderTimeout, Class<T> nodeClass) {
        super(mongoConnection);
        this.pingTimeout = staleLeaderTimeout;
        this.nodeClass = nodeClass;
    }

    @Override
    public boolean registerServer(NodeDto dto) {
        final var params = dto.toEntityParameters();

        final Map<String, Object> fields = Map.of(
                "$set", params,
                "$currentDate", lastSeenFieldDefinition
        );

        final WriteResult result = this.collection(nodeClass).update(
                new BasicDBObject("node_id", dto.getId()),
                new BasicDBObject(fields),
                true,
                false
        );
        return result.getN() == 1;
    }

    @SuppressWarnings("unchecked")
    @Override
    public DTO byNodeId(String nodeId) throws NodeNotFoundException {
        DBObject query = new BasicDBObject("node_id", nodeId);
        DBObject o = findOne(nodeClass, query);

        if (o == null || !o.containsField("node_id")) {
            throw new NodeNotFoundException("Unable to find node " + nodeId);
        }

        return (DTO) construct((ObjectId) o.get("_id"), o.toMap()).toDto();
    }

    protected T construct(ObjectId id, Map map) {
        try {
            return nodeClass
                    .getDeclaredConstructor(ObjectId.class, Map.class)
                    .newInstance(id, map);
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException |
                 NoSuchMethodException e) {
            LOG.error("Could not construct node {}", nodeClass.getName(), e);
            throw new RuntimeException("Could not construct node");
        }
    }

    @Override
    public DTO byNodeId(NodeId nodeId) throws NodeNotFoundException {
        return byNodeId(nodeId.getNodeId());
    }

    @SuppressWarnings("unchecked")
    @Override
    public Map<String, DTO> byNodeIds(Collection<String> nodeIds) {
        return query(nodeClass, new BasicDBObject("node_id", new BasicDBObject("$in", nodeIds)))
                .stream()
                .map(o -> (DTO) construct((ObjectId) o.get("_id"), o.toMap()).toDto())
                .collect(Collectors.toMap(Node::getNodeId, Function.identity()));
    }

    private Stream<DBObject> aggregate(List<? extends DBObject> pipeline) {
        return cursorToStream(this.collection(nodeClass).aggregate(pipeline, AggregationOptions.builder().build()));
    }

    @SuppressWarnings("unchecked")
    @Override
    public Map<String, DTO> allActive() {
        return aggregate(recentHeartbeat(List.of(Map.of())))
                .collect(Collectors.toMap(obj -> (String) obj.get("node_id"), obj -> (DTO) construct((ObjectId) obj.get("_id"), obj.toMap()).toDto()));
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
            destroyAll(nodeClass, query);
        }
    }

    private Stream<DBObject> cursorToStream(Iterator<DBObject> cursor) {
        return StreamSupport.stream(Spliterators.spliteratorUnknownSize(cursor, Spliterator.ORDERED), false);
    }

    /**
     * Mark this node as alive and probably update some settings that may have changed since last server boot.
     */
    @Override
    public void markAsAlive(NodeDto dto) throws NodeNotFoundException {
        BasicDBObject query = new BasicDBObject("node_id", dto.getId());
        final var params = dto.toEntityParameters();

        final BasicDBObject update = new BasicDBObject(Map.of(
                "$set", params,
                "$currentDate", lastSeenFieldDefinition
        ));

        final WriteResult result = super.collection(nodeClass).update(query, update);

        final int updatedDocumentsCount = result.getN();
        if (updatedDocumentsCount != 1) {
            throw new NodeNotFoundException("Unable to find node " + dto.getId());
        }
    }

    @Override
    public boolean isOnlyLeader(NodeId nodeId) {
        return aggregate(recentHeartbeat(List.of(
                Map.of(
                        "node_id", new BasicDBObject("$ne", nodeId.getNodeId()),
                        "is_leader", true
                )
        ))).findAny().isEmpty();
    }

    @Override
    public boolean isAnyLeaderPresent() {
        return aggregate(recentHeartbeat(List.of(
                Map.of(
                        "is_leader", true
                )
        ))).findAny().isPresent();
    }

    @Override
    public void ping(NodeDto dto) {
        try {
            markAsAlive(dto);
        } catch (NodeNotFoundException e) {
            LOG.warn("Did not find meta info of this node. Re-registering.");
            registerServer(dto);
        }
        try {
            // Remove old nodes that are no longer running. (Just some housekeeping)
            dropOutdated();
        } catch (Exception e) {
            LOG.warn("Caught exception during node ping.", e);
        }
    }
}
