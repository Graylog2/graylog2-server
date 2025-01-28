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
package org.graylog2.indexer.ranges;

import com.google.common.base.Stopwatch;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSortedSet;
import com.google.common.eventbus.AllowConcurrentEvents;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.google.common.primitives.Ints;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Indexes;
import com.mongodb.client.model.Updates;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.bson.conversions.Bson;
import org.graylog2.audit.AuditActor;
import org.graylog2.audit.AuditEventSender;
import org.graylog2.database.MongoCollections;
import org.graylog2.database.NotFoundException;
import org.graylog2.indexer.IndexSetRegistry;
import org.graylog2.indexer.indices.Indices;
import org.graylog2.indexer.indices.events.IndicesClosedEvent;
import org.graylog2.indexer.indices.events.IndicesDeletedEvent;
import org.graylog2.indexer.indices.events.IndicesReopenedEvent;
import org.graylog2.indexer.searches.IndexRangeStats;
import org.graylog2.plugin.system.NodeId;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.SortedSet;
import java.util.concurrent.TimeUnit;

import static com.mongodb.client.model.Filters.and;
import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Filters.exists;
import static com.mongodb.client.model.Filters.gte;
import static com.mongodb.client.model.Filters.in;
import static com.mongodb.client.model.Filters.lte;
import static com.mongodb.client.model.Filters.or;
import static org.graylog2.audit.AuditEventTypes.ES_INDEX_RANGE_CREATE;
import static org.graylog2.audit.AuditEventTypes.ES_INDEX_RANGE_DELETE;
import static org.graylog2.indexer.indices.Indices.checkIfHealthy;

@Singleton
public class MongoIndexRangeService implements IndexRangeService {
    private static final Logger LOG = LoggerFactory.getLogger(MongoIndexRangeService.class);
    private static final String COLLECTION_NAME = "index_ranges";

    private final Indices indices;
    private final IndexSetRegistry indexSetRegistry;
    private final AuditEventSender auditEventSender;
    private final NodeId nodeId;
    private final MongoCollection<MongoIndexRange> collection;

    @Inject
    public MongoIndexRangeService(MongoCollections mongoCollections,
                                  Indices indices,
                                  IndexSetRegistry indexSetRegistry,
                                  AuditEventSender auditEventSender,
                                  NodeId nodeId,
                                  EventBus eventBus) {
        this.indices = indices;
        this.indexSetRegistry = indexSetRegistry;
        this.auditEventSender = auditEventSender;
        this.nodeId = nodeId;
        this.collection = mongoCollections.nonEntityCollection(COLLECTION_NAME, MongoIndexRange.class);

        eventBus.register(this);

        collection.createIndex(Indexes.ascending(MongoIndexRange.FIELD_INDEX_NAME));
        collection.createIndex(Indexes.ascending(MongoIndexRange.FIELD_BEGIN, MongoIndexRange.FIELD_END));
    }

    @Override
    public IndexRange get(String index) throws NotFoundException {
        final var query = and(
                exists("start", false),
                eq(IndexRange.FIELD_INDEX_NAME, index));
        final MongoIndexRange indexRange = collection.find(query).first();
        if (indexRange == null) {
            throw new NotFoundException("Index range for index <" + index + "> not found.");
        }

        return indexRange;
    }

    @Override
    public SortedSet<IndexRange> find(DateTime begin, DateTime end) {
        final var query = or(
                and(
                        exists("start", false),  // "start" has been used by the old index ranges in MongoDB
                        lte(IndexRange.FIELD_BEGIN, end.getMillis()),
                        gte(IndexRange.FIELD_END, begin.getMillis())
                ),
                and(
                        exists("start", false),  // "start" has been used by the old index ranges in MongoDB
                        lte(IndexRange.FIELD_BEGIN, 0L),
                        gte(IndexRange.FIELD_END, 0L)
                )
        );

        return ImmutableSortedSet.copyOf(IndexRange.COMPARATOR, collection.find(query));
    }

    @Override
    public SortedSet<IndexRange> findAll() {
        return ImmutableSortedSet.copyOf(IndexRange.COMPARATOR, collection.find(exists("start", false)));
    }

    @Override
    public SortedSet<IndexRange> find(Bson query) {
        return ImmutableSortedSet.copyOf(IndexRange.COMPARATOR, collection.find(query));
    }

    @Override
    public IndexRange calculateRange(String index) {
        checkIfHealthy(indices.waitForRecovery(index),
                (status) -> new RuntimeException("Unable to calculate range for index <" + index + ">, index is unhealthy: " + status));
        final DateTime now = DateTime.now(DateTimeZone.UTC);
        final Stopwatch sw = Stopwatch.createStarted();
        final IndexRangeStats stats = indices.indexRangeStatsOfIndex(index);
        final int duration = Ints.saturatedCast(sw.stop().elapsed(TimeUnit.MILLISECONDS));

        LOG.info("Calculated range of [{}] in [{}ms].", index, duration);
        return MongoIndexRange.create(index, stats.min(), stats.max(), now, duration, stats.streamIds());
    }

    @Override
    public IndexRange createUnknownRange(String index) {
        final DateTime begin = new DateTime(0L, DateTimeZone.UTC);
        final DateTime end = new DateTime(0L, DateTimeZone.UTC);
        final DateTime now = DateTime.now(DateTimeZone.UTC);
        return MongoIndexRange.create(index, begin, end, now, 0);
    }

    @Override
    public void save(IndexRange indexRange) {
        remove(indexRange.indexName());
        collection.insertOne(MongoIndexRange.create(indexRange));
    }

    @Override
    public boolean renameIndex(String from, String to) {
        return collection.updateMany(
                        eq(IndexRange.FIELD_INDEX_NAME, from),
                        Updates.set(IndexRange.FIELD_INDEX_NAME, to))
                .getMatchedCount() > 0;
    }

    @Override
    public boolean remove(String index) {
        return collection.deleteMany(in(IndexRange.FIELD_INDEX_NAME, index)).getDeletedCount() > 0;
    }

    @Subscribe
    @AllowConcurrentEvents
    public void handleIndexDeletion(IndicesDeletedEvent event) {
        for (String index : event.indices()) {
            LOG.debug("Index \"{}\" has been deleted. Removing index range.", index);
            if (remove(index)) {
                auditEventSender.success(AuditActor.system(nodeId), ES_INDEX_RANGE_DELETE, ImmutableMap.of("index_name", index));
            }
        }
    }

    @Subscribe
    @AllowConcurrentEvents
    public void handleIndexClosing(IndicesClosedEvent event) {
        for (String index : event.indices()) {
            if (!indexSetRegistry.isManagedIndex(index)) {
                LOG.debug("Not handling closed index <{}> because it's not managed by any index set.", index);
                continue;
            }
            LOG.debug("Index \"{}\" has been closed. Removing index range.", index);
            if (remove(index)) {
                auditEventSender.success(AuditActor.system(nodeId), ES_INDEX_RANGE_DELETE, ImmutableMap.of("index_name", index));
            }
        }
    }

    @Subscribe
    @AllowConcurrentEvents
    public void handleIndexReopening(IndicesReopenedEvent event) {
        for (final String index : event.indices()) {
            if (!indexSetRegistry.isManagedIndex(index)) {
                LOG.debug("Not handling reopened index <{}> because it's not managed by any index set.", index);
                continue;
            }
            LOG.debug("Index \"{}\" has been reopened. Calculating index range.", index);

            checkIfHealthy(indices.waitForRecovery(index), (status) -> new RuntimeException("Not handling reopened index <" + index + ">, index is unhealthy: " + status));

            final IndexRange indexRange;
            try {
                indexRange = calculateRange(index);
                auditEventSender.success(AuditActor.system(nodeId), ES_INDEX_RANGE_CREATE, ImmutableMap.of("index_name", index));
            } catch (Exception e) {
                final String message = "Couldn't calculate index range for index \"" + index + "\"";
                LOG.error(message, e);
                auditEventSender.failure(AuditActor.system(nodeId), ES_INDEX_RANGE_CREATE, ImmutableMap.of("index_name", index));
                throw new RuntimeException(message, e);
            }

            save(indexRange);
        }
    }
}
