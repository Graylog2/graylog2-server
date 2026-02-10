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
import org.graylog.plugins.views.search.searchtypes.pivot.buckets.AutoInterval;
import org.graylog.plugins.views.search.searchtypes.pivot.buckets.Interval;
import org.graylog.plugins.views.search.searchtypes.pivot.buckets.Time;
import org.graylog.storage.opensearch3.views.OSGeneratedQueryContext;
import org.graylog.storage.opensearch3.views.searchtypes.pivot.MutableNamedAggregationBuilder;
import org.graylog.storage.opensearch3.views.searchtypes.pivot.OSPivotBucketSpecHandler;
import org.graylog.storage.opensearch3.views.searchtypes.pivot.PivotBucket;
import org.graylog2.plugin.indexer.searches.timeranges.RelativeRange;
import org.graylog2.plugin.indexer.searches.timeranges.TimeRange;
import org.opensearch.client.opensearch._types.SortOrder;
import org.opensearch.client.opensearch._types.aggregations.Aggregate;
import org.opensearch.client.opensearch._types.aggregations.Aggregation;
import org.opensearch.client.opensearch._types.aggregations.AutoDateHistogramAggregation;
import org.opensearch.client.opensearch._types.aggregations.CalendarInterval;
import org.opensearch.client.opensearch._types.aggregations.DateHistogramAggregation;
import org.opensearch.client.opensearch._types.aggregations.DateHistogramBucket;
import org.opensearch.client.opensearch._types.aggregations.HistogramOrder;
import org.opensearch.client.opensearch._types.aggregations.MultiBucketAggregateBase;
import org.opensearch.client.opensearch._types.aggregations.MultiBucketBase;

import javax.annotation.Nonnull;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

public class OSTimeHandler extends OSPivotBucketSpecHandler<Time> {
    private static final String AGG_NAME = "agg";
    private static final BucketOrder defaultOrder = BucketOrder.key(SortOrder.Asc);
    private static final int BASE_NUM_BUCKETS = 25;
    public static final String DATE_TIME_FORMAT = "date_time";

    @Nonnull
    @Override
    public CreatedAggregations<MutableNamedAggregationBuilder> doCreateAggregation(Direction direction, String name, Pivot pivot, Time timeSpec, OSGeneratedQueryContext queryContext, Query query) {
        MutableNamedAggregationBuilder root = null;
        MutableNamedAggregationBuilder leaf = null;
        final var timeZone = queryContext.timezone().toTimeZone().getID();

        final Interval interval = timeSpec.interval();
        final TimeRange timerange = query.timerange();
        if (interval instanceof AutoInterval autoInterval
                && isAllMessages(timerange)) {
            for (String timeField : timeSpec.fields()) {
                AutoDateHistogramAggregation.Builder aggBuilder = AutoDateHistogramAggregation.builder()
                        .field(timeField)
                        .buckets((int) (BASE_NUM_BUCKETS / autoInterval.scaling()))
                        .format(DATE_TIME_FORMAT)
                        .timeZone(timeZone);

                AutoDateHistogramAggregation aggregation = aggBuilder.build();
                MutableNamedAggregationBuilder builder = new MutableNamedAggregationBuilder(
                        name,
                        Aggregation.builder().autoDateHistogram(aggregation)
                );

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
                String dateHistogramInterval = interval.toDateInterval(query.effectiveTimeRange(pivot)).toString();
                final var ordering = orderListForPivot(pivot, queryContext, defaultOrder, query);

                DateHistogramAggregation.Builder aggBuilder = DateHistogramAggregation.builder()
                        .field(timeField)
                        .order(mapOrders(ordering.orders()))
                        .format(DATE_TIME_FORMAT)
                        .timeZone(timeZone);
                setInterval(aggBuilder, dateHistogramInterval);

                DateHistogramAggregation aggregation = aggBuilder.build();
                MutableNamedAggregationBuilder builder = new MutableNamedAggregationBuilder(
                        name,
                        Aggregation.builder().dateHistogram(aggregation)
                                .aggregations(ordering.sortingAggregations())
                );
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

    private HistogramOrder mapOrders(List<BucketOrder> orders) {
        HistogramOrder.Builder builder = HistogramOrder.builder();
        orders.forEach(order -> {
            if (order.type() == BucketOrder.Type.KEY) {
                builder.key(order.order());
            } else if (order.type() == BucketOrder.Type.COUNT) {
                builder.count(order.order());
            }
        });
        return builder.build();
    }

    private boolean isAllMessages(final TimeRange timerange) {
        return timerange instanceof RelativeRange && timerange.isAllMessages();
    }

    private void setInterval(DateHistogramAggregation.Builder builder, String interval) {
        Arrays.stream(CalendarInterval.values())
                .filter(ci ->
                        ci.name().equals(interval) || (
                                ci.aliases() != null && Arrays.asList(ci.aliases()).contains(interval)
                        )
                ).findFirst()
                .ifPresentOrElse(
                        builder::calendarInterval,
                        () -> builder.fixedInterval(t -> t.time(interval))
                );
    }

    @Override
    public Stream<PivotBucket> extractBuckets(Pivot pivot, BucketSpec bucketSpecs, PivotBucket initialBucket) {
        final ImmutableList<String> previousKeys = initialBucket.keys();
        final MultiBucketBase previousBucket = initialBucket.bucket();
        final Aggregate aggregation = previousBucket.aggregations().get(AGG_NAME);
        final MultiBucketAggregateBase<DateHistogramBucket> dateHistogramAggregation =
                aggregation.isDateHistogram() ?
                        aggregation.dateHistogram() : aggregation.autoDateHistogram();
        return dateHistogramAggregation.buckets().array().stream()
                .flatMap(bucket -> {
                    final ImmutableList<String> keys = ImmutableList.<String>builder()
                            .addAll(previousKeys)
                            .add(bucket.keyAsString())
                            .build();

                    return Stream.of(PivotBucket.create(keys, bucket));
                });
    }
}
