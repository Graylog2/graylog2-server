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
package org.graylog.storage.elasticsearch7.views.searchtypes.pivot.buckets;

import com.google.common.collect.ImmutableList;
import org.graylog.plugins.views.search.Query;
import org.graylog.plugins.views.search.searchtypes.pivot.BucketSpec;
import org.graylog.plugins.views.search.searchtypes.pivot.Pivot;
import org.graylog.plugins.views.search.searchtypes.pivot.buckets.DateRangeBucket;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.search.aggregations.AggregationBuilder;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.search.aggregations.AggregationBuilders;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.search.aggregations.bucket.MultiBucketsAggregation;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.search.aggregations.bucket.range.DateRangeAggregationBuilder;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.search.aggregations.bucket.range.ParsedDateRange;
import org.graylog.storage.elasticsearch7.views.ESGeneratedQueryContext;
import org.graylog.storage.elasticsearch7.views.searchtypes.pivot.ESPivotBucketSpecHandler;
import org.graylog.storage.elasticsearch7.views.searchtypes.pivot.PivotBucket;
import org.joda.time.base.AbstractDateTime;

import javax.annotation.Nonnull;
import java.util.stream.Stream;

public class ESDateRangeHandler extends ESPivotBucketSpecHandler<DateRangeBucket> {
    private static final String AGG_NAME = "agg";
    @Nonnull
    @Override
    public CreatedAggregations<AggregationBuilder> doCreateAggregation(Direction direction, String name, Pivot pivot, DateRangeBucket dateRangeBucket, ESGeneratedQueryContext queryContext, Query query) {
        AggregationBuilder root = null;
        AggregationBuilder leaf = null;
        for (String dateRangeField : dateRangeBucket.fields()) {
            final DateRangeAggregationBuilder builder = AggregationBuilders.dateRange(name).field(dateRangeField);
            dateRangeBucket.ranges().forEach(r -> {
                final String from = r.from().map(AbstractDateTime::toString).orElse(null);
                final String to = r.to().map(AbstractDateTime::toString).orElse(null);
                if (from != null && to != null) {
                    builder.addRange(from, to);
                } else if (to != null) {
                    builder.addUnboundedTo(to);
                } else if (from != null) {
                    builder.addUnboundedFrom(from);
                }
            });
            builder.format("date_time");
            builder.keyed(false);

            record(queryContext, pivot, dateRangeBucket, name, ParsedDateRange.class);

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
    public Stream<PivotBucket> extractBuckets(Pivot pivot, BucketSpec bucketSpec, PivotBucket initialBucket) {
        final ImmutableList<String> previousKeys = initialBucket.keys();
        final MultiBucketsAggregation.Bucket previousBucket = initialBucket.bucket();
        final ParsedDateRange aggregation = previousBucket.getAggregations().get(AGG_NAME);
        final DateRangeBucket dateRangeBucket = (DateRangeBucket) bucketSpec;

        return aggregation.getBuckets().stream()
                .flatMap(bucket -> {
                    final String bucketKey = dateRangeBucket.bucketKey().equals(DateRangeBucket.BucketKey.TO)
                            ? bucket.getToAsString()
                            : bucket.getFromAsString();
                    final ImmutableList<String> keys = ImmutableList.<String>builder()
                            .addAll(previousKeys)
                            .add(bucketKey)
                            .build();

                    return Stream.of(PivotBucket.create(keys, bucket, false));
                });
    }
}
