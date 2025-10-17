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
package org.graylog.plugins.views.search.rest.scriptingapi.mapping;

import org.graylog.plugins.views.search.rest.scriptingapi.request.Grouping;
import org.graylog.plugins.views.search.searchtypes.pivot.BucketSpec;
import org.graylog.plugins.views.search.searchtypes.pivot.buckets.DateRange;
import org.graylog.plugins.views.search.searchtypes.pivot.buckets.DateRangeBucket;
import org.graylog.plugins.views.search.searchtypes.pivot.buckets.Values;
import org.graylog2.plugin.Message;
import org.graylog2.plugin.indexer.searches.timeranges.TimeRange;
import org.joda.time.DateTime;
import org.joda.time.Duration;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;

public class GroupingToBucketSpecMapper implements BiFunction<Grouping, TimeRange, BucketSpec> {

    @Override
    public BucketSpec apply(final Grouping grouping,
                            final TimeRange timerange) {
        if (timerange != null && Message.FIELD_TIMESTAMP.equals(grouping.requestedField().name())) {
            return DateRangeBucket.builder()
                    .field(Message.FIELD_TIMESTAMP)
                    .type(DateRangeBucket.NAME)
                    .ranges(buildEqualDateRangeBuckets(timerange, grouping.limit()))
                    .build();
        } else {
            return Values.builder()
                    .field(grouping.requestedField().name())
                    .type(Values.NAME)
                    .limit(grouping.limit())
                    .build();
        }
    }

    private List<DateRange> buildEqualDateRangeBuckets(final TimeRange timeRange,
                                                       final int numBuckets) {
        final List<DateRange> ranges = new ArrayList<>(numBuckets);
        DateTime from = timeRange.getFrom();
        DateTime to = timeRange.getTo();
        final long bucketRangeInSeconds = new Duration(from, to).getStandardSeconds() / numBuckets;

        for (int i = 0; i < numBuckets; i++) {
            ranges.add(DateRange.builder()
                    .from(from.plusSeconds((int) (i * bucketRangeInSeconds)))
                    .to(to.plusSeconds((int) ((i + 1) * bucketRangeInSeconds)))
                    .build());
        }
        return ranges;
    }
}
