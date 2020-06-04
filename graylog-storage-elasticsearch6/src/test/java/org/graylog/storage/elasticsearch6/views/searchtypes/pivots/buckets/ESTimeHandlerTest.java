/**
 * This file is part of Graylog.
 *
 * Graylog is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.graylog.storage.elasticsearch6.views.searchtypes.pivots.buckets;

import org.elasticsearch.search.aggregations.bucket.histogram.DateHistogramInterval;
import org.graylog.storage.elasticsearch6.views.searchtypes.pivot.buckets.ESTimeHandler;
import org.graylog.plugins.views.search.timeranges.DerivedTimeRange;
import org.graylog.plugins.views.search.Query;
import org.graylog.storage.elasticsearch6.views.ESGeneratedQueryContext;
import org.graylog.storage.elasticsearch6.views.searchtypes.pivot.ESPivot;
import org.graylog.plugins.views.search.searchtypes.pivot.Pivot;
import org.graylog.plugins.views.search.searchtypes.pivot.buckets.Interval;
import org.graylog.plugins.views.search.searchtypes.pivot.buckets.Time;
import org.graylog2.plugin.indexer.searches.timeranges.InvalidRangeParametersException;
import org.graylog2.plugin.indexer.searches.timeranges.RelativeRange;
import org.graylog2.plugin.indexer.searches.timeranges.TimeRange;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Answers;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ESTimeHandlerTest {
    @Rule
    public final MockitoRule mockitoRule = MockitoJUnit.rule();

    private ESTimeHandler esTimeHandler;

    @Mock
    private Pivot pivot;

    @Mock
    private Time time;

    @Mock
    private ESPivot esPivot;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private ESGeneratedQueryContext queryContext;

    @Mock
    private Query query;

    @Mock
    private Interval interval;

    @Before
    public void setUp() throws Exception {
        this.esTimeHandler = new ESTimeHandler();
        when(time.interval()).thenReturn(interval);
        when(time.field()).thenReturn("foobar");
        final ESPivot.AggTypes aggTypes = mock(ESPivot.AggTypes.class);
        when(queryContext.contextMap().get(any())).thenReturn(aggTypes);
        when(query.effectiveTimeRange(any())).thenCallRealMethod();
    }

    @Test
    public void timeSpecIntervalIsCalculatedOnPivotTimerangeIfOverridden() throws InvalidRangeParametersException {
        final ArgumentCaptor<TimeRange> timeRangeCaptor = ArgumentCaptor.forClass(TimeRange.class);
        when(interval.toDateHistogramInterval(timeRangeCaptor.capture())).thenReturn(DateHistogramInterval.DAY);
        when(pivot.timerange()).thenReturn(Optional.of(DerivedTimeRange.of(RelativeRange.create(4242))));

        this.esTimeHandler.doCreateAggregation("foobar", pivot, time, esPivot, queryContext, query);

        final TimeRange argumentTimeRange = timeRangeCaptor.getValue();
        assertThat(argumentTimeRange).isEqualTo(RelativeRange.create(4242));
    }

    @Test
    public void timeSpecIntervalIsCalculatedOnQueryTimeRangeIfNoPivotTimeRange() throws InvalidRangeParametersException {
        final ArgumentCaptor<TimeRange> timeRangeCaptor = ArgumentCaptor.forClass(TimeRange.class);
        when(interval.toDateHistogramInterval(timeRangeCaptor.capture())).thenReturn(DateHistogramInterval.DAY);
        when(pivot.timerange()).thenReturn(Optional.empty());
        when(query.timerange()).thenReturn(RelativeRange.create(2323));

        this.esTimeHandler.doCreateAggregation("foobar", pivot, time, esPivot, queryContext, query);

        final TimeRange argumentTimeRange = timeRangeCaptor.getValue();
        assertThat(argumentTimeRange).isEqualTo(RelativeRange.create(2323));
    }
}
