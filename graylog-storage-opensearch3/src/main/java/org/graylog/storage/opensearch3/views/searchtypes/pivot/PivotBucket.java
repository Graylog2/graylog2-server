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
package org.graylog.storage.opensearch3.views.searchtypes.pivot;

import com.google.common.collect.ImmutableList;
import jakarta.annotation.Nullable;
import org.opensearch.client.opensearch._types.aggregations.MultiBucketBase;

import java.util.List;
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
 * @param otherParent   for the synthetic {@code (Other)} bucket, the enclosing {@code filters} bucket that the
 *                      tail was subtracted from. {@code null} for every ordinary bucket.
 * @param otherSiblings for the synthetic {@code (Other)} bucket, the shown buckets that made the limit at that
 *                      grouping level. Empty for every ordinary bucket. Together with {@code otherParent}, this
 *                      lets {@code PivotResultProcessor} derive the {@code (Other)} row's per-column cells.
 */
public record PivotBucket(ImmutableList<String> keys,
                          MultiBucketBase bucket,
                          boolean isOtherBucket,
                          Map<String, Object> derivedValues,
                          @Nullable MultiBucketBase otherParent,
                          List<MultiBucketBase> otherSiblings) {

    public static PivotBucket create(ImmutableList<String> keys, MultiBucketBase bucket) {
        return new PivotBucket(keys, bucket, false, Map.of(), null, List.of());
    }

    public static PivotBucket createOther(ImmutableList<String> keys,
                                          MultiBucketBase bucket,
                                          Map<String, Object> derivedValues,
                                          @Nullable MultiBucketBase otherParent,
                                          List<MultiBucketBase> otherSiblings) {
        return new PivotBucket(keys, bucket, true, Map.copyOf(derivedValues), otherParent, List.copyOf(otherSiblings));
    }
}
