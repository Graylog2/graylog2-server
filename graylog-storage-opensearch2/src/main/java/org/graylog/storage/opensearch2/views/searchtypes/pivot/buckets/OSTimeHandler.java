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
package org.graylog.storage.opensearch2.views.searchtypes.pivot.buckets;

import com.google.common.collect.ImmutableList;
import org.graylog.plugins.views.search.Query;
import org.graylog.plugins.views.search.searchtypes.pivot.BucketSpec;
import org.graylog.plugins.views.search.searchtypes.pivot.Pivot;
import org.graylog.plugins.views.search.searchtypes.pivot.PivotSort;
import org.graylog.plugins.views.search.searchtypes.pivot.SeriesSort;
import org.graylog.plugins.views.search.searchtypes.pivot.SeriesSpec;
import org.graylog.plugins.views.search.searchtypes.pivot.SortSpec;
import org.graylog.plugins.views.search.searchtypes.pivot.buckets.Time;
import org.graylog.shaded.opensearch2.org.opensearch.search.aggregations.bucket.MultiBucketsAggregation;
import org.graylog.storage.opensearch2.views.OSGeneratedQueryContext;
import org.graylog.storage.opensearch2.views.searchtypes.pivot.OSPivotBucketSpecHandler;
import org.graylog.shaded.opensearch2.org.opensearch.search.aggregations.AggregationBuilder;
import org.graylog.shaded.opensearch2.org.opensearch.search.aggregations.AggregationBuilders;
import org.graylog.shaded.opensearch2.org.opensearch.search.aggregations.BucketOrder;
import org.graylog.shaded.opensearch2.org.opensearch.search.aggregations.bucket.histogram.DateHistogramAggregationBuilder;
import org.graylog.shaded.opensearch2.org.opensearch.search.aggregations.bucket.histogram.DateHistogramInterval;
import org.graylog.shaded.opensearch2.org.opensearch.search.aggregations.bucket.histogram.ParsedDateHistogram;
import org.jooq.lambda.tuple.Tuple2;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

public class OSTimeHandler extends OSPivotBucketSpecHandler<Time, ParsedDateHistogram> {
    private static final String AGG_NAME = "agg";

    @Nonnull
    @Override
    public Optional<CreatedAggregations<AggregationBuilder>> doCreateAggregation(String name, Pivot pivot, List<Time> bucketSpec, OSGeneratedQueryContext queryContext, Query query) {
        AggregationBuilder root = null;
        AggregationBuilder leaf = null;

        for (Time timeSpec : bucketSpec) {
            final DateHistogramInterval dateHistogramInterval = new DateHistogramInterval(timeSpec.interval().toDateInterval(query.effectiveTimeRange(pivot)).toString());
            final Optional<BucketOrder> ordering = orderForPivot(pivot, timeSpec, queryContext);
            final DateHistogramAggregationBuilder builder = AggregationBuilders.dateHistogram(name)
                    .field(timeSpec.field())
                    .order(ordering.orElse(BucketOrder.key(true)))
                    .format("date_time");

            setInterval(builder, dateHistogramInterval);

            if (root == null && leaf == null) {
                root = builder;
                leaf = builder;
            } else {
                leaf.subAggregation(builder);
                leaf = builder;
            }
        }

        return Optional.of(CreatedAggregations.create(root, leaf));
    }

    private void setInterval(DateHistogramAggregationBuilder builder, DateHistogramInterval interval) {
        if (DateHistogramAggregationBuilder.DATE_FIELD_UNITS.get(interval.toString()) != null) {
            builder.calendarInterval(interval);
        } else {
            builder.fixedInterval(interval);
        }
    }

    private Optional<BucketOrder> orderForPivot(Pivot pivot, Time timeSpec, OSGeneratedQueryContext OSGeneratedQueryContext) {
        return pivot.sort()
                .stream()
                .map(sortSpec -> {
                    if (sortSpec instanceof PivotSort && timeSpec.field().equals(sortSpec.field())) {
                        return sortSpec.direction().equals(SortSpec.Direction.Ascending) ? BucketOrder.key(true) : BucketOrder.key(false);
                    }
                    if (sortSpec instanceof SeriesSort) {
                        final Optional<SeriesSpec> matchingSeriesSpec = pivot.series()
                                .stream()
                                .filter(series -> series.literal().equals(sortSpec.field()))
                                .findFirst();
                        return matchingSeriesSpec
                                .map(seriesSpec -> {
                                    if (seriesSpec.literal().equals("count()")) {
                                        return sortSpec.direction().equals(SortSpec.Direction.Ascending) ? BucketOrder.count(true) : BucketOrder.count(false);
                                    }
                                    return BucketOrder.aggregation(OSGeneratedQueryContext.seriesName(seriesSpec, pivot), sortSpec.direction().equals(SortSpec.Direction.Ascending));
                                })
                                .orElse(null);
                    }

                    return null;
                })
                .filter(Objects::nonNull)
                .findFirst();
    }

    @Override
    public Stream<Tuple2<ImmutableList<String>, MultiBucketsAggregation.Bucket>> extractBuckets(List<BucketSpec> bucketSpecs, Tuple2<ImmutableList<String>, MultiBucketsAggregation.Bucket> initialBucket) {
        if (bucketSpecs.isEmpty()) {
            return Stream.empty();
        }
        final ImmutableList<String> previousKeys = initialBucket.v1();
        final MultiBucketsAggregation.Bucket previousBucket = initialBucket.v2();
        final MultiBucketsAggregation aggregation = previousBucket.getAggregations().get(AGG_NAME);
        return aggregation.getBuckets().stream()
                .flatMap(bucket -> {
                    final ImmutableList<String> keys = ImmutableList.<String>builder()
                            .addAll(previousKeys)
                            .add(bucket.getKeyAsString())
                            .build();

                    if (bucketSpecs.size() == 1) {
                        return Stream.of(new Tuple2<>(keys, bucket));
                    }

                    return extractBuckets(bucketSpecs.subList(0, bucketSpecs.size()), new Tuple2<>(keys, bucket));
                });
    }
}
