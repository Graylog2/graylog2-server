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
package org.graylog2.inputs.persistence;

import com.mongodb.client.model.Filters;
import com.mongodb.client.model.FindOneAndUpdateOptions;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;
import com.mongodb.client.model.ReturnDocument;
import com.mongodb.client.model.Updates;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.bson.conversions.Bson;
import org.graylog2.database.MongoCollection;
import org.graylog2.database.MongoCollections;
import org.graylog2.database.utils.MongoUtils;
import org.graylog2.plugin.IOState;
import org.graylog2.plugin.Tools;
import org.graylog2.plugin.system.NodeId;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.graylog2.inputs.persistence.InputStateDto.FIELD_DETAILED_MESSAGE;
import static org.graylog2.inputs.persistence.InputStateDto.FIELD_INPUT_ID;
import static org.graylog2.inputs.persistence.InputStateDto.FIELD_LAST_FAILED_AT;
import static org.graylog2.inputs.persistence.InputStateDto.FIELD_NODE_ID;
import static org.graylog2.inputs.persistence.InputStateDto.FIELD_STARTED_AT;
import static org.graylog2.inputs.persistence.InputStateDto.FIELD_STATE;
import static org.graylog2.inputs.persistence.InputStateDto.FIELD_UPDATED_AT;

@Singleton
public class InputStateService {
    private static final Logger LOG = LoggerFactory.getLogger(InputStateService.class);
    private static final String COLLECTION_NAME = "input_runtime_states";

    private final MongoCollection<InputStateDto> collection;
    private final MongoUtils<InputStateDto> mongoUtils;
    private final String thisNodeId;

    @Inject
    public InputStateService(MongoCollections mongoCollections, NodeId nodeId) {
        this.collection = mongoCollections.collection(COLLECTION_NAME, InputStateDto.class);
        this.mongoUtils = mongoCollections.utils(collection);
        this.thisNodeId = nodeId.getNodeId();

        collection.createIndex(
                Indexes.ascending(FIELD_INPUT_ID, FIELD_NODE_ID),
                new IndexOptions().unique(true));
        collection.createIndex(Indexes.ascending(FIELD_NODE_ID));
        collection.createIndex(Indexes.ascending(FIELD_STATE));
    }

    public void upsertState(String inputId, IOState.Type state,
                            @Nullable DateTime startedAt,
                            @Nullable DateTime lastFailedAt,
                            @Nullable String detailedMessage) {
        final Bson filter = Filters.and(
                Filters.eq(FIELD_INPUT_ID, inputId),
                Filters.eq(FIELD_NODE_ID, thisNodeId));

        final var updates = new java.util.ArrayList<Bson>();
        updates.add(Updates.set(FIELD_INPUT_ID, inputId));
        updates.add(Updates.set(FIELD_NODE_ID, thisNodeId));
        updates.add(Updates.set(FIELD_STATE, state.toString()));
        updates.add(Updates.set(FIELD_UPDATED_AT, Tools.nowUTC()));

        if (startedAt != null) {
            updates.add(Updates.set(FIELD_STARTED_AT, startedAt));
        }
        if (lastFailedAt != null) {
            updates.add(Updates.set(FIELD_LAST_FAILED_AT, lastFailedAt));
        }
        if (detailedMessage != null) {
            updates.add(Updates.set(FIELD_DETAILED_MESSAGE, detailedMessage));
        } else {
            updates.add(Updates.unset(FIELD_DETAILED_MESSAGE));
        }

        collection.findOneAndUpdate(filter, Updates.combine(updates),
                new FindOneAndUpdateOptions().upsert(true).returnDocument(ReturnDocument.AFTER));
    }

    public void removeState(String inputId) {
        collection.deleteOne(Filters.and(
                Filters.eq(FIELD_INPUT_ID, inputId),
                Filters.eq(FIELD_NODE_ID, thisNodeId)));
    }

    public void removeAllForNode() {
        final long deleted = collection.deleteMany(Filters.eq(FIELD_NODE_ID, thisNodeId)).getDeletedCount();
        LOG.debug("Removed {} runtime state documents for node {}", deleted, thisNodeId);
    }

    public void removeAllForNode(String nodeId) {
        final long deleted = collection.deleteMany(Filters.eq(FIELD_NODE_ID, nodeId)).getDeletedCount();
        if (deleted > 0) {
            LOG.info("Removed {} stale runtime state documents for node {}", deleted, nodeId);
        }
    }


    public Map<String, Set<String>> getClusterStatuses() {
        try (var stream = MongoUtils.stream(collection.find())) {
            return stream.collect(Collectors.groupingBy(
                    InputStateDto::inputId,
                    HashMap::new,
                    Collectors.mapping(InputStateDto::state, Collectors.toSet())
            ));
        }
    }

    public Set<String> getByState(IOState.Type state) {
        try (var stream = MongoUtils.stream(collection.find(Filters.eq(FIELD_STATE, state.toString())))) {
            return stream.map(InputStateDto::inputId).collect(Collectors.toSet());
        }
    }

    public Set<InputStateDto> getByStates(Collection<IOState.Type> states) {
        List<String> stateStrings = states.stream().map(IOState.Type::toString).toList();
        Bson filter = Filters.in(FIELD_STATE, stateStrings);
        try (var stream = MongoUtils.stream(collection.find(filter))) {
            return stream.collect(Collectors.toSet());
        }
    }

    public Set<String> getDistinctNodeIds() {
        try (var stream = MongoUtils.stream(collection.distinct(FIELD_NODE_ID, String.class))) {
            return stream.collect(Collectors.toSet());
        }
    }
}
