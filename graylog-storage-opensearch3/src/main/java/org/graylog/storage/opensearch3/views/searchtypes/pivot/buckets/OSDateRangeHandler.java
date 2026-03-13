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
import org.graylog.plugins.views.search.searchtypes.pivot.buckets.DateRangeBucket;
import org.graylog.storage.opensearch3.views.OSGeneratedQueryContext;
import org.graylog.storage.opensearch3.views.searchtypes.pivot.MutableNamedAggregationBuilder;
import org.graylog.storage.opensearch3.views.searchtypes.pivot.OSPivotBucketSpecHandler;
import org.graylog.storage.opensearch3.views.searchtypes.pivot.PivotBucket;
import org.joda.time.base.AbstractDateTime;
import org.opensearch.client.opensearch._types.aggregations.Aggregation;
import org.opensearch.client.opensearch._types.aggregations.DateRangeAggregate;
import org.opensearch.client.opensearch._types.aggregations.DateRangeAggregation;
import org.opensearch.client.opensearch._types.aggregations.DateRangeExpression;
import org.opensearch.client.opensearch._types.aggregations.MultiBucketBase;

import javax.annotation.Nonnull;
import java.util.stream.Stream;

public class OSDateRangeHandler extends OSPivotBucketSpecHandler<DateRangeBucket> {
    private static final String AGG_NAME = "agg";
    @Nonnull
    @Override
    public CreatedAggregations<MutableNamedAggregationBuilder> doCreateAggregation(Direction direction, String name, Pivot pivot, DateRangeBucket dateRangeBucket, OSGeneratedQueryContext queryContext, Query query) {
        MutableNamedAggregationBuilder root = null;
        MutableNamedAggregationBuilder leaf = null;
        // need to iterate through the list reverse to be able to create subaggregations
        for (String field : dateRangeBucket.fields()) {
            final DateRangeAggregation.Builder dateRangeBuilder = DateRangeAggregation.builder()
                    .field(field);
            dateRangeBucket.ranges().forEach(r -> {
                final String from = r.from().map(AbstractDateTime::toString).orElse(null);
                final String to = r.to().map(AbstractDateTime::toString).orElse(null);
                DateRangeExpression.Builder range = DateRangeExpression.builder();
                if (from != null) {
                    range.from(f -> f.expr(from));
                }
                if (to != null) {
                    range.to(f -> f.expr(to));
                }
                dateRangeBuilder.ranges(range.build());
            });
            dateRangeBuilder.format("date_time");
            dateRangeBuilder.keyed(false);

            queryContext.recordNameForPivotSpec(pivot, dateRangeBucket, name);

            MutableNamedAggregationBuilder builder = new MutableNamedAggregationBuilder(
                    name,
                    Aggregation.builder().dateRange(dateRangeBuilder.build())
            );

            if (root == null && leaf == null) {
                root = builder;
                leaf = builder;
            } else {
                leaf.subAggregation(builder);
                leaf = builder;
            }
        }

        return CreatedAggregations.create(root, leaf);
    }

    @Override
    public Stream<PivotBucket> extractBuckets(Pivot pivot, BucketSpec bucketSpecs, PivotBucket initialBucket) {
        final ImmutableList<String> previousKeys = initialBucket.keys();
        final MultiBucketBase previousBucket = initialBucket.bucket();
        final DateRangeAggregate aggregation = previousBucket.aggregations().get(AGG_NAME).dateRange();
        final DateRangeBucket dateRangeBucket = (DateRangeBucket) bucketSpecs;

        return aggregation.buckets().array().stream()
                .flatMap(bucket -> {
                    final String bucketKey = dateRangeBucket.bucketKey().equals(DateRangeBucket.BucketKey.TO)
                            ? bucket.toAsString()
                            : bucket.fromAsString();
                    final ImmutableList<String> keys = ImmutableList.<String>builder()
                            .addAll(previousKeys)
                            .add(bucketKey)
                            .build();

                    return Stream.of(PivotBucket.create(keys, bucket));
                });
    }
}
