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
package org.graylog.storage.opensearch3.views.searchtypes.pivot.buckets;

import com.google.common.collect.ImmutableList;
import org.graylog.plugins.views.search.Query;
import org.graylog.plugins.views.search.searchtypes.pivot.BucketSpec;
import org.graylog.plugins.views.search.searchtypes.pivot.Pivot;
import org.graylog.plugins.views.search.searchtypes.pivot.buckets.RangeBucket;
import org.graylog.storage.opensearch3.views.OSGeneratedQueryContext;
import org.graylog.storage.opensearch3.views.searchtypes.pivot.MutableNamedAggregationBuilder;
import org.graylog.storage.opensearch3.views.searchtypes.pivot.OSPivotBucketSpecHandler;
import org.graylog.storage.opensearch3.views.searchtypes.pivot.PivotBucket;
import org.opensearch.client.json.JsonData;
import org.opensearch.client.opensearch._types.aggregations.Aggregation;
import org.opensearch.client.opensearch._types.aggregations.AggregationRange;
import org.opensearch.client.opensearch._types.aggregations.MultiBucketBase;
import org.opensearch.client.opensearch._types.aggregations.RangeAggregate;
import org.opensearch.client.opensearch._types.aggregations.RangeAggregation;

import javax.annotation.Nonnull;
import java.util.Optional;
import java.util.stream.Stream;

public class OSRangeHandler extends OSPivotBucketSpecHandler<RangeBucket> {
    private static final String AGG_NAME = "agg";

    @Nonnull
    @Override
    public CreatedAggregations<MutableNamedAggregationBuilder> doCreateAggregation(Direction direction, String name, Pivot pivot, RangeBucket rangeBucket, OSGeneratedQueryContext queryContext, Query query) {
        MutableNamedAggregationBuilder root = null;
        MutableNamedAggregationBuilder leaf = null;
        for (final String field : rangeBucket.fields()) {
            final RangeAggregation.Builder rangeBuilder = new RangeAggregation.Builder()
                    .field(field);
            rangeBucket.ranges().forEach(r -> {
                final AggregationRange.Builder range = new AggregationRange.Builder();
                Optional.ofNullable(r.from()).ifPresent(from -> range.from(JsonData.of(from)));
                Optional.ofNullable(r.to()).ifPresent(to -> range.to(JsonData.of(to)));
                rangeBuilder.ranges(range.build());
            });
            rangeBuilder.keyed(false);

            queryContext.recordNameForPivotSpec(pivot, rangeBucket, name);

            final MutableNamedAggregationBuilder builder = new MutableNamedAggregationBuilder(
                    name,
                    Aggregation.builder().range(rangeBuilder.build())
            );

            if (root == null) {
                root = builder;
            } else {
                leaf.subAggregation(builder);
            }
            leaf = builder;
        }

        return CreatedAggregations.create(root, leaf);
    }

    @Override
    public Stream<PivotBucket> extractBuckets(Pivot pivot, BucketSpec bucketSpecs, PivotBucket initialBucket) {
        final ImmutableList<String> previousKeys = initialBucket.keys();
        final MultiBucketBase previousBucket = initialBucket.bucket();
        final RangeAggregate aggregation = previousBucket.aggregations().get(AGG_NAME).range();

        return aggregation.buckets().array().stream()
                .flatMap(bucket -> {
                    final String bucketKey = bucket.key();
                    final ImmutableList<String> keys = ImmutableList.<String>builder()
                            .addAll(previousKeys)
                            .add(bucketKey)
                            .build();

                    return Stream.of(PivotBucket.create(keys, bucket));
                });
    }
}
