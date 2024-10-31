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
package org.graylog2.telemetry.cluster.db;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.FindOneAndUpdateOptions;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;
import com.mongodb.client.model.Updates;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.graylog2.database.MongoConnection;

import jakarta.inject.Inject;
import jakarta.inject.Named;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Updates.currentDate;
import static org.graylog2.configuration.TelemetryConfiguration.TELEMETRY_CLUSTER_INFO_TTL;
import static org.graylog2.database.indices.MongoDbIndexTools.ensureTTLIndex;

public class DBTelemetryClusterInfo {

    public static final String FIELD_NODE_ID = "node_id";
    public static final String FIELD_IS_LEADER = "is_leader";
    public static final String FIELD_VERSION = "version";
    private static final String FIELD_ID = "_id";
    private static final String FIELD_UPDATED_AT = "updated_at";
    private static final String COLLECTION_NAME = "telemetry_cluster_infos";
    private final MongoCollection<Document> collection;


    @Inject
    public DBTelemetryClusterInfo(@Named(TELEMETRY_CLUSTER_INFO_TTL) Duration telemetryClusterInfoTtl,
                                  MongoConnection mongoConnection) {

        collection = mongoConnection.getMongoDatabase().getCollection(COLLECTION_NAME);
        collection.createIndex(Indexes.ascending(FIELD_NODE_ID), new IndexOptions().unique(true));

        ensureTTLIndex(collection, telemetryClusterInfoTtl, FIELD_UPDATED_AT);
    }

    public void update(Map<String, Object> nodeInfo, String nodeId) {
        List<Bson> updateValues = nodeInfo.entrySet().stream().map(entry -> Updates.set(entry.getKey(), entry.getValue())).collect(Collectors.toList());
        updateValues.add(currentDate(FIELD_UPDATED_AT));
        collection.findOneAndUpdate(
                eq(FIELD_NODE_ID, nodeId),
                Updates.combine(updateValues),
                new FindOneAndUpdateOptions().upsert(true)
        );
    }

    public Map<String, Map<String, Object>> findAll() {
        Map<String, Map<String, Object>> nodes = new LinkedHashMap<>();
        for (Document document : collection.find()) {
            nodes.put(document.getString(FIELD_NODE_ID), document.entrySet().stream()
                    .filter(entry -> !FIELD_UPDATED_AT.equals(entry.getKey()))
                    .filter(entry -> !FIELD_ID.equals(entry.getKey()))
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)));
        }
        return nodes;
    }
}
