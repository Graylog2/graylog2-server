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
package org.graylog.storage.opensearch2.views.searchtypes.pivot;

import com.google.common.collect.ImmutableList;
import org.graylog.shaded.opensearch2.org.opensearch.search.aggregations.bucket.MultiBucketsAggregation;

import java.util.Map;

/**
 * A bucket extracted from a search response, together with the grouping keys leading to it.
 *
 * @param isOtherBucket whether this is the synthetic {@code (Other)} bucket. This is the bucket's identity;
 *                      {@code derivedValues} is only its payload and must never be re-derived from it.
 * @param derivedValues for the synthetic {@code (Other)} bucket, the series values computed by
 *                      {@code OtherBucketDerivation}, keyed by the bare {@code seriesSpec.id()}. The column key
 *                      path is prepended separately by {@code PivotResultProcessor} when it emits each value.
 *                      Empty for every ordinary bucket, whose values come from the series handlers.
 */
public record PivotBucket(ImmutableList<String> keys,
                          MultiBucketsAggregation.Bucket bucket,
                          boolean isOtherBucket,
                          Map<String, Object> derivedValues) {

    public static PivotBucket create(ImmutableList<String> keys, MultiBucketsAggregation.Bucket bucket) {
        return new PivotBucket(keys, bucket, false, Map.of());
    }

    public static PivotBucket createOther(ImmutableList<String> keys,
                                          MultiBucketsAggregation.Bucket bucket,
                                          Map<String, Object> derivedValues) {
        return new PivotBucket(keys, bucket, true, Map.copyOf(derivedValues));
    }
}
