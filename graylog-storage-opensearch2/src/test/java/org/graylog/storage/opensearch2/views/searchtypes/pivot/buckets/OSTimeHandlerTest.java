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
import org.graylog.plugins.views.search.searchtypes.pivot.BucketSpecHandler;
import org.graylog.plugins.views.search.searchtypes.pivot.Pivot;
import org.graylog.plugins.views.search.searchtypes.pivot.buckets.AutoInterval;
import org.graylog.plugins.views.search.searchtypes.pivot.buckets.DateInterval;
import org.graylog.plugins.views.search.searchtypes.pivot.buckets.Interval;
import org.graylog.plugins.views.search.searchtypes.pivot.buckets.Time;
import org.graylog.plugins.views.search.timeranges.DerivedTimeRange;
import org.graylog.shaded.opensearch2.org.opensearch.search.aggregations.AggregationBuilder;
import org.graylog.shaded.opensearch2.org.opensearch.search.aggregations.bucket.histogram.AutoDateHistogramAggregationBuilder;
import org.graylog.storage.opensearch2.views.OSGeneratedQueryContext;
import org.graylog.storage.opensearch2.views.searchtypes.pivot.AggTypes;
import org.graylog2.plugin.indexer.searches.timeranges.InvalidRangeParametersException;
import org.graylog2.plugin.indexer.searches.timeranges.RelativeRange;
import org.graylog2.plugin.indexer.searches.timeranges.TimeRange;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Collections;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.graylog.storage.opensearch2.views.searchtypes.pivot.buckets.OSTimeHandler.DATE_TIME_FORMAT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class OSTimeHandlerTest {
    private OSTimeHandler osTimeHandler;

    private final Pivot pivot = mock(Pivot.class);

    private final Time time = mock(Time.class);

    private final OSGeneratedQueryContext queryContext = mock(OSGeneratedQueryContext.class, RETURNS_DEEP_STUBS);

    private final Query query = mock(Query.class);

    private final Interval interval = mock(Interval.class, RETURNS_DEEP_STUBS);

    @BeforeEach
    public void setUp() throws Exception {
        this.osTimeHandler = new OSTimeHandler();
        when(time.interval()).thenReturn(interval);
        when(time.fields()).thenReturn(Collections.singletonList("foobar"));
        final AggTypes aggTypes = mock(AggTypes.class);
        when(queryContext.contextMap().get(any())).thenReturn(aggTypes);
        when(query.effectiveTimeRange(any())).thenCallRealMethod();
    }

    @Test
    public void timeSpecIntervalIsCalculatedOnPivotTimerangeIfOverridden() throws InvalidRangeParametersException {
        final ArgumentCaptor<TimeRange> timeRangeCaptor = ArgumentCaptor.forClass(TimeRange.class);
        when(interval.toDateInterval(timeRangeCaptor.capture())).thenReturn(DateInterval.days(1));
        when(pivot.timerange()).thenReturn(Optional.of(DerivedTimeRange.of(RelativeRange.create(4242))));

        this.osTimeHandler.doCreateAggregation(BucketSpecHandler.Direction.Row, "foobar", pivot, time, queryContext, query);

        final TimeRange argumentTimeRange = timeRangeCaptor.getValue();
        assertThat(argumentTimeRange).isEqualTo(RelativeRange.create(4242));
    }

    @Test
    public void timeSpecIntervalIsCalculatedOnQueryTimeRangeIfNoPivotTimeRange() throws InvalidRangeParametersException {
        final ArgumentCaptor<TimeRange> timeRangeCaptor = ArgumentCaptor.forClass(TimeRange.class);
        when(interval.toDateInterval(timeRangeCaptor.capture())).thenReturn(DateInterval.days(1));
        when(pivot.timerange()).thenReturn(Optional.empty());
        when(query.timerange()).thenReturn(RelativeRange.create(2323));


        this.osTimeHandler.doCreateAggregation(BucketSpecHandler.Direction.Row, "foobar", pivot, time, queryContext, query);
        final TimeRange argumentTimeRange = timeRangeCaptor.getValue();
        assertThat(argumentTimeRange).isEqualTo(RelativeRange.create(2323));
    }

    @Test
    public void autoDateHistogramAggregationBuilderUsedForAutoIntervalAndAllMessages() {
        final AutoInterval interval = mock(AutoInterval.class);
        final RelativeRange allMessagesRange = RelativeRange.allTime();
        doReturn(allMessagesRange).when(query).timerange();
        when(time.interval()).thenReturn(interval);

        final BucketSpecHandler.CreatedAggregations<AggregationBuilder> createdAggregations = this.osTimeHandler.doCreateAggregation(BucketSpecHandler.Direction.Row, "foobar", pivot, time, queryContext, query);
        assertEquals(createdAggregations.root(), createdAggregations.leaf());
        assertTrue(createdAggregations.root() instanceof AutoDateHistogramAggregationBuilder);
        assertEquals("foobar", createdAggregations.root().getName());
        assertEquals("foobar", ((AutoDateHistogramAggregationBuilder) createdAggregations.root()).field());
        assertEquals(DATE_TIME_FORMAT, ((AutoDateHistogramAggregationBuilder) createdAggregations.root()).format());

    }

}
