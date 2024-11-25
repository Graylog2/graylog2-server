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
import org.graylog.plugins.views.search.searchtypes.pivot.buckets.AutoInterval;
import org.graylog.plugins.views.search.searchtypes.pivot.buckets.Interval;
import org.graylog.plugins.views.search.searchtypes.pivot.buckets.Time;
import org.graylog.shaded.opensearch2.org.opensearch.search.aggregations.AggregationBuilder;
import org.graylog.shaded.opensearch2.org.opensearch.search.aggregations.AggregationBuilders;
import org.graylog.shaded.opensearch2.org.opensearch.search.aggregations.BucketOrder;
import org.graylog.shaded.opensearch2.org.opensearch.search.aggregations.bucket.MultiBucketsAggregation;
import org.graylog.shaded.opensearch2.org.opensearch.search.aggregations.bucket.histogram.AutoDateHistogramAggregationBuilder;
import org.graylog.shaded.opensearch2.org.opensearch.search.aggregations.bucket.histogram.DateHistogramAggregationBuilder;
import org.graylog.shaded.opensearch2.org.opensearch.search.aggregations.bucket.histogram.DateHistogramInterval;
import org.graylog.storage.opensearch2.views.OSGeneratedQueryContext;
import org.graylog.storage.opensearch2.views.searchtypes.pivot.OSPivotBucketSpecHandler;
import org.graylog.storage.opensearch2.views.searchtypes.pivot.PivotBucket;
import org.graylog2.plugin.indexer.searches.timeranges.RelativeRange;
import org.graylog2.plugin.indexer.searches.timeranges.TimeRange;

import javax.annotation.Nonnull;
import java.util.stream.Stream;

public class OSTimeHandler extends OSPivotBucketSpecHandler<Time> {
    private static final String AGG_NAME = "agg";
    private static final BucketOrder defaultOrder = BucketOrder.key(true);
    private static final int BASE_NUM_BUCKETS = 25;
    public static final String DATE_TIME_FORMAT = "date_time";

    @Nonnull
    @Override
    public CreatedAggregations<AggregationBuilder> doCreateAggregation(Direction direction, String name, Pivot pivot, Time timeSpec, OSGeneratedQueryContext queryContext, Query query) {
        AggregationBuilder root = null;
        AggregationBuilder leaf = null;
        final Interval interval = timeSpec.interval();
        final TimeRange timerange = query.timerange();
        if (interval instanceof AutoInterval autoInterval
                && isAllMessages(timerange)) {
            for (String timeField : timeSpec.fields()) {
                final AutoDateHistogramAggregationBuilder builder = new AutoDateHistogramAggregationBuilder(name)
                        .field(timeField)
                        .setNumBuckets((int) (BASE_NUM_BUCKETS / autoInterval.scaling()))
                        .format(DATE_TIME_FORMAT);

                if (root == null && leaf == null) {
                    root = builder;
                    leaf = builder;
                } else {
                    leaf.subAggregation(builder);
                    leaf = builder;
                }
            }
        } else {
            for (String timeField : timeSpec.fields()) {
                final DateHistogramInterval dateHistogramInterval = new DateHistogramInterval(interval.toDateInterval(query.effectiveTimeRange(pivot)).toString());
                final var ordering = orderListForPivot(pivot, queryContext, defaultOrder, query);
                final DateHistogramAggregationBuilder builder = AggregationBuilders.dateHistogram(name)
                        .field(timeField)
                        .order(ordering.orders())
                        .format(DATE_TIME_FORMAT);

                ordering.sortingAggregations().forEach(builder::subAggregation);

                setInterval(builder, dateHistogramInterval);

                if (root == null && leaf == null) {
                    root = builder;
                    leaf = builder;
                } else {
                    leaf.subAggregation(builder);
                    leaf = builder;
                }
            }
        }

        return CreatedAggregations.create(root, leaf);
    }

    private boolean isAllMessages(final TimeRange timerange) {
        return timerange instanceof RelativeRange
                && ((RelativeRange) timerange).isAllMessages();
    }

    private void setInterval(DateHistogramAggregationBuilder builder, DateHistogramInterval interval) {
        if (DateHistogramAggregationBuilder.DATE_FIELD_UNITS.get(interval.toString()) != null) {
            builder.calendarInterval(interval);
        } else {
            builder.fixedInterval(interval);
        }
    }

    @Override
    public Stream<PivotBucket> extractBuckets(Pivot pivot, BucketSpec bucketSpecs, PivotBucket initialBucket) {
        final ImmutableList<String> previousKeys = initialBucket.keys();
        final MultiBucketsAggregation.Bucket previousBucket = initialBucket.bucket();
        final MultiBucketsAggregation aggregation = previousBucket.getAggregations().get(AGG_NAME);
        return aggregation.getBuckets().stream()
                .flatMap(bucket -> {
                    final ImmutableList<String> keys = ImmutableList.<String>builder()
                            .addAll(previousKeys)
                            .add(bucket.getKeyAsString())
                            .build();

                    return Stream.of(PivotBucket.create(keys, bucket));
                });
    }
}
