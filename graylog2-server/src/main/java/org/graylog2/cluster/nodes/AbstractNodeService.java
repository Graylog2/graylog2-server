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
import com.google.errorprone.annotations.MustBeClosed;
import com.mongodb.BasicDBObject;
import com.mongodb.client.model.ReplaceOptions;
import com.mongodb.client.result.UpdateResult;
import jakarta.inject.Inject;
import org.bson.conversions.Bson;
import org.graylog2.Configuration;
import org.graylog2.cluster.Node;
import org.graylog2.cluster.NodeNotFoundException;
import org.graylog2.database.MongoCollection;
import org.graylog2.database.MongoCollections;
import org.graylog2.database.utils.MongoUtils;
import org.graylog2.plugin.system.NodeId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public abstract class AbstractNodeService<DTO extends NodeDto> implements NodeService<DTO> {

    private static final Logger LOG = LoggerFactory.getLogger(AbstractNodeService.class);

    public static final String LAST_SEEN_FIELD = "$last_seen";
    private final long pingTimeout;
    private final static Map<String, Object> lastSeenFieldDefinition = Map.of("last_seen", Map.of("$type", "timestamp"));
    private final static BasicDBObject addLastSeenFieldAsDate = new BasicDBObject("$addFields",
            Map.of("last_seen_date", Map.of("$cond",
            Map.of(
                    "if", Map.of("$isNumber", LAST_SEEN_FIELD),
                    "then", Map.of("$toDate", Map.of("$toLong", LAST_SEEN_FIELD)),
                    "else", Map.of("$toDate", Map.of("$dateToString", Map.of("date", LAST_SEEN_FIELD)))
            )
    )));

    private final MongoCollection<DTO> db;

    @Inject
    public AbstractNodeService(final MongoCollections mongoCollections,
                               Configuration configuration,
                               String collectionName,
                               Class<DTO> nodeClass) {
        this(mongoCollections, configuration.getStaleLeaderTimeout(), collectionName, nodeClass);
    }

    private AbstractNodeService(final MongoCollections mongoCollections,
                                final int staleLeaderTimeout,
                                String collectionName,
                                Class<DTO> nodeClass) {
        this.db = mongoCollections.collection(collectionName, nodeClass);
        this.pingTimeout = staleLeaderTimeout;
    }

    @Override
    public boolean registerServer(DTO dto) {
        UpdateResult result = this.db.replaceOne(
                new BasicDBObject("node_id", dto.getNodeId()),
                dto,
                new ReplaceOptions().upsert(true)
        );
        // set timestamp using db
        db.updateOne(
                new BasicDBObject("node_id", dto.getNodeId()),
                new BasicDBObject("$currentDate", lastSeenFieldDefinition)
        );
        return result.getMatchedCount() > 0 || result.getUpsertedId() != null;
    }

    @Override
    public DTO byNodeId(String nodeId) throws NodeNotFoundException {
        return Optional.ofNullable(db.find(new BasicDBObject("node_id", nodeId)).first())
                .orElseThrow(() -> new NodeNotFoundException("Unable to find node " + nodeId));
    }

    @Override
    public DTO byNodeId(NodeId nodeId) throws NodeNotFoundException {
        return byNodeId(nodeId.getNodeId());
    }

    @Override
    public Map<String, DTO> byNodeIds(Collection<String> nodeIds) {
        try (var stream = MongoUtils.stream(db.find(new BasicDBObject("node_id", new BasicDBObject("$in", nodeIds))))) {
            return stream.collect(Collectors.toMap(Node::getNodeId, Function.identity()));
        }
    }

    @MustBeClosed
    private Stream<DTO> aggregate(List<Bson> pipeline) {
        return MongoUtils.stream(db.aggregate(pipeline));
    }

    @Override
    public Map<String, DTO> allActive() {
        try (var stream = aggregate(recentHeartbeat(List.of(Map.of())))) {
            return stream
                    .collect(Collectors.toMap(Node::getNodeId, Function.identity()));
        }
    }

    private List<Bson> recentHeartbeat(List<? extends Map<String, Object>> additionalMatches) {
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
        try (var stream = aggregate(List.of(
                addLastSeenFieldAsDate,
                new BasicDBObject("$match", Map.of("$expr", Map.of("$lt", List.of("$last_seen_date", Map.of("$subtract", List.of("$$NOW", this.pingTimeout))))))
        ))) {
            var outdatedIds = stream.map(DTO::id).toList();

            if (!outdatedIds.isEmpty()) {
                db.deleteMany(MongoUtils.stringIdsIn(outdatedIds));
            }
        }
    }

    @Override
    public boolean isOnlyLeader(NodeId nodeId) {
        try (var stream = aggregate(recentHeartbeat(List.of(
                Map.of(
                        "node_id", new BasicDBObject("$ne", nodeId.getNodeId()),
                        "is_leader", true
                )
        )))) {
            return stream.findAny().isEmpty();
        }
    }

    @Override
    public boolean isAnyLeaderPresent() {
        try (var stream = aggregate(recentHeartbeat(List.of(
                Map.of(
                        "is_leader", true
                )
        )))) {
            return stream.findAny().isPresent();
        }
    }

    @Override
    public void ping(DTO dto) {
        var result = db.replaceOne(new BasicDBObject("node_id", dto.getNodeId()), dto);
        if (result.getMatchedCount() != 1) {
            LOG.warn("Did not find meta info of this node. Re-registering.");
            registerServer(dto);
        }
        // set timestamp using db
        db.updateOne(
                new BasicDBObject("node_id", dto.getNodeId()),
                new BasicDBObject("$currentDate", lastSeenFieldDefinition)
        );
        try {
            // Remove old nodes that are no longer running. (Just some housekeeping)
            dropOutdated();
        } catch (Exception e) {
            LOG.warn("Caught exception during node ping.", e);
        }
    }

    @Override
    public void update(DTO dto) {
        db.replaceOne(new BasicDBObject("node_id", dto.getNodeId()), dto);
    }

}
