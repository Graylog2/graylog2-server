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

import org.graylog.plugins.views.search.Query;
import org.graylog.plugins.views.search.searchtypes.pivot.Pivot;
import org.graylog.plugins.views.search.searchtypes.pivot.buckets.DateRangeBucket;
import org.graylog.storage.opensearch2.views.OSGeneratedQueryContext;
import org.graylog.storage.opensearch2.views.searchtypes.pivot.OSPivotBucketSpecHandler;
import org.joda.time.base.AbstractDateTime;
import org.graylog.shaded.opensearch2.org.opensearch.search.aggregations.AggregationBuilder;
import org.graylog.shaded.opensearch2.org.opensearch.search.aggregations.AggregationBuilders;
import org.graylog.shaded.opensearch2.org.opensearch.search.aggregations.bucket.range.DateRangeAggregationBuilder;
import org.graylog.shaded.opensearch2.org.opensearch.search.aggregations.bucket.range.ParsedDateRange;

import javax.annotation.Nonnull;
import java.util.Optional;
import java.util.stream.Stream;

public class OSDateRangeHandler extends OSPivotBucketSpecHandler<DateRangeBucket, ParsedDateRange> {
    @Nonnull
    @Override
    public Optional<AggregationBuilder> doCreateAggregation(String name, Pivot pivot, DateRangeBucket dateRangeBucket, OSGeneratedQueryContext OSGeneratedQueryContext, Query query) {
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
        record(OSGeneratedQueryContext, pivot, dateRangeBucket, name, ParsedDateRange.class);

        return Optional.of(builder);
    }

    @Override
    public Stream<Bucket> doHandleResult(
            DateRangeBucket dateRangeBucket,
            ParsedDateRange rangeAggregation) {
        if (dateRangeBucket.bucketKey().equals(DateRangeBucket.BucketKey.TO)) {
            return rangeAggregation.getBuckets().stream()
                    .map(range -> Bucket.create(range.getToAsString(), range));
        } else {
            return rangeAggregation.getBuckets().stream()
                    .map(range -> Bucket.create(range.getFromAsString(), range));
        }
    }
}
