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

import com.mongodb.client.model.FindOneAndUpdateOptions;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;
import com.mongodb.client.model.Updates;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.graylog2.database.MongoCollection;
import org.graylog2.database.MongoCollections;

import jakarta.inject.Inject;
import jakarta.inject.Named;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Updates.currentDate;
import static org.graylog2.configuration.TelemetryConfiguration.TELEMETRY_CLUSTER_INFO_TTL;
import static org.graylog2.database.indices.MongoDbIndexTools.ensureTTLIndex;
import static org.graylog2.telemetry.cluster.db.TelemetryClusterInfoDto.FIELD_CLUSTER_ID;
import static org.graylog2.telemetry.cluster.db.TelemetryClusterInfoDto.FIELD_CODENAME;
import static org.graylog2.telemetry.cluster.db.TelemetryClusterInfoDto.FIELD_CPU_CORES;
import static org.graylog2.telemetry.cluster.db.TelemetryClusterInfoDto.FIELD_FACILITY;
import static org.graylog2.telemetry.cluster.db.TelemetryClusterInfoDto.FIELD_HOSTNAME;
import static org.graylog2.telemetry.cluster.db.TelemetryClusterInfoDto.FIELD_IS_LEADER;
import static org.graylog2.telemetry.cluster.db.TelemetryClusterInfoDto.FIELD_IS_PROCESSING;
import static org.graylog2.telemetry.cluster.db.TelemetryClusterInfoDto.FIELD_LB_STATUS;
import static org.graylog2.telemetry.cluster.db.TelemetryClusterInfoDto.FIELD_LIFECYCLE;
import static org.graylog2.telemetry.cluster.db.TelemetryClusterInfoDto.FIELD_JVM_HEAP_COMMITTED;
import static org.graylog2.telemetry.cluster.db.TelemetryClusterInfoDto.FIELD_JVM_HEAP_MAX;
import static org.graylog2.telemetry.cluster.db.TelemetryClusterInfoDto.FIELD_JVM_HEAP_USED;
import static org.graylog2.telemetry.cluster.db.TelemetryClusterInfoDto.FIELD_MEMORY_TOTAL;
import static org.graylog2.telemetry.cluster.db.TelemetryClusterInfoDto.FIELD_NODE_ID;
import static org.graylog2.telemetry.cluster.db.TelemetryClusterInfoDto.FIELD_OPERATING_SYSTEM;
import static org.graylog2.telemetry.cluster.db.TelemetryClusterInfoDto.FIELD_STARTED_AT;
import static org.graylog2.telemetry.cluster.db.TelemetryClusterInfoDto.FIELD_TIMEZONE;
import static org.graylog2.telemetry.cluster.db.TelemetryClusterInfoDto.FIELD_UPDATED_AT;
import static org.graylog2.telemetry.cluster.db.TelemetryClusterInfoDto.FIELD_VERSION;

public class DBTelemetryClusterInfo {
    private static final String COLLECTION_NAME = "telemetry_cluster_infos";
    private final MongoCollection<TelemetryClusterInfoDto> collection;

    @Inject
    public DBTelemetryClusterInfo(@Named(TELEMETRY_CLUSTER_INFO_TTL) Duration telemetryClusterInfoTtl,
                                  MongoCollections mongoCollections) {

        collection = mongoCollections.collection(COLLECTION_NAME, TelemetryClusterInfoDto.class);
        collection.createIndex(Indexes.ascending(FIELD_NODE_ID), new IndexOptions().unique(true));

        com.mongodb.client.MongoCollection<Document> nonEntityCollection = mongoCollections.nonEntityCollection(
                COLLECTION_NAME,
                Document.class
        );
        ensureTTLIndex(nonEntityCollection, telemetryClusterInfoTtl, FIELD_UPDATED_AT);
    }

    public void update(TelemetryClusterInfoDto nodeInfo) {
        List<Bson> updates = new ArrayList<>();
        updates.add(Updates.set(FIELD_NODE_ID, nodeInfo.nodeId()));
        updates.add(Updates.set(FIELD_CLUSTER_ID, nodeInfo.clusterId()));
        updates.add(Updates.set(FIELD_CODENAME, nodeInfo.codename()));
        updates.add(Updates.set(FIELD_FACILITY, nodeInfo.facility()));
        updates.add(Updates.set(FIELD_HOSTNAME, nodeInfo.hostname()));
        updates.add(Updates.set(FIELD_LB_STATUS, nodeInfo.lbStatus()));
        updates.add(Updates.set(FIELD_LIFECYCLE, nodeInfo.lifecycle()));
        updates.add(Updates.set(FIELD_OPERATING_SYSTEM, nodeInfo.operatingSystem()));
        updates.add(Updates.set(FIELD_STARTED_AT, nodeInfo.startedAt()));
        updates.add(Updates.set(FIELD_TIMEZONE, nodeInfo.timezone()));
        updates.add(Updates.set(FIELD_IS_LEADER, nodeInfo.isLeader()));
        updates.add(Updates.set(FIELD_IS_PROCESSING, nodeInfo.isProcessing()));
        updates.add(Updates.set(FIELD_VERSION, nodeInfo.version()));
        updates.add(Updates.set(FIELD_JVM_HEAP_USED, nodeInfo.jvmHeapUsed()));
        updates.add(Updates.set(FIELD_JVM_HEAP_COMMITTED, nodeInfo.jvmHeapCommitted()));
        updates.add(Updates.set(FIELD_JVM_HEAP_MAX, nodeInfo.jvmHeapMax()));
        updates.add(Updates.set(FIELD_MEMORY_TOTAL, nodeInfo.memoryTotal()));
        updates.add(Updates.set(FIELD_CPU_CORES, nodeInfo.cpuCores()));
        updates.add(Updates.set(FIELD_VERSION, nodeInfo.version()));
        updates.add(currentDate(FIELD_UPDATED_AT));

        collection.findOneAndUpdate(
                eq(FIELD_NODE_ID, nodeInfo.nodeId()),
                Updates.combine(updates),
                new FindOneAndUpdateOptions().upsert(true)
        );
    }

    public List<TelemetryClusterInfoDto> findAll() {
        return collection.find().into(new ArrayList<>());
    }
}
