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
package org.graylog.storage.elasticsearch6.views.searchtypes.pivot.buckets;

import io.searchbox.core.search.aggregation.DateHistogramAggregation;
import org.graylog.plugins.views.search.Query;
import org.graylog.plugins.views.search.searchtypes.pivot.Pivot;
import org.graylog.plugins.views.search.searchtypes.pivot.PivotSort;
import org.graylog.plugins.views.search.searchtypes.pivot.SeriesSort;
import org.graylog.plugins.views.search.searchtypes.pivot.SeriesSpec;
import org.graylog.plugins.views.search.searchtypes.pivot.SortSpec;
import org.graylog.plugins.views.search.searchtypes.pivot.buckets.Time;
import org.graylog.shaded.elasticsearch6.org.elasticsearch.search.aggregations.AggregationBuilder;
import org.graylog.shaded.elasticsearch6.org.elasticsearch.search.aggregations.AggregationBuilders;
import org.graylog.shaded.elasticsearch6.org.elasticsearch.search.aggregations.BucketOrder;
import org.graylog.shaded.elasticsearch6.org.elasticsearch.search.aggregations.bucket.histogram.DateHistogramAggregationBuilder;
import org.graylog.shaded.elasticsearch6.org.elasticsearch.search.aggregations.bucket.histogram.DateHistogramInterval;
import org.graylog.storage.elasticsearch6.views.ESGeneratedQueryContext;
import org.graylog.storage.elasticsearch6.views.searchtypes.pivot.ESPivotBucketSpecHandler;

import javax.annotation.Nonnull;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

public class ESTimeHandler extends ESPivotBucketSpecHandler<Time, DateHistogramAggregation> {
    @Nonnull
    @Override
    public Optional<AggregationBuilder> doCreateAggregation(String name, Pivot pivot, Time timeSpec, ESGeneratedQueryContext esGeneratedQueryContext, Query query) {
        final DateHistogramInterval dateHistogramInterval = new DateHistogramInterval(timeSpec.interval().toDateInterval(query.effectiveTimeRange(pivot)).toString());
        final Optional<BucketOrder> ordering = orderForPivot(pivot, timeSpec, esGeneratedQueryContext);
        final DateHistogramAggregationBuilder builder = AggregationBuilders.dateHistogram(name)
                .dateHistogramInterval(dateHistogramInterval)
                .field(timeSpec.field())
                .order(ordering.orElse(BucketOrder.key(true)))
                .format("date_time");
        record(esGeneratedQueryContext, pivot, timeSpec, name, DateHistogramAggregation.class);

        return Optional.of(builder);
    }


    private Optional<BucketOrder> orderForPivot(Pivot pivot, Time timeSpec, ESGeneratedQueryContext esGeneratedQueryContext) {
        return pivot.sort()
                .stream()
                .map(sortSpec -> {
                    if (sortSpec instanceof PivotSort && timeSpec.field().equals(sortSpec.field())) {
                        return sortSpec.direction().equals(SortSpec.Direction.Ascending) ? BucketOrder.key(true) :  BucketOrder.key(false);
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
                                    return BucketOrder.aggregation(esGeneratedQueryContext.seriesName(seriesSpec, pivot), sortSpec.direction().equals(SortSpec.Direction.Ascending));
                                })
                                .orElse(null);
                    }

                    return null;
                })
                .filter(Objects::nonNull)
                .findFirst();
    }

    @Override
    public Stream<Bucket> doHandleResult(Time bucketSpec,
                                         DateHistogramAggregation dateHistogramAggregation) {
        return dateHistogramAggregation.getBuckets().stream()
                .map(dateHistogram -> Bucket.create(dateHistogram.getTimeAsString(), dateHistogram));
    }
}
