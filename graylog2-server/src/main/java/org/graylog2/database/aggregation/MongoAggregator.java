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
package org.graylog2.database.aggregation;

import com.mongodb.client.MongoCollection;
import org.bson.Document;
import org.graylog2.database.aggregation.BucketAggregationResult.Bucket;

import java.time.Duration;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static org.graylog2.database.aggregation.AggregationPipelines.BUCKET_COUNT;
import static org.graylog2.database.aggregation.AggregationPipelines.COUNT;
import static org.graylog2.database.aggregation.AggregationPipelines.FACET_BUCKETS;
import static org.graylog2.database.aggregation.AggregationPipelines.FACET_MISSING;
import static org.graylog2.database.aggregation.AggregationPipelines.FACET_OTHER;
import static org.graylog2.database.aggregation.AggregationPipelines.ID;

/**
 * Counts the documents of one collection per bucket, e.g. for slicing, in a single round trip.
 * <p>
 * The aggregator knows nothing about entities or permissions: callers pass a filter that already restricts the
 * documents to those a user may see. Every aggregation is bounded by a server-side time limit, and fails with a
 * {@link com.mongodb.MongoExecutionTimeoutException} when it runs longer instead of returning partial results.
 */
public class MongoAggregator {
    public static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(30);

    private final MongoCollection<Document> collection;
    private final Duration timeout;

    public MongoAggregator(final MongoCollection<Document> collection) {
        this(collection, DEFAULT_TIMEOUT);
    }

    private MongoAggregator(final MongoCollection<Document> collection, final Duration timeout) {
        this.collection = Objects.requireNonNull(collection, "collection");
        this.timeout = Objects.requireNonNull(timeout, "timeout");
    }

    public MongoAggregator withTimeout(final Duration timeout) {
        return new MongoAggregator(collection, timeout);
    }

    public BucketAggregationResult buckets(final BucketAggregation request) {
        // $facet always returns exactly one document
        final Document result = Objects.requireNonNull(collection.aggregate(AggregationPipelines.buckets(request))
                .allowDiskUse(true)
                .maxTime(timeout.toMillis(), TimeUnit.MILLISECONDS)
                .first());
        final List<Bucket> buckets = result.getList(FACET_BUCKETS, Document.class).stream()
                .map(bucket -> new Bucket(normalizeKey(bucket.get(ID)), count(bucket, COUNT)))
                .toList();
        final long missingCount = result.getList(FACET_MISSING, Document.class).stream()
                .findFirst()
                .map(missing -> count(missing, COUNT))
                .orElse(0L);
        final Optional<Document> other = result.getList(FACET_OTHER, Document.class).stream().findFirst();
        return new BucketAggregationResult(buckets,
                missingCount,
                other.map(rest -> count(rest, BUCKET_COUNT)).orElse(0L),
                other.map(rest -> count(rest, COUNT)).orElse(0L));
    }

    private static long count(final Document document, final String field) {
        return document.get(field, Number.class).longValue();
    }

    private static Object normalizeKey(final Object key) {
        return switch (key) {
            case Integer integer -> integer.longValue();
            case Date date -> date.toInstant();
            default -> key;
        };
    }
}
