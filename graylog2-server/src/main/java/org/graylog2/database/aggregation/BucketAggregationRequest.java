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

import com.mongodb.client.model.Filters;
import jakarta.annotation.Nullable;
import org.bson.conversions.Bson;
import org.graylog2.database.aggregation.grouping.Grouping;
import org.graylog2.rest.models.SortOrder;

import java.util.Objects;

import static org.graylog2.shared.utilities.StringUtils.f;

/**
 * Groups the documents matching a filter into buckets and counts the documents in each bucket.
 *
 * @param documentFilter selects the documents to aggregate, before anything else runs. This is where a caller
 *                       restricts the documents to those a user may see; it should be backed by an index.
 *                       {@code null} aggregates all documents of the collection.
 * @param grouping       how documents are assigned to buckets
 * @param order          the order of buckets, which decides which ones the limit keeps. {@code null} orders them by
 *                       {@link #DEFAULT_ORDER}.
 * @param limit          how many buckets to return; the rest are summed up as "other" in the result
 */
public record BucketAggregationRequest(Bson documentFilter, Grouping grouping, BucketOrder order, int limit) {
    public static final int DEFAULT_LIMIT = 100;
    public static final int MAX_LIMIT = 10_000;

    public static final BucketOrder DEFAULT_ORDER = BucketOrder.byCount(SortOrder.DESCENDING);

    public BucketAggregationRequest(@Nullable final Bson documentFilter,
                                    final Grouping grouping,
                                    @Nullable final BucketOrder order,
                                    final int limit) {
        this.documentFilter = documentFilter == null ? Filters.empty() : documentFilter;
        this.order = order == null ? DEFAULT_ORDER : order;
        this.grouping = Objects.requireNonNull(grouping, "grouping");
        if (limit < 1 || limit > MAX_LIMIT) {
            throw new IllegalArgumentException(f("Limit must be between 1 and %d, got %d", MAX_LIMIT, limit));
        }
        this.limit = limit;
    }

    /**
     * Creates an aggregation in the {@link #DEFAULT_ORDER}, returning up to {@link #DEFAULT_LIMIT} buckets.
     *
     * @param documentFilter selects the documents to aggregate; {@code null} aggregates all of them
     */
    public static BucketAggregationRequest of(@Nullable final Bson documentFilter, final Grouping grouping) {
        return new BucketAggregationRequest(documentFilter, grouping, DEFAULT_ORDER, DEFAULT_LIMIT);
    }

    /**
     * @param order the order of buckets; {@code null} restores the {@link #DEFAULT_ORDER}
     */
    public BucketAggregationRequest withOrder(@Nullable final BucketOrder order) {
        return new BucketAggregationRequest(documentFilter, grouping, order, limit);
    }

    public BucketAggregationRequest withLimit(final int limit) {
        return new BucketAggregationRequest(documentFilter, grouping, order, limit);
    }
}
