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
import io.searchbox.core.search.aggregation.DateRangeAggregation;
import org.graylog.shaded.elasticsearch5.org.elasticsearch.search.aggregations.AggregationBuilder;
import org.graylog.shaded.elasticsearch5.org.elasticsearch.search.aggregations.AggregationBuilders;
import org.graylog.shaded.elasticsearch5.org.elasticsearch.search.aggregations.bucket.range.date.DateRangeAggregationBuilder;
import org.graylog.plugins.views.search.Query;
import org.graylog.storage.elasticsearch6.views.ESGeneratedQueryContext;
import org.graylog.storage.elasticsearch6.views.searchtypes.pivot.ESPivot;
import org.graylog.storage.elasticsearch6.views.searchtypes.pivot.ESPivotBucketSpecHandler;
import org.graylog.plugins.views.search.searchtypes.pivot.Pivot;
import org.graylog.plugins.views.search.searchtypes.pivot.buckets.DateRangeBucket;
import org.joda.time.base.AbstractDateTime;

import javax.annotation.Nonnull;
import java.util.Optional;
import java.util.stream.Stream;

public class ESDateRangeHandler extends ESPivotBucketSpecHandler<DateRangeBucket, DateRangeAggregation> {
    @Nonnull
    @Override
    public Optional<AggregationBuilder> doCreateAggregation(String name, Pivot pivot, DateRangeBucket dateRangeBucket, ESPivot searchTypeHandler, ESGeneratedQueryContext esGeneratedQueryContext, Query query) {
        final DateRangeAggregationBuilder builder = AggregationBuilders.dateRange(name).field(dateRangeBucket.field());
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
        record(esGeneratedQueryContext, pivot, dateRangeBucket, name, DateRangeAggregation.class);

        return Optional.of(builder);
    }

    @Override
    public Stream<Bucket> doHandleResult(Pivot pivot,
                                         DateRangeBucket dateRangeBucket,
                                         SearchResult searchResult,
                                         DateRangeAggregation rangeAggregation,
                                         ESPivot searchTypeHandler,
                                         ESGeneratedQueryContext esGeneratedQueryContext) {
        if (dateRangeBucket.bucketKey().equals(DateRangeBucket.BucketKey.TO)) {
            return rangeAggregation.getBuckets().stream()
                    .map(range -> Bucket.create(range.getToAsString(), range));
        } else {
            return rangeAggregation.getBuckets().stream()
                    .map(range -> Bucket.create(range.getFromAsString(), range));
        }
    }
}
