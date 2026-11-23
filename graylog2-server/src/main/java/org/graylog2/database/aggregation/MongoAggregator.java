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
import com.mongodb.client.model.Filters;
import jakarta.annotation.Nullable;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.graylog2.database.aggregation.grouping.Grouping;
import org.graylog2.database.aggregation.grouping.RelatedSource;

import java.time.Duration;
import java.util.Objects;
import java.util.function.Function;

/**
 * Counts the documents of one collection per bucket, e.g. for slicing, in a single round trip.
 * <p>
 * An aggregator may be restricted to the documents a user may read, see {@link PermittedAggregators}. It then counts
 * only those, and groups by related documents only if the user may read them as well. Aggregators created with
 * {@link org.graylog2.database.MongoCollections#aggregator(String)} aren't restricted.
 * <p>
 * Every aggregation is bounded by a server-side time limit, and fails with a
 * {@link com.mongodb.MongoExecutionTimeoutException} when it runs longer instead of returning partial results.
 */
public class MongoAggregator {
    public static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(30);

    private final MongoCollection<Document> collection;
    private final PermissionFilter permissionFilter;
    private final Function<String, PermissionFilter> relatedPermissionFilters;
    private final Duration timeout;

    public MongoAggregator(final MongoCollection<Document> collection) {
        this(collection, PermissionFilter.unrestricted(), related -> PermissionFilter.unrestricted(), DEFAULT_TIMEOUT);
    }

    private MongoAggregator(final MongoCollection<Document> collection,
                            final PermissionFilter permissionFilter,
                            final Function<String, PermissionFilter> relatedPermissionFilters,
                            final Duration timeout) {
        this.collection = Objects.requireNonNull(collection, "collection");
        this.permissionFilter = Objects.requireNonNull(permissionFilter, "permissionFilter");
        this.relatedPermissionFilters = Objects.requireNonNull(relatedPermissionFilters, "relatedPermissionFilters");
        this.timeout = Objects.requireNonNull(timeout, "timeout");
    }

    public MongoAggregator withTimeout(final Duration timeout) {
        return new MongoAggregator(collection, permissionFilter, relatedPermissionFilters, timeout);
    }

    /**
     * @param permissionFilter         the documents of the collection the user may read
     * @param relatedPermissionFilters the documents the user may read, per related collection
     */
    MongoAggregator withPermissions(final PermissionFilter permissionFilter,
                                    final Function<String, PermissionFilter> relatedPermissionFilters) {
        return new MongoAggregator(collection, permissionFilter, relatedPermissionFilters, timeout);
    }

    public BucketAggregationResult buckets(final BucketAggregationRequest request) {
        return BucketQuery.run(collection, permitted(request), timeout);
    }

    private BucketAggregationRequest permitted(final BucketAggregationRequest request) {
        final Grouping grouping = request.grouping().source() instanceof RelatedSource related
                ? request.grouping().withSource(permitted(related))
                : request.grouping();
        return new BucketAggregationRequest(and(permissionFilter, request.documentFilter()), grouping, request.order(),
                request.limit());
    }

    // A document whose related documents the user may not read counts as missing, like one without any.
    private RelatedSource permitted(final RelatedSource related) {
        final PermissionFilter relatedPermissionFilter = relatedPermissionFilters.apply(related.relatedCollection());
        if (relatedPermissionFilter.isUnrestricted()) {
            return related;
        }
        return related.withRelatedFilter(and(relatedPermissionFilter, related.relatedFilter()));
    }

    private static Bson and(final PermissionFilter permissionFilter, @Nullable final Bson filter) {
        if (permissionFilter.isUnrestricted()) {
            return filter;
        }
        return filter == null ? permissionFilter.filter() : Filters.and(permissionFilter.filter(), filter);
    }
}
