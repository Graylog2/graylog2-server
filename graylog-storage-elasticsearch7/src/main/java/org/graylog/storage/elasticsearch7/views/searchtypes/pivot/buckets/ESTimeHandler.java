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

import org.graylog.plugins.views.search.Query;
import org.graylog.plugins.views.search.searchtypes.pivot.Pivot;
import org.graylog.plugins.views.search.searchtypes.pivot.buckets.Time;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.action.search.SearchResponse;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.search.aggregations.AggregationBuilder;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.search.aggregations.bucket.histogram.AutoDateHistogramAggregationBuilder;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.search.aggregations.bucket.histogram.ParsedAutoDateHistogram;
import org.graylog.storage.elasticsearch7.views.ESGeneratedQueryContext;
import org.graylog.storage.elasticsearch7.views.searchtypes.pivot.ESPivot;
import org.graylog.storage.elasticsearch7.views.searchtypes.pivot.ESPivotBucketSpecHandler;

import javax.annotation.Nonnull;
import java.util.Optional;
import java.util.stream.Stream;

public class ESTimeHandler extends ESPivotBucketSpecHandler<Time, ParsedAutoDateHistogram> {
    @Nonnull
    @Override
    public Optional<AggregationBuilder> doCreateAggregation(String name, Pivot pivot, Time timeSpec, ESPivot searchTypeHandler, ESGeneratedQueryContext esGeneratedQueryContext, Query query) {
        final AutoDateHistogramAggregationBuilder builder = new AutoDateHistogramAggregationBuilder(name);
        builder.field(timeSpec.field())
                .format("date_time")
                .setNumBuckets(timeSpec.buckets());

        record(esGeneratedQueryContext, pivot, timeSpec, name, ParsedAutoDateHistogram.class);

        return Optional.of(builder);
    }

    @Override
    public Stream<Bucket> doHandleResult(Pivot pivot,
                                         Time bucketSpec,
                                         SearchResponse searchResult,
                                         ParsedAutoDateHistogram dateHistogramAggregation,
                                         ESPivot searchTypeHandler,
                                         ESGeneratedQueryContext esGeneratedQueryContext) {
        return dateHistogramAggregation.getBuckets().stream()
                .map(dateHistogram -> Bucket.create(dateHistogram.getKeyAsString(), dateHistogram));
    }
}
