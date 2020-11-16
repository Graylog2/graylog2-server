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

import io.searchbox.core.SearchResult;
import io.searchbox.core.search.aggregation.DateHistogramAggregation;
import org.graylog.shaded.elasticsearch5.org.elasticsearch.search.aggregations.AggregationBuilder;
import org.graylog.shaded.elasticsearch5.org.elasticsearch.search.aggregations.AggregationBuilders;
import org.graylog.shaded.elasticsearch5.org.elasticsearch.search.aggregations.bucket.histogram.DateHistogramAggregationBuilder;
import org.graylog.shaded.elasticsearch5.org.elasticsearch.search.aggregations.bucket.histogram.DateHistogramInterval;
import org.graylog.shaded.elasticsearch5.org.elasticsearch.search.aggregations.bucket.histogram.Histogram;
import org.graylog.plugins.views.search.Query;
import org.graylog.storage.elasticsearch6.views.ESGeneratedQueryContext;
import org.graylog.storage.elasticsearch6.views.searchtypes.pivot.ESPivot;
import org.graylog.storage.elasticsearch6.views.searchtypes.pivot.ESPivotBucketSpecHandler;
import org.graylog.plugins.views.search.searchtypes.pivot.Pivot;
import org.graylog.plugins.views.search.searchtypes.pivot.PivotSort;
import org.graylog.plugins.views.search.searchtypes.pivot.SeriesSort;
import org.graylog.plugins.views.search.searchtypes.pivot.SeriesSpec;
import org.graylog.plugins.views.search.searchtypes.pivot.SortSpec;
import org.graylog.plugins.views.search.searchtypes.pivot.buckets.Time;

import javax.annotation.Nonnull;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

public class ESTimeHandler extends ESPivotBucketSpecHandler<Time, DateHistogramAggregation> {
    @Nonnull
    @Override
    public Optional<AggregationBuilder> doCreateAggregation(String name, Pivot pivot, Time timeSpec, ESPivot searchTypeHandler, ESGeneratedQueryContext esGeneratedQueryContext, Query query) {
        final DateHistogramInterval dateHistogramInterval = new DateHistogramInterval(timeSpec.interval().toDateInterval(query.effectiveTimeRange(pivot)).toString());
        final Optional<Histogram.Order> ordering = orderForPivot(pivot, timeSpec, esGeneratedQueryContext);
        final DateHistogramAggregationBuilder builder = AggregationBuilders.dateHistogram(name)
                .dateHistogramInterval(dateHistogramInterval)
                .field(timeSpec.field())
                .order(ordering.orElse(Histogram.Order.KEY_ASC))
                .format("date_time");
        record(esGeneratedQueryContext, pivot, timeSpec, name, DateHistogramAggregation.class);

        return Optional.of(builder);
    }


    private Optional<Histogram.Order> orderForPivot(Pivot pivot, Time timeSpec, ESGeneratedQueryContext esGeneratedQueryContext) {
        return pivot.sort()
                .stream()
                .map(sortSpec -> {
                    if (sortSpec instanceof PivotSort && timeSpec.field().equals(sortSpec.field())) {
                        return sortSpec.direction().equals(SortSpec.Direction.Ascending) ? Histogram.Order.KEY_ASC : Histogram.Order.KEY_DESC;
                    }
                    if (sortSpec instanceof SeriesSort) {
                        final Optional<SeriesSpec> matchingSeriesSpec = pivot.series()
                                .stream()
                                .filter(series -> series.literal().equals(sortSpec.field()))
                                .findFirst();
                        return matchingSeriesSpec
                                .map(seriesSpec -> {
                                    if (seriesSpec.literal().equals("count()")) {
                                        return sortSpec.direction().equals(SortSpec.Direction.Ascending) ? Histogram.Order.COUNT_ASC : Histogram.Order.COUNT_DESC;
                                    }
                                    return Histogram.Order.aggregation(esGeneratedQueryContext.seriesName(seriesSpec, pivot), sortSpec.direction().equals(SortSpec.Direction.Ascending));
                                })
                                .orElse(null);
                    }

                    return null;
                })
                .filter(Objects::nonNull)
                .findFirst();
    }

    @Override
    public Stream<Bucket> doHandleResult(Pivot pivot,
                                         Time bucketSpec,
                                         SearchResult searchResult,
                                         DateHistogramAggregation dateHistogramAggregation,
                                         ESPivot searchTypeHandler,
                                         ESGeneratedQueryContext esGeneratedQueryContext) {
        return dateHistogramAggregation.getBuckets().stream()
                .map(dateHistogram -> Bucket.create(dateHistogram.getTimeAsString(), dateHistogram));
    }
}
