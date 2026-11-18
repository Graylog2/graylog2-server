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

import org.bson.conversions.Bson;
import org.graylog2.rest.models.SortOrder;

import java.util.Objects;

import static org.graylog2.shared.utilities.StringUtils.f;

/**
 * Groups the documents matching a filter into buckets and counts the documents in each bucket.
 *
 * @param filter   selects the documents to aggregate, before anything else runs. This is where a caller restricts
 *                 the documents to those a user may see; it should be backed by an index.
 * @param grouping how documents are assigned to buckets
 * @param order    the order of buckets, which decides which ones the limit keeps
 * @param limit    how many buckets to return; the rest are summed up as "other" in the result
 */
public record BucketAggregation(Bson filter, Grouping grouping, BucketOrder order, int limit) {
    public static final int DEFAULT_LIMIT = 100;
    public static final int MAX_LIMIT = 10_000;

    public BucketAggregation {
        Objects.requireNonNull(filter, "filter");
        Objects.requireNonNull(grouping, "grouping");
        Objects.requireNonNull(order, "order");
        if (limit < 1 || limit > MAX_LIMIT) {
            throw new IllegalArgumentException(f("Limit must be between 1 and %d, got %d", MAX_LIMIT, limit));
        }
    }

    /**
     * Creates an aggregation ordered by count, descending, returning up to {@link #DEFAULT_LIMIT} buckets.
     */
    public static BucketAggregation of(final Bson filter, final Grouping grouping) {
        return new BucketAggregation(filter, grouping, BucketOrder.byCount(SortOrder.DESCENDING), DEFAULT_LIMIT);
    }

    public BucketAggregation withOrder(final BucketOrder order) {
        return new BucketAggregation(filter, grouping, order, limit);
    }

    public BucketAggregation withLimit(final int limit) {
        return new BucketAggregation(filter, grouping, order, limit);
    }
}
