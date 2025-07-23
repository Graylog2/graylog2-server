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
import org.graylog.events.processor.EventProcessorException;
import org.graylog.plugins.views.search.ParameterProvider;
import org.graylog.plugins.views.search.Query;
import org.graylog.plugins.views.search.QueryResult;
import org.graylog.plugins.views.search.Search;
import org.graylog.plugins.views.search.SearchJob;
import org.graylog.plugins.views.search.SearchType;
import org.graylog.plugins.views.search.ValueParameter;
import org.graylog.plugins.views.search.db.SearchJobService;
import org.graylog.plugins.views.search.elasticsearch.QueryStringDecorators;
import org.graylog.plugins.views.search.engine.PositionTrackingQuery;
import org.graylog.plugins.views.search.engine.QueryEngine;
import org.graylog.plugins.views.search.engine.normalization.SearchNormalization;
import org.graylog.plugins.views.search.rest.PermittedStreams;
import org.graylog.plugins.views.search.searchfilters.model.InlineQueryStringSearchFilter;
import org.graylog.plugins.views.search.searchfilters.model.UsedSearchFilter;
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
import org.graylog2.streams.StreamService;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Duration;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

public class PivotAggregationSearchTest {
    private static final List<UsedSearchFilter> SEARCH_FILTERS = List.of(InlineQueryStringSearchFilter.builder().title("title").description("desc").queryString("host:localhost").build());
    private static final String QUERY = "source:foo";
    private static final String TEST_USER = "test";
    private static final long WINDOW_LENGTH = 30000;

    @Rule
    public final MockitoRule mockitoRule = MockitoJUnit.rule();

    @Mock
    private SearchJobService searchJobService;
    @Mock
    private QueryEngine queryEngine;
    @Mock
    private EventDefinition eventDefinition;
    @Mock
    private NotificationService notificationService;
    @Mock
    private StreamService streamService;
    @Mock
    private SearchNormalization searchNormalization;

    private final PermittedStreams permittedStreams = new PermittedStreams(Stream::of, (categories) -> Stream.of());

    @Test
    public void testExtractValuesWithGroupBy() throws Exception {
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

        final PivotAggregationSearch pivotAggregationSearch = createPivotAggregationSearch(config, buildParameters(timerange));

        final String toString = timerange.getTo().toString();
        final PivotResult pivotResult = PivotResult.builder()
                .id(TEST_USER)
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
                .searchWithinMs(WINDOW_LENGTH)
                .executeEveryMs(WINDOW_LENGTH)
                .build();
        final PivotAggregationSearch pivotAggregationSearch = createPivotAggregationSearch(config, buildParameters(timerange));

        final PivotResult pivotResult = PivotResult.builder()
                .id(TEST_USER)
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
                .searchWithinMs(WINDOW_LENGTH)
                .executeEveryMs(WINDOW_LENGTH)
                .build();

        final PivotAggregationSearch pivotAggregationSearch = createPivotAggregationSearch(
                config,
                buildParameters(timerange),
                List.of(Pivot.builder()
                        .id("risk-asset-1")
                        .rowGroups(Values.builder().limit(10).field("Field").build())
                        .rollup(false)
                        .series(Count.builder().build())
                        .build())
        );

        final PivotResult pivotResult = PivotResult.builder()
                .id(TEST_USER)
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
        final PivotAggregationSearch pivotAggregationSearch = createPivotAggregationSearch(config, buildParameters(timerange));

        final PivotResult pivotResult = PivotResult.builder()
                .id(TEST_USER)
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
        final DateTime to = now.plusMillis((int) processingWindowSize);
        TimeRange timeRange = AbsoluteRange.create(now, to);
        final DateRangeBucket rangeBucket = PivotAggregationSearch.buildDateRangeBuckets(timeRange, processingWindowSize, processingHopSize);

        assertThat(rangeBucket.ranges()).containsExactly(DateRange.create(now, to));
    }

    @Test
    public void testDateRangeBucketWithCatchUpTumblingWindows() {
        final long processingWindowSize = Duration.standardSeconds(60).getMillis();
        final long processingHopSize = Duration.standardSeconds(60).getMillis();
        final DateTime now = DateTime.now(DateTimeZone.UTC);
        // We are 3 full processingWindows behind
        final DateTime to = now.plusMillis((int) processingWindowSize * 3);
        TimeRange timeRange = AbsoluteRange.create(now, to);
        final DateRangeBucket rangeBucket = PivotAggregationSearch.buildDateRangeBuckets(timeRange, processingWindowSize, processingHopSize);

        assertThat(rangeBucket.ranges()).containsExactly(
                DateRange.create(now.plusMillis((int) (processingWindowSize * 0)), now.plusMillis((int) (processingWindowSize * 1))),
                DateRange.create(now.plusMillis((int) (processingWindowSize * 1)), now.plusMillis((int) (processingWindowSize * 2))),
                DateRange.create(now.plusMillis((int) (processingWindowSize * 2)), now.plusMillis((int) (processingWindowSize * 3)))
        );
    }

    @Test
    public void testDateRangeBucketWithSlidingWindow() {
        final long processingWindowSize = Duration.standardSeconds(3600).getMillis();
        final long processingHopSize = Duration.standardSeconds(60).getMillis();
        final DateTime now = DateTime.now(DateTimeZone.UTC);
        final DateTime to = now.plusMillis((int) processingWindowSize);
        TimeRange timeRange = AbsoluteRange.create(now, to);
        final DateRangeBucket rangeBucket = PivotAggregationSearch.buildDateRangeBuckets(timeRange, processingWindowSize, processingHopSize);

        assertThat(rangeBucket.ranges()).containsExactly(
                DateRange.create(now, to)
        );
    }

    @Test
    public void testDateRangeBucketWithCatchUpSlidingWindows() {
        final int processingWindowSizeSec = 120;
        final int processingHopSizeSec = 60;
        final DateTime now = DateTime.now(DateTimeZone.UTC);
        // We are 3 full processingWindows behind
        final DateTime to = now.plusSeconds(processingWindowSizeSec * 3);
        TimeRange timeRange = AbsoluteRange.create(now, to);
        final DateRangeBucket rangeBucket = PivotAggregationSearch.buildDateRangeBuckets(timeRange, processingWindowSizeSec * 1000, processingHopSizeSec * 1000);

        assertThat(rangeBucket.ranges()).containsExactly(
                DateRange.create(now.plusSeconds(processingHopSizeSec * 0), now.plusSeconds(processingWindowSizeSec)),
                DateRange.create(now.plusSeconds(processingHopSizeSec * 1), now.plusSeconds(processingHopSizeSec * 1).plusSeconds(processingWindowSizeSec)),
                DateRange.create(now.plusSeconds(processingHopSizeSec * 2), now.plusSeconds(processingHopSizeSec * 2).plusSeconds(processingWindowSizeSec)),
                DateRange.create(now.plusSeconds(processingHopSizeSec * 3), now.plusSeconds(processingHopSizeSec * 3).plusSeconds(processingWindowSizeSec)),
                DateRange.create(now.plusSeconds(processingHopSizeSec * 4), to)
        );
    }

    @Test
    public void testQueryParameterSubstitution() {
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
                .filters(SEARCH_FILTERS)
                .searchWithinMs(WINDOW_LENGTH)
                .executeEveryMs(WINDOW_LENGTH)
                .build();
        final AggregationEventProcessorParameters parameters = buildParameters(timerange);

        final PivotAggregationSearch pivotAggregationSearch = createPivotAggregationSearch(
                config,
                parameters,
                Collections.emptyList(),
                new PermittedStreams(() -> Stream.of("00001"), (categories) -> Stream.of()),
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
        Assertions.assertThat(query.filters().size()).isEqualTo(1);
    }

    @Test
    public void testAdditionalSearchTypes() {
        final AbsoluteRange timerange = AbsoluteRange.create(DateTime.now(DateTimeZone.UTC).minusSeconds(3600), DateTime.now(DateTimeZone.UTC));
        var seriesCount = Count.builder().field("source").build();
        var seriesCard = Cardinality.builder().field("source").build();
        final AggregationEventProcessorConfig config = AggregationEventProcessorConfig.builder()
                .query(QUERY)
                .queryParameters(ImmutableSet.of())
                .streams(Collections.emptySet())
                .groupBy(Collections.emptyList())
                .series(ImmutableList.of(seriesCount, seriesCard))
                .conditions(null)
                .filters(SEARCH_FILTERS)
                .searchWithinMs(WINDOW_LENGTH)
                .executeEveryMs(WINDOW_LENGTH)
                .build();
        final AggregationEventProcessorParameters parameters = buildParameters(timerange);

        final PivotAggregationSearch pivotAggregationSearch = createPivotAggregationSearch(
                config,
                parameters,
                List.of(Pivot.builder()
                        .id("risk-asset-1")
                        .rowGroups(Values.builder().limit(10).field("Field").build())
                        .rollup(false)
                        .series(Count.builder().build())
                        .build()),
                new PermittedStreams(() -> Stream.of("00001"), (categories) -> Stream.of())
        );
        final Query query = pivotAggregationSearch.getAggregationQuery(parameters, WINDOW_LENGTH, WINDOW_LENGTH);
        Assertions.assertThatCollection(query.searchTypes()).contains(
                Pivot.builder()
                        .id("risk-asset-1")
                        .rowGroups(Values.builder().limit(10).field("Field").build())
                        .rollup(false)
                        .series(Count.builder().build())
                        .build());
        Assertions.assertThat(query.filters().size()).isEqualTo(1);
    }


    @Test
    public void testPrepareSearch() throws EventProcessorException {
        final AbsoluteRange timerange = AbsoluteRange.create(DateTime.now(DateTimeZone.UTC).minusSeconds(3600), DateTime.now(DateTimeZone.UTC));
        var seriesCount = Count.builder().field("source").build();
        var seriesCard = Cardinality.builder().field("source").build();
        final AggregationEventProcessorConfig config = AggregationEventProcessorConfig.builder()
                .query(QUERY)
                .queryParameters(ImmutableSet.of())
                .streams(Collections.emptySet())
                .groupBy(Collections.emptyList())
                .series(ImmutableList.of(seriesCount, seriesCard))
                .conditions(null)
                .filters(SEARCH_FILTERS)
                .searchWithinMs(WINDOW_LENGTH)
                .executeEveryMs(WINDOW_LENGTH)
                .build();
        final AggregationEventProcessorParameters parameters = buildParameters(timerange);

        final PivotAggregationSearch pivotAggregationSearch = createPivotAggregationSearch(
                config,
                parameters,
                List.of(Pivot.builder()
                        .id("risk-asset-1")
                        .rowGroups(Values.builder().limit(10).field("Field").build())
                        .rollup(false)
                        .series(Count.builder().build())
                        .build()),
                new PermittedStreams(() -> Stream.of("00001"), (categories) -> Stream.of())
        );
        when(searchNormalization.postValidation(isA(Query.class), isA(ParameterProvider.class))).thenAnswer(invocation -> {
            return invocation.getArgument(0); // Return same query received.
        });
        final SearchJob job = new SearchJob("job", mock(Search.class), TEST_USER, "test-node-id");
        final QueryResult queryResult = QueryResult.builder()
                .searchTypes(Collections.singletonMap("searchType", mock(SearchType.Result.class)))
                .query(mock(Query.class))
                .build();
        job.addQueryResultFuture(TEST_USER, CompletableFuture.completedFuture(queryResult));
        job.seal();
        when(searchJobService.create(any(), eq(TEST_USER), eq(0))).thenReturn(job);
        when(queryEngine.execute(any(), anySet(), any())).thenReturn(job).thenReturn(job);
        pivotAggregationSearch.getSearchJob(parameters,
                new AggregationSearch.User(TEST_USER, DateTimeZone.UTC), WINDOW_LENGTH, WINDOW_LENGTH);
        Mockito.verify(searchNormalization, times(1)).postValidation(isA(Query.class), any());
        Mockito.verify(queryEngine, times(1)).execute(isA(SearchJob.class), argThat(Set::isEmpty), eq(DateTimeZone.UTC));
    }

    private static AggregationEventProcessorParameters buildParameters(AbsoluteRange timerange) {
        return AggregationEventProcessorParameters.builder()
                .streams(Collections.emptySet())
                .timerange(timerange)
                .batchSize(500)
                .build();
    }

    private PivotAggregationSearch createPivotAggregationSearch(
            AggregationEventProcessorConfig config,
            AggregationEventProcessorParameters parameters
    ) {
        return createPivotAggregationSearch(config, parameters, Collections.emptyList(), permittedStreams);
    }

    private PivotAggregationSearch createPivotAggregationSearch(
            AggregationEventProcessorConfig config,
            AggregationEventProcessorParameters parameters,
            List<SearchType> additionalSearchTypes
    ) {
        return createPivotAggregationSearch(config, parameters, additionalSearchTypes, permittedStreams);
    }

    private PivotAggregationSearch createPivotAggregationSearch(
            AggregationEventProcessorConfig config,
            AggregationEventProcessorParameters parameters,
            List<SearchType> additionalSearchTypes,
            PermittedStreams permittedStreams
    ) {
        return createPivotAggregationSearch(config, parameters, additionalSearchTypes, permittedStreams, new QueryStringDecorators(Optional.empty()));
    }

    private PivotAggregationSearch createPivotAggregationSearch(
            AggregationEventProcessorConfig config,
            AggregationEventProcessorParameters parameters,
            List<SearchType> additionalSearchTypes,
            PermittedStreams permittedStreams,
            QueryStringDecorators queryStringDecorators
    ) {
        return new PivotAggregationSearch(
                config,
                parameters,
                new AggregationSearch.User(TEST_USER, DateTimeZone.UTC),
                eventDefinition,
                additionalSearchTypes,
                searchJobService,
                queryEngine,
                EventsConfigurationTestProvider.create(),
                permittedStreams,
                notificationService,
                queryStringDecorators,
                streamService,
                searchNormalization,
                false
        );
    }
}
