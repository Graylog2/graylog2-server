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
package org.graylog2.telemetry.pipelines;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.BulkWriteOptions;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;
import com.mongodb.client.model.Projections;
import com.mongodb.client.model.UpdateOneModel;
import com.mongodb.client.model.UpdateOptions;
import com.mongodb.client.model.Updates;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.bson.Document;
import org.graylog2.database.MongoCollections;

import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Filters.gte;
import static org.graylog2.database.indices.MongoDbIndexTools.ensureTTLIndex;

/**
 * Records when each pipeline last processed a message on any node.
 */
@Singleton
public class PipelineActivityService {
    static final String COLLECTION_NAME = "telemetry_pipeline_activity";
    static final String FIELD_PIPELINE_ID = "pipeline_id";
    static final String FIELD_LAST_ACTIVE_AT = "last_active_at";

    // Rows older than this can't affect the 24h metric.
    private static final Duration RETENTION = Duration.ofDays(2);

    private final MongoCollection<Document> collection;

    @Inject
    public PipelineActivityService(MongoCollections mongoCollections) {
        this.collection = mongoCollections.nonEntityCollection(COLLECTION_NAME, Document.class);
        collection.createIndex(Indexes.ascending(FIELD_PIPELINE_ID), new IndexOptions().unique(true));
        ensureTTLIndex(collection, RETENTION, FIELD_LAST_ACTIVE_AT);
    }

    public void markActive(Collection<String> pipelineIds, Instant activeAt) {
        if (pipelineIds.isEmpty()) {
            return;
        }
        final Date activeAtDate = Date.from(activeAt);
        // $max keeps the latest timestamp when several nodes report the same pipeline.
        final List<UpdateOneModel<Document>> writes = pipelineIds.stream()
                .map(pipelineId -> new UpdateOneModel<Document>(
                        eq(FIELD_PIPELINE_ID, pipelineId),
                        Updates.max(FIELD_LAST_ACTIVE_AT, activeAtDate),
                        new UpdateOptions().upsert(true)))
                .toList();
        collection.bulkWrite(writes, new BulkWriteOptions().ordered(false));
    }

    public Set<String> findActiveSince(Instant since) {
        return collection.find(gte(FIELD_LAST_ACTIVE_AT, Date.from(since)))
                .projection(Projections.include(FIELD_PIPELINE_ID))
                .map(doc -> doc.getString(FIELD_PIPELINE_ID))
                .into(new HashSet<>());
    }
}
