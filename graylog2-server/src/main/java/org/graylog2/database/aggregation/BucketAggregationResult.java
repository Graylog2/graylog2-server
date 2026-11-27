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

import org.graylog2.database.aggregation.grouping.Terms;

import java.util.List;

/**
 * The result of a {@link BucketAggregationRequest}.
 * <p>
 * A document with several values belongs to several buckets, so bucket counts can add up to more than the number of
 * matched documents.
 *
 * @param buckets          the buckets with a value, in the requested order, up to the limit
 * @param missingCount     the number of documents without a value; never subject to the limit
 * @param otherBucketCount the number of buckets left out by the limit
 * @param otherCount       the sum of the counts of the buckets left out by the limit. A document in several of them
 *                         counts several times.
 */
public record BucketAggregationResult(List<Bucket> buckets, long missingCount, long otherBucketCount, long otherCount) {
    public BucketAggregationResult {
        buckets = List.copyOf(buckets);
    }

    /**
     * The number of distinct keys, including those left out by the limit. For a {@link Terms} grouping, this
     * is the number of distinct values of the field, counting the elements of arrays.
     */
    public long bucketCount() {
        return buckets.size() + otherBucketCount;
    }

    public boolean isTruncated() {
        return otherBucketCount > 0;
    }

    /**
     * @param key   the value the bucket stands for: a value of the grouped field, a range key, or the start of a date
     *              histogram period. Integers are widened to {@link Long} and dates converted to
     *              {@link java.time.Instant}.
     * @param count the number of documents in the bucket
     */
    public record Bucket(Object key, long count) {}
}
