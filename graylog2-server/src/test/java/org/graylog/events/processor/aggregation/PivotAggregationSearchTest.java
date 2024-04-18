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
import com.google.common.collect.ImmutableSet;
import org.assertj.core.api.Assertions;
import org.graylog.events.EventsConfigurationTestProvider;
import org.graylog.events.processor.EventDefinition;
import org.graylog.events.search.MoreSearch;
import org.graylog.plugins.views.search.Query;
import org.graylog.plugins.views.search.ValueParameter;
import org.graylog.plugins.views.search.db.SearchJobService;
import org.graylog.plugins.views.search.elasticsearch.QueryStringDecorators;
import org.graylog.plugins.views.search.engine.PositionTrackingQuery;
import org.graylog.plugins.views.search.engine.QueryEngine;
import org.graylog.plugins.views.search.rest.PermittedStreams;
import org.graylog.plugins.views.search.searchtypes.pivot.Pivot;
import org.graylog.plugins.views.search.searchtypes.pivot.PivotResult;
import org.graylog.plugins.views.search.searchtypes.pivot.SeriesSpec;
import org.graylog.plugins.views.search.searchtypes.pivot.buckets.DateRange;
import org.graylog.plugins.views.search.searchtypes.pivot.buckets.DateRangeBucket;
import org.graylog.plugins.views.search.searchtypes.pivot.buckets.Values;
import org.graylog.plugins.views.search.searchtypes.pivot.series.Average;
import org.graylog.plugins.views.search.searchtypes.pivot.series.Cardinality;
import org.graylog.plugins.views.search.searchtypes.pivot.series.Count;
import org.graylog2.notifications.NotificationService;
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
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

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
    private NotificationService notificationService;

    private final PermittedStreams permittedStreams = new PermittedStreams(Stream::of);

    @Test
    public void testExtractValuesWithGroupBy() throws Exception {
        final long WINDOW_LENGTH = 30000;
        final AbsoluteRange timerange = AbsoluteRange.create(DateTime.now(DateTimeZone.UTC).minusSeconds(3600), DateTime.now(DateTimeZone.UTC));
        final SeriesSpec seriesCount = Count.builder().id("abc123").field("source").build();
        final SeriesSpec seriesCard = Cardinality.builder().id("abc123").field("source").build();
        final AggregationEventProcessorConfig config = AggregationEventProcessorConfig.builder()
                .query("")
                .streams(Collections.emptySet())
                .groupBy(Collections.emptyList())
                .series(ImmutableList.of(seriesCount, seriesCard))
                .conditions(null)
                .searchWithinMs(WINDOW_LENGTH)
                .executeEveryMs(WINDOW_LENGTH)
                .build();
        final AggregationEventProcessorParameters parameters = AggregationEventProcessorParameters.builder()
                .streams(Collections.emptySet())
                .timerange(timerange)
                .batchSize(500)
                .build();

        final PivotAggregationSearch pivotAggregationSearch = new PivotAggregationSearch(
                config,
                parameters,
                new AggregationSearch.User("test", DateTimeZone.UTC),
                eventDefinition,
                Collections.emptyList(),
                searchJobService,
                queryEngine,
                EventsConfigurationTestProvider.create(),
                moreSearch,
                permittedStreams,
                notificationService,
                new QueryStringDecorators(Optional.empty())
        );

        final String toString = timerange.getTo().toString();
        final PivotResult pivotResult = PivotResult.builder()
                .id("test")
                .effectiveTimerange(timerange)
                .total(1)
                .addRow(PivotResult.Row.builder()
                        .key(ImmutableList.of(toString, "a", "b"))
                        .addValue(PivotResult.Value.create(ImmutableList.of("metric/count(source)"), 42, true, "row-leaf"))
                        .addValue(PivotResult.Value.create(ImmutableList.of("metric/card(source)"), 1, true, "row-leaf"))
                        .source("leaf")
                        .build())
                .addRow(PivotResult.Row.builder()
                        .key(ImmutableList.of(toString, "a"))
                        .addValue(PivotResult.Value.create(ImmutableList.of("metric/count(source)"), 84, true, "row-inner"))
                        .addValue(PivotResult.Value.create(ImmutableList.of("metric/card(source)"), 1, true, "row-inner"))
                        .source("non-leaf")
                        .build())
                .addRow(PivotResult.Row.builder()
                        .key(ImmutableList.of(toString, "a", "c"))
                        .addValue(PivotResult.Value.create(ImmutableList.of("metric/count(source)"), 42, true, "row-leaf"))
                        .addValue(PivotResult.Value.create(ImmutableList.of("metric/card(source)"), 1, true, "row-leaf"))
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
        final long WINDOW_LENGTH = 30000;
        final AbsoluteRange timerange = AbsoluteRange.create(DateTime.now(DateTimeZone.UTC).minusSeconds(3600), DateTime.now(DateTimeZone.UTC));
        final SeriesSpec seriesCount = Count.builder().id("abc123").field("source").build();
        final SeriesSpec seriesCountNoField = Count.builder().id("abc123").build();
        final SeriesSpec seriesCard = Cardinality.builder().id("abc123").field("source").build();
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
                new AggregationSearch.User("test", DateTimeZone.UTC),
                eventDefinition,
                Collections.emptyList(),
                searchJobService,
                queryEngine,
                EventsConfigurationTestProvider.create(),
                moreSearch,
                permittedStreams,
                notificationService,
                new QueryStringDecorators(Optional.empty())
        );

        final PivotResult pivotResult = PivotResult.builder()
                .id("test")
                .effectiveTimerange(timerange)
                .total(1)
                .addRow(PivotResult.Row.builder()
                        .key(ImmutableList.of(timerange.getTo().toString()))
                        .addValue(PivotResult.Value.create(ImmutableList.of("metric/count(source)"), 42, true, "row-leaf"))
                        .addValue(PivotResult.Value.create(ImmutableList.of("metric/count()"), 23, true, "row-leaf"))
                        .addValue(PivotResult.Value.create(ImmutableList.of("metric/card(source)"), 1, true, "row-leaf"))
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
    public void testExtractAdditionalSearchTypes() throws Exception {
        final AbsoluteRange timerange = AbsoluteRange.create(DateTime.now(DateTimeZone.UTC).minusSeconds(3600), DateTime.now(DateTimeZone.UTC));
        final SeriesSpec seriesCount = Count.builder().id("abc123").field("source").build();
        final AggregationEventProcessorConfig config = AggregationEventProcessorConfig.builder()
                .query("")
                .streams(Collections.emptySet())
                .groupBy(Collections.emptyList())
                .series(ImmutableList.of(seriesCount))
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
                new AggregationSearch.User("test", DateTimeZone.UTC),
                eventDefinition,
                List.of(Pivot.builder()
                        .id("risk-asset-1")
                        .rowGroups(Values.builder().limit(10).field("Field").build())
                        .rollup(false)
                        .series(Count.builder().build())
                        .build()),
                searchJobService,
                queryEngine,
                EventsConfigurationTestProvider.create(),
                moreSearch,
                permittedStreams,
                notificationService,
                new QueryStringDecorators(Optional.empty())
        );

        final PivotResult pivotResult = PivotResult.builder()
                .id("test")
                .effectiveTimerange(timerange)
                .total(1)
                .addRow(PivotResult.Row.builder()
                        .key(ImmutableList.of(timerange.getTo().toString()))
                        .addValue(PivotResult.Value.create(ImmutableList.of("metric/count(source)"), 42, true, "row-leaf"))
                        .addValue(PivotResult.Value.create(ImmutableList.of("metric/count()"), 23, true, "row-leaf"))
                        .addValue(PivotResult.Value.create(ImmutableList.of("metric/card(source)"), 1, true, "row-leaf"))
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
                                .build()
                ))
                .build());
    }

    @Test
    public void testExtractValuesWithNullValues() throws Exception {
        final long WINDOW_LENGTH = 30000;
        final AbsoluteRange timerange = AbsoluteRange.create(DateTime.now(DateTimeZone.UTC).minusSeconds(3600), DateTime.now(DateTimeZone.UTC));
        final SeriesSpec seriesCount = Count.builder().id("abc123").field("source").build();
        final SeriesSpec seriesAvg = Average.builder().id("abc123").field("some_field").build();
        final AggregationEventProcessorConfig config = AggregationEventProcessorConfig.builder()
                .query("")
                .streams(Collections.emptySet())
                .groupBy(Collections.emptyList())
                .series(ImmutableList.of(seriesCount, seriesAvg))
                .conditions(null)
                .searchWithinMs(WINDOW_LENGTH)
                .executeEveryMs(WINDOW_LENGTH)
                .build();
        final AggregationEventProcessorParameters parameters = AggregationEventProcessorParameters.builder()
                .streams(Collections.emptySet())
                .timerange(timerange)
                .batchSize(500)
                .build();

        final PivotAggregationSearch pivotAggregationSearch = new PivotAggregationSearch(
                config,
                parameters,
                new AggregationSearch.User("test", DateTimeZone.UTC),
                eventDefinition,
                Collections.emptyList(),
                searchJobService,
                queryEngine,
                EventsConfigurationTestProvider.create(),
                moreSearch,
                permittedStreams,
                notificationService,
                new QueryStringDecorators(Optional.empty())
        );

        final PivotResult pivotResult = PivotResult.builder()
                .id("test")
                .effectiveTimerange(timerange)
                .total(1)
                .addRow(PivotResult.Row.builder()
                        .key(ImmutableList.of(timerange.getTo().toString()))
                        .addValue(PivotResult.Value.create(ImmutableList.of("metric/count(source)"), 42, true, "row-leaf"))
                        // A "null" value can happen with some Elasticsearch aggregations (e.g. avg on a non-existent field)
                        .addValue(PivotResult.Value.create(ImmutableList.of("metric/avg(some_field)"), null, true, "row-leaf"))
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

    @Test
    public void testQueryParameterSubstitution() {
        final long WINDOW_LENGTH = 30000;
        final AbsoluteRange timerange = AbsoluteRange.create(DateTime.now(DateTimeZone.UTC).minusSeconds(3600), DateTime.now(DateTimeZone.UTC));
        var seriesCount = Count.builder().field("source").build();
        var seriesCard = Cardinality.builder().field("source").build();
        final AggregationEventProcessorConfig config = AggregationEventProcessorConfig.builder()
                .query("source:$secret$")
                .queryParameters(ImmutableSet.of(ValueParameter.builder().dataType("any").name("secret").build()))
                .streams(Collections.emptySet())
                .groupBy(Collections.emptyList())
                .series(ImmutableList.of(seriesCount, seriesCard))
                .conditions(null)
                .searchWithinMs(WINDOW_LENGTH)
                .executeEveryMs(WINDOW_LENGTH)
                .build();
        final AggregationEventProcessorParameters parameters = AggregationEventProcessorParameters.builder()
                .streams(Collections.emptySet())
                .timerange(timerange)
                .batchSize(500)
                .build();

        final PivotAggregationSearch pivotAggregationSearch = new PivotAggregationSearch(
                config,
                parameters,
                new AggregationSearch.User("test", DateTimeZone.UTC),
                eventDefinition,
                Collections.emptyList(),
                searchJobService,
                queryEngine,
                EventsConfigurationTestProvider.create(),
                moreSearch,
                new PermittedStreams(() -> Stream.of("00001")),
                notificationService,
                new QueryStringDecorators(Optional.of((queryString, parameterProvider, query) -> {
                    if (queryString.equals("source:$secret$") && parameterProvider.getParameter("secret").isPresent()) {
                        return PositionTrackingQuery.of("source:example.org");
                    } else {
                        throw new IllegalArgumentException("Unexpected query decoration request!");
                    }
                }))
        );
        final Query query = pivotAggregationSearch.getAggregationQuery(parameters, WINDOW_LENGTH, WINDOW_LENGTH);
        Assertions.assertThat(query.query().queryString()).isEqualTo("source:example.org");
    }

    @Test
    public void testAdditionalSearchTypes() {
        final long WINDOW_LENGTH = 30000;
        final AbsoluteRange timerange = AbsoluteRange.create(DateTime.now(DateTimeZone.UTC).minusSeconds(3600), DateTime.now(DateTimeZone.UTC));
        var seriesCount = Count.builder().field("source").build();
        var seriesCard = Cardinality.builder().field("source").build();
        final AggregationEventProcessorConfig config = AggregationEventProcessorConfig.builder()
                .query("source:foo")
                .queryParameters(ImmutableSet.of())
                .streams(Collections.emptySet())
                .groupBy(Collections.emptyList())
                .series(ImmutableList.of(seriesCount, seriesCard))
                .conditions(null)
                .searchWithinMs(WINDOW_LENGTH)
                .executeEveryMs(WINDOW_LENGTH)
                .build();
        final AggregationEventProcessorParameters parameters = AggregationEventProcessorParameters.builder()
                .streams(Collections.emptySet())
                .timerange(timerange)
                .batchSize(500)
                .build();

        final PivotAggregationSearch pivotAggregationSearch = new PivotAggregationSearch(
                config,
                parameters,
                new AggregationSearch.User("test", DateTimeZone.UTC),
                eventDefinition,
                List.of(Pivot.builder()
                        .id("risk-asset-1")
                        .rowGroups(Values.builder().limit(10).field("Field").build())
                        .rollup(false)
                        .series(Count.builder().build())
                        .build()),
                searchJobService,
                queryEngine,
                EventsConfigurationTestProvider.create(),
                moreSearch,
                new PermittedStreams(() -> Stream.of("00001")),
                notificationService,
                new QueryStringDecorators(Optional.empty())
        );
        final Query query = pivotAggregationSearch.getAggregationQuery(parameters, WINDOW_LENGTH, WINDOW_LENGTH);
        Assertions.assertThatCollection(query.searchTypes()).contains(
                Pivot.builder()
                        .id("risk-asset-1")
                        .rowGroups(Values.builder().limit(10).field("Field").build())
                        .rollup(false)
                        .series(Count.builder().build())
                        .build());

    }
}
