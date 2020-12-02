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
package org.graylog.events.processor.aggregation;

import com.google.common.collect.ImmutableList;
import org.graylog.events.EventsConfigurationTestProvider;
import org.graylog.events.processor.EventDefinition;
import org.graylog.events.search.MoreSearch;
import org.graylog.plugins.views.search.db.SearchJobService;
import org.graylog.plugins.views.search.engine.QueryEngine;
import org.graylog.plugins.views.search.rest.PermittedStreams;
import org.graylog.plugins.views.search.searchtypes.pivot.PivotResult;
import org.graylog.plugins.views.search.searchtypes.pivot.buckets.DateRange;
import org.graylog.plugins.views.search.searchtypes.pivot.buckets.DateRangeBucket;
import org.graylog2.plugin.indexer.searches.timeranges.AbsoluteRange;
import org.graylog2.plugin.indexer.searches.timeranges.TimeRange;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Duration;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;

public class PivotAggregationSearchTest {
    @Rule
    public final MockitoRule mockitoRule = MockitoJUnit.rule();

    @Mock
    private SearchJobService searchJobService;
    @Mock
    private QueryEngine queryEngine;
    @Mock
    private EventDefinition eventDefinition;
    @Mock
    private MoreSearch moreSearch;
    @Mock
    private PermittedStreams permittedStreams;

    @Test
    public void testExtractValuesWithGroupBy() throws Exception {
        final AbsoluteRange timerange = AbsoluteRange.create(DateTime.now(DateTimeZone.UTC).minusSeconds(3600), DateTime.now(DateTimeZone.UTC));
        final AggregationSeries seriesCount = AggregationSeries.create("abc123", AggregationFunction.COUNT, "source");
        final AggregationSeries seriesCard = AggregationSeries.create("abc123", AggregationFunction.CARD, "source");
        final AggregationEventProcessorConfig config = AggregationEventProcessorConfig.builder()
                .query("")
                .streams(Collections.emptySet())
                .groupBy(Collections.emptyList())
                .series(ImmutableList.of(seriesCount, seriesCard))
                .conditions(null)
                .searchWithinMs(30000)
                .executeEveryMs(30000)
                .build();
        final AggregationEventProcessorParameters parameters = AggregationEventProcessorParameters.builder()
                .streams(Collections.emptySet())
                .timerange(timerange)
                .batchSize(500)
                .build();

        final PivotAggregationSearch pivotAggregationSearch = new PivotAggregationSearch(
                config,
                parameters,
                "test",
                eventDefinition,
                searchJobService,
                queryEngine,
                EventsConfigurationTestProvider.create(),
                moreSearch,
                permittedStreams);

        final String toString = timerange.getTo().toString();
        final PivotResult pivotResult = PivotResult.builder()
                .id("test")
                .effectiveTimerange(timerange)
                .total(1)
                .addRow(PivotResult.Row.builder()
                        .key(ImmutableList.of(toString, "a", "b"))
                        .addValue(PivotResult.Value.create(ImmutableList.of("metric/count/source/abc123"), 42, true, "row-leaf"))
                        .addValue(PivotResult.Value.create(ImmutableList.of("metric/card/source/abc123"), 1, true, "row-leaf"))
                        .source("leaf")
                        .build())
                .addRow(PivotResult.Row.builder()
                        .key(ImmutableList.of(toString, "a"))
                        .addValue(PivotResult.Value.create(ImmutableList.of("metric/count/source/abc123"), 84, true, "row-inner"))
                        .addValue(PivotResult.Value.create(ImmutableList.of("metric/card/source/abc123"), 1, true, "row-inner"))
                        .source("non-leaf")
                        .build())
                .addRow(PivotResult.Row.builder()
                        .key(ImmutableList.of(toString, "a", "c"))
                        .addValue(PivotResult.Value.create(ImmutableList.of("metric/count/source/abc123"), 42, true, "row-leaf"))
                        .addValue(PivotResult.Value.create(ImmutableList.of("metric/card/source/abc123"), 1, true, "row-leaf"))
                        .source("leaf")
                        .build())
                .build();

        final ImmutableList<AggregationKeyResult> results = pivotAggregationSearch.extractValues(pivotResult);

        assertThat(results.size()).isEqualTo(2);

        assertThat(results.get(0)).isEqualTo(AggregationKeyResult.builder()
                .timestamp(timerange.getTo())
                .key(ImmutableList.of("a", "b"))
                .seriesValues(ImmutableList.of(
                        AggregationSeriesValue.builder()
                                .key(ImmutableList.of("a", "b"))
                                .value(42.0)
                                .series(seriesCount)
                                .build(),
                        AggregationSeriesValue.builder()
                                .key(ImmutableList.of("a", "b"))
                                .value(1.0)
                                .series(seriesCard)
                                .build()
                ))
                .build());

        assertThat(results.get(1)).isEqualTo(AggregationKeyResult.builder()
                .timestamp(timerange.getTo())
                .key(ImmutableList.of("a", "c"))
                .seriesValues(ImmutableList.of(
                        AggregationSeriesValue.builder()
                                .key(ImmutableList.of("a", "c"))
                                .value(42.0)
                                .series(seriesCount)
                                .build(),
                        AggregationSeriesValue.builder()
                                .key(ImmutableList.of("a", "c"))
                                .value(1.0)
                                .series(seriesCard)
                                .build()
                ))
                .build());
    }

    @Test
    public void testExtractValuesWithoutGroupBy() throws Exception {
        final AbsoluteRange timerange = AbsoluteRange.create(DateTime.now(DateTimeZone.UTC).minusSeconds(3600), DateTime.now(DateTimeZone.UTC));
        final AggregationSeries seriesCount = AggregationSeries.create("abc123", AggregationFunction.COUNT, "source");
        final AggregationSeries seriesCountNoField = AggregationSeries.create("abc123", AggregationFunction.COUNT, "");
        final AggregationSeries seriesCard = AggregationSeries.create("abc123", AggregationFunction.CARD, "source");
        final AggregationEventProcessorConfig config = AggregationEventProcessorConfig.builder()
                .query("")
                .streams(Collections.emptySet())
                .groupBy(Collections.emptyList())
                .series(ImmutableList.of(seriesCount, seriesCountNoField, seriesCard))
                .conditions(null)
                .searchWithinMs(30000)
                .executeEveryMs(30000)
                .build();
        final AggregationEventProcessorParameters parameters = AggregationEventProcessorParameters.builder()
                .streams(Collections.emptySet())
                .timerange(timerange)
                .batchSize(500)
                .build();

        final PivotAggregationSearch pivotAggregationSearch = new PivotAggregationSearch(
                config,
                parameters,
                "test",
                eventDefinition,
                searchJobService,
                queryEngine,
                EventsConfigurationTestProvider.create(),
                moreSearch,
                permittedStreams);

        final PivotResult pivotResult = PivotResult.builder()
                .id("test")
                .effectiveTimerange(timerange)
                .total(1)
                .addRow(PivotResult.Row.builder()
                        .key(ImmutableList.of(timerange.getTo().toString()))
                        .addValue(PivotResult.Value.create(ImmutableList.of("metric/count/source/abc123"), 42, true, "row-leaf"))
                        .addValue(PivotResult.Value.create(ImmutableList.of("metric/count/<no-field>/abc123"), 23, true, "row-leaf"))
                        .addValue(PivotResult.Value.create(ImmutableList.of("metric/card/source/abc123"), 1, true, "row-leaf"))
                        .source("leaf")
                        .build())
                .build();

        final ImmutableList<AggregationKeyResult> results = pivotAggregationSearch.extractValues(pivotResult);

        assertThat(results.size()).isEqualTo(1);

        assertThat(results.get(0)).isEqualTo(AggregationKeyResult.builder()
                .key(ImmutableList.of())
                .timestamp(timerange.getTo())
                .seriesValues(ImmutableList.of(
                        AggregationSeriesValue.builder()
                                .key(ImmutableList.of())
                                .value(42.0)
                                .series(seriesCount)
                                .build(),
                        AggregationSeriesValue.builder()
                                .key(ImmutableList.of())
                                .value(23.0)
                                .series(seriesCountNoField)
                                .build(),
                        AggregationSeriesValue.builder()
                                .key(ImmutableList.of())
                                .value(1.0)
                                .series(seriesCard)
                                .build()
                ))
                .build());
    }

    @Test
    public void testExtractValuesWithNullValues() throws Exception {
        final AbsoluteRange timerange = AbsoluteRange.create(DateTime.now(DateTimeZone.UTC).minusSeconds(3600), DateTime.now(DateTimeZone.UTC));
        final AggregationSeries seriesCount = AggregationSeries.create("abc123", AggregationFunction.COUNT, "source");
        final AggregationSeries seriesAvg = AggregationSeries.create("abc123", AggregationFunction.AVG, "some_field");
        final AggregationEventProcessorConfig config = AggregationEventProcessorConfig.builder()
                .query("")
                .streams(Collections.emptySet())
                .groupBy(Collections.emptyList())
                .series(ImmutableList.of(seriesCount, seriesAvg))
                .conditions(null)
                .searchWithinMs(30000)
                .executeEveryMs(30000)
                .build();
        final AggregationEventProcessorParameters parameters = AggregationEventProcessorParameters.builder()
                .streams(Collections.emptySet())
                .timerange(timerange)
                .batchSize(500)
                .build();

        final PivotAggregationSearch pivotAggregationSearch = new PivotAggregationSearch(
                config,
                parameters,
                "test",
                eventDefinition,
                searchJobService,
                queryEngine,
                EventsConfigurationTestProvider.create(),
                moreSearch,
                permittedStreams);

        final PivotResult pivotResult = PivotResult.builder()
                .id("test")
                .effectiveTimerange(timerange)
                .total(1)
                .addRow(PivotResult.Row.builder()
                        .key(ImmutableList.of(timerange.getTo().toString()))
                        .addValue(PivotResult.Value.create(ImmutableList.of("metric/count/source/abc123"), 42, true, "row-leaf"))
                        // A "null" value can happen with some Elasticsearch aggregations (e.g. avg on a non-existent field)
                        .addValue(PivotResult.Value.create(ImmutableList.of("metric/avg/some_field/abc123"), null, true, "row-leaf"))
                        .source("leaf")
                        .build())
                .build();

        final ImmutableList<AggregationKeyResult> results = pivotAggregationSearch.extractValues(pivotResult);

        assertThat(results.size()).isEqualTo(1);

        assertThat(results.get(0)).isEqualTo(AggregationKeyResult.builder()
                .key(ImmutableList.of())
                .timestamp(timerange.getTo())
                .seriesValues(ImmutableList.of(
                        AggregationSeriesValue.builder()
                                .key(ImmutableList.of())
                                .value(42.0)
                                .series(seriesCount)
                                .build(),
                        AggregationSeriesValue.builder()
                                .key(ImmutableList.of())
                                .value(Double.NaN) // For "null" we expect NaN
                                .series(seriesAvg)
                                .build()
                ))
                .build());
    }

    @Test
    public void testDateRangeBucketWithOneTumblingWindow() {
        final long processingWindowSize = Duration.standardSeconds(60).getMillis();
        final long processingHopSize = Duration.standardSeconds(60).getMillis();
        final DateTime now = DateTime.now(DateTimeZone.UTC);
        final DateTime from = now;
        final DateTime to = now.plusMillis((int) processingWindowSize);
        TimeRange timeRange = AbsoluteRange.create(from, to);
        final DateRangeBucket rangeBucket = PivotAggregationSearch.buildDateRangeBuckets(timeRange, processingWindowSize, processingHopSize);

        assertThat(rangeBucket.ranges()).containsExactly(DateRange.create(from, to));
    }

    @Test
    public void testDateRangeBucketWithCatchUpTumblingWindows() {
        final long processingWindowSize = Duration.standardSeconds(60).getMillis();
        final long processingHopSize = Duration.standardSeconds(60).getMillis();
        final DateTime now = DateTime.now(DateTimeZone.UTC);
        final DateTime from = now;
        // We are 3 full processingWindows behind
        final DateTime to = now.plusMillis((int) processingWindowSize * 3);
        TimeRange timeRange = AbsoluteRange.create(from, to);
        final DateRangeBucket rangeBucket = PivotAggregationSearch.buildDateRangeBuckets(timeRange, processingWindowSize, processingHopSize);

        assertThat(rangeBucket.ranges()).containsExactly(
                DateRange.create(from.plusMillis((int) (processingWindowSize * 0)), from.plusMillis((int) (processingWindowSize * 1))),
                DateRange.create(from.plusMillis((int) (processingWindowSize * 1)), from.plusMillis((int) (processingWindowSize * 2))),
                DateRange.create(from.plusMillis((int) (processingWindowSize * 2)), from.plusMillis((int) (processingWindowSize * 3)))
        );
    }

    @Test
    public void testDateRangeBucketWithSlidingWindow() {
        final long processingWindowSize = Duration.standardSeconds(3600).getMillis();
        final long processingHopSize = Duration.standardSeconds(60).getMillis();
        final DateTime now = DateTime.now(DateTimeZone.UTC);
        final DateTime from = now;
        final DateTime to = now.plusMillis((int) processingWindowSize);
        TimeRange timeRange = AbsoluteRange.create(from, to);
        final DateRangeBucket rangeBucket = PivotAggregationSearch.buildDateRangeBuckets(timeRange, processingWindowSize, processingHopSize);

        assertThat(rangeBucket.ranges()).containsExactly(
                DateRange.create(from, to)
        );
    }

    @Test
    public void testDateRangeBucketWithCatchUpSlidingWindows() {
        final int processingWindowSizeSec = 120;
        final int processingHopSizeSec = 60;
        final DateTime now = DateTime.now(DateTimeZone.UTC);
        final DateTime from = now;
        // We are 3 full processingWindows behind
        final DateTime to = now.plusSeconds(processingWindowSizeSec * 3);
        TimeRange timeRange = AbsoluteRange.create(from, to);
        final DateRangeBucket rangeBucket = PivotAggregationSearch.buildDateRangeBuckets(timeRange, processingWindowSizeSec * 1000, processingHopSizeSec * 1000);

        assertThat(rangeBucket.ranges()).containsExactly(
                DateRange.create(from.plusSeconds(processingHopSizeSec * 0), from.plusSeconds(processingWindowSizeSec)),
                DateRange.create(from.plusSeconds(processingHopSizeSec * 1), from.plusSeconds(processingHopSizeSec * 1).plusSeconds(processingWindowSizeSec)),
                DateRange.create(from.plusSeconds(processingHopSizeSec * 2), from.plusSeconds(processingHopSizeSec * 2).plusSeconds(processingWindowSizeSec)),
                DateRange.create(from.plusSeconds(processingHopSizeSec * 3), from.plusSeconds(processingHopSizeSec * 3).plusSeconds(processingWindowSizeSec)),
                DateRange.create(from.plusSeconds(processingHopSizeSec * 4), to)
        );
    }
}
