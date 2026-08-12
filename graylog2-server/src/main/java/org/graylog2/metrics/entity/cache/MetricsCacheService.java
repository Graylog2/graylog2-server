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
package org.graylog2.metrics.entity.cache;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;
import com.mongodb.client.model.UpdateOneModel;
import com.mongodb.client.model.UpdateOptions;
import com.mongodb.client.model.Updates;
import com.mongodb.client.model.WriteModel;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.graylog2.database.MongoCollections;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static org.graylog2.database.indices.MongoDbIndexTools.ensureTTLIndex;
import static org.graylog2.metrics.entity.cache.MetricsCacheConfiguration.METRICS_CACHE_CLEANUP_TTL;

/**
 * Service for caching expensive entity metrics (e.g. OpenSearch aggregations) in MongoDB.
 * <p>
 * Each cached document represents one entity (stream, input, forwarder_input) and contains
 * per-field values with individual computed_at timestamps for staleness checks.
 * A top-level computed_at field drives the MongoDB TTL index for cleanup of deleted entities.
 * </p>
 */
@Singleton
public class MetricsCacheService {

    static final String COLLECTION_NAME = "metrics_cache";
    static final String FIELD_ENTITY_ID = "entity_id";
    static final String FIELD_ENTITY_TYPE = "entity_type";
    static final String FIELD_COMPUTED_AT = "computed_at";
    static final String FIELD_VALUE = "value";

    private final MongoCollection<Document> collection;
    private final Clock clock;

    @Inject
    public MetricsCacheService(MongoCollections mongoCollections,
                               @Named(METRICS_CACHE_CLEANUP_TTL) Duration cleanupTtl,
                               Clock clock) {
        this.clock = clock;
        this.collection = mongoCollections.nonEntityCollection(COLLECTION_NAME, Document.class);
        collection.createIndex(
                Indexes.compoundIndex(Indexes.ascending(FIELD_ENTITY_ID), Indexes.ascending(FIELD_ENTITY_TYPE)),
                new IndexOptions().unique(true)
        );
        ensureTTLIndex(collection, cleanupTtl, FIELD_COMPUTED_AT);
    }

    /**
     * Checks the cache for the given entities and fields in a single MongoDB query.
     *
     * @param entityIds  the entity IDs to look up
     * @param entityType the entity type (e.g. "inputs", "streams")
     * @param fieldTtls  map of field name to TTL duration
     * @param fields     the field names to check
     * @return cache result with fresh values and per-entity stale fields
     */
    public CacheResult checkCache(Collection<String> entityIds,
                                  String entityType,
                                  Map<String, Duration> fieldTtls,
                                  Collection<String> fields) {
        final var builder = CacheResult.builder(entityIds, fields);
        final Instant now = clock.instant();

        final var cursor = collection.find(Filters.and(
                Filters.in(FIELD_ENTITY_ID, entityIds),
                Filters.eq(FIELD_ENTITY_TYPE, entityType)
        ));

        for (final Document doc : cursor) {
            final String entityId = doc.getString(FIELD_ENTITY_ID);
            builder.markFound(entityId);

            for (final String field : fields) {
                classifyField(doc, field, fieldTtls.get(field), now, builder, entityId);
            }
        }

        return builder.build();
    }

    /**
     * Writes a single cached field value for multiple entities in one bulk operation.
     *
     * @param entityType       the entity type
     * @param fieldName        the field name
     * @param valuesByEntityId map of entity ID to field value
     */
    public void putFieldBatch(String entityType, String fieldName, Map<String, ?> valuesByEntityId) {
        if (valuesByEntityId.isEmpty()) {
            return;
        }

        final Date now = new Date();
        final List<WriteModel<Document>> writes = new ArrayList<>();

        for (final var entry : valuesByEntityId.entrySet()) {
            final Bson filter = Filters.and(
                    Filters.eq(FIELD_ENTITY_ID, entry.getKey()),
                    Filters.eq(FIELD_ENTITY_TYPE, entityType)
            );
            final Bson update = Updates.combine(
                    Updates.set(fieldName + "." + FIELD_VALUE, entry.getValue()),
                    Updates.set(fieldName + "." + FIELD_COMPUTED_AT, now),
                    Updates.set(FIELD_ENTITY_TYPE, entityType),
                    Updates.set(FIELD_COMPUTED_AT, now)
            );
            writes.add(new UpdateOneModel<>(filter, update, new UpdateOptions().upsert(true)));
        }

        collection.bulkWrite(writes);
    }

    private void classifyField(Document doc, String field, Duration ttl,
                               Instant now, CacheResult.Builder builder, String entityId) {
        if (ttl == null) {
            builder.addStale(entityId, field);
            return;
        }
        final Document fieldDoc = doc.get(field, Document.class);
        if (fieldDoc == null) {
            builder.addStale(entityId, field);
            return;
        }
        final Date computedAt = fieldDoc.getDate(FIELD_COMPUTED_AT);
        if (computedAt == null || !isFresh(computedAt.toInstant(), now, ttl)) {
            builder.addStale(entityId, field);
            return;
        }
        builder.addFresh(entityId, field, fieldDoc.get(FIELD_VALUE));
    }

    private boolean isFresh(Instant computedAt, Instant now, Duration ttl) {
        return computedAt.plus(ttl).isAfter(now);
    }
}
