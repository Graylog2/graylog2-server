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
package org.graylog.storage.elasticsearch6.views.searchtypes.pivots;

import com.google.common.collect.ImmutableList;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import com.revinate.assertj.json.JsonPathAssert;
import io.searchbox.core.SearchResult;
import io.searchbox.core.search.aggregation.Aggregation;
import io.searchbox.core.search.aggregation.MaxAggregation;
import io.searchbox.core.search.aggregation.MetricAggregation;
import io.searchbox.core.search.aggregation.MinAggregation;
import org.assertj.core.api.AbstractCharSequenceAssert;
import org.graylog.shaded.elasticsearch5.org.elasticsearch.index.query.QueryBuilders;
import org.graylog.shaded.elasticsearch5.org.elasticsearch.search.aggregations.AggregationBuilders;
import org.graylog.shaded.elasticsearch5.org.elasticsearch.search.builder.SearchSourceBuilder;
import org.graylog.plugins.views.search.Query;
import org.graylog.plugins.views.search.SearchJob;
import org.graylog.plugins.views.search.SearchType;
import org.graylog.storage.elasticsearch6.views.ESGeneratedQueryContext;
import org.graylog.storage.elasticsearch6.views.searchtypes.pivot.ESPivot;
import org.graylog.storage.elasticsearch6.views.searchtypes.pivot.ESPivotBucketSpecHandler;
import org.graylog.storage.elasticsearch6.views.searchtypes.pivot.ESPivotSeriesSpecHandler;
import org.graylog.storage.elasticsearch6.views.searchtypes.pivot.buckets.ESTimeHandler;
import org.graylog.storage.elasticsearch6.views.searchtypes.pivot.buckets.ESValuesHandler;
import org.graylog.storage.elasticsearch6.views.searchtypes.pivot.series.ESCountHandler;
import org.graylog.plugins.views.search.searchtypes.pivot.BucketSpec;
import org.graylog.plugins.views.search.searchtypes.pivot.Pivot;
import org.graylog.plugins.views.search.searchtypes.pivot.PivotResult;
import org.graylog.plugins.views.search.searchtypes.pivot.SeriesSpec;
import org.graylog.plugins.views.search.searchtypes.pivot.buckets.AutoInterval;
import org.graylog.plugins.views.search.searchtypes.pivot.buckets.Time;
import org.graylog.plugins.views.search.searchtypes.pivot.buckets.Values;
import org.graylog.plugins.views.search.searchtypes.pivot.series.Count;
import org.graylog2.plugin.indexer.searches.timeranges.AbsoluteRange;
import org.graylog2.plugin.indexer.searches.timeranges.InvalidRangeParametersException;
import org.graylog2.plugin.indexer.searches.timeranges.RelativeRange;
import org.joda.time.DateTime;
import org.joda.time.DateTimeUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ESPivotTest {
    @Rule
    public final MockitoRule mockitoRule = MockitoJUnit.rule();
    @Mock
    private SearchJob job;
    @Mock
    private Query query;
    @Mock
    private Pivot pivot;
    @Mock
    private SearchResult queryResult;
    @Mock
    private MetricAggregation aggregations;
    @Mock
    private ESGeneratedQueryContext queryContext;

    private ESPivot esPivot;
    private Map<String, ESPivotBucketSpecHandler<? extends BucketSpec, ? extends Aggregation>> bucketHandlers;
    private Map<String, ESPivotSeriesSpecHandler<? extends SeriesSpec, ? extends Aggregation>> seriesHandlers;

    @Before
    public void setUp() throws Exception {
        bucketHandlers = new HashMap<>();
        seriesHandlers = new HashMap<>();
        this.esPivot = new ESPivot(bucketHandlers, seriesHandlers);
        when(pivot.id()).thenReturn("dummypivot");
    }

    @After
    public void tearDown() throws Exception {
        // Some tests modify the time so we make sure to reset it after each test even if assertions fail
        DateTimeUtils.setCurrentMillisSystem();
    }

    private MetricAggregation createTimestampRangeAggregations(Double min, Double max) {
        final MetricAggregation metricAggregation = mock(MetricAggregation.class);

        final MinAggregation timestampMinAggregation = mock(MinAggregation.class);
        when(timestampMinAggregation.getMin()).thenReturn(min);
        final MaxAggregation timestampMaxAggregation = mock(MaxAggregation.class);
        when(timestampMaxAggregation.getMax()).thenReturn(max);

        when(metricAggregation.getMinAggregation("timestamp-min")).thenReturn(timestampMinAggregation);
        when(metricAggregation.getMaxAggregation("timestamp-max")).thenReturn(timestampMaxAggregation);

        return metricAggregation;
    }

    @Test
    public void searchResultIncludesDocumentCount() throws InvalidRangeParametersException {
        final long documentCount = 424242;
        when(queryResult.getTotal()).thenReturn(documentCount);
        final MetricAggregation mockMetricAggregation = createTimestampRangeAggregations((double) new Date().getTime(), (double) new Date().getTime());
        when(queryResult.getAggregations()).thenReturn(mockMetricAggregation);
        when(query.effectiveTimeRange(pivot)).thenReturn(RelativeRange.create(300));

        final SearchType.Result result = this.esPivot.doExtractResult(job, query, pivot, queryResult, aggregations, queryContext);

        final PivotResult pivotResult = (PivotResult)result;

        assertThat(pivotResult.total()).isEqualTo(documentCount);
    }

    @Test
    public void generatesQueryWhenOnlyColumnPivotsArePresent() {
        final SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();

        when(queryContext.searchSourceBuilder(pivot)).thenReturn(searchSourceBuilder);
        when(queryContext.nextName()).thenReturn("agg-1");
        final Values values = Values.builder().field("action").limit(10).build();
        when(pivot.columnGroups()).thenReturn(Collections.singletonList(values));

        this.esPivot.doGenerateQueryPart(job, query, pivot, queryContext);

        final DocumentContext context = JsonPath.parse(searchSourceBuilder.toString());
        extractAggregation(context, "values-agg")
                .isEqualTo("Values{type=values, field=action, limit=10}");
        extractAggregation(context, "values-agg.time-agg")
                .isEqualTo("Time{type=time, field=timestamp, interval=AutoInterval{type=auto, scaling=1.0}}");
    }

    private void mockBucketSpecGeneratesComparableString(ESPivotBucketSpecHandler<? extends BucketSpec, ? extends Aggregation> bucketHandler) {
        when(bucketHandler.createAggregation(any(), any(), any(), any(), any(), any()))
                .thenAnswer(invocation -> Optional.of(AggregationBuilders.filter(invocation.getArgument(0), QueryBuilders.existsQuery(invocation.getArgument(2).toString()))));
    }

    @Test
    @Ignore
    // TODO: remove or change, test is no longer possible this way
    public void columnPivotsShouldBeNested() {
        final SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        final ESPivotBucketSpecHandler<? extends BucketSpec, ? extends Aggregation> valuesBucketHandler = mock(ESValuesHandler.class);
        mockBucketSpecGeneratesComparableString(valuesBucketHandler);
        final ESPivotBucketSpecHandler<? extends BucketSpec, ? extends Aggregation> timeBucketHandler = mock(ESTimeHandler.class);
        mockBucketSpecGeneratesComparableString(timeBucketHandler);

        bucketHandlers.put(Values.NAME, valuesBucketHandler);
        bucketHandlers.put(Time.NAME, timeBucketHandler);

        when(queryContext.searchSourceBuilder(pivot)).thenReturn(searchSourceBuilder);
        when(queryContext.nextName()).thenReturn("values-agg", "time-agg");

        final Values values = Values.builder().field("action").limit(10).build();
        final Time time = Time.builder().field("timestamp").interval(AutoInterval.create()).build();
        when(pivot.columnGroups()).thenReturn(ImmutableList.of(values, time));

        this.esPivot.doGenerateQueryPart(job, query, pivot, queryContext);

        verify(valuesBucketHandler, times(1)).createAggregation(eq("values-agg"), eq(pivot), eq(values), eq(this.esPivot), eq(queryContext), eq(query));
        verify(timeBucketHandler, times(1)).createAggregation(eq("time-agg"), eq(pivot), eq(time), eq(this.esPivot), eq(queryContext), eq(query));

        final DocumentContext context = JsonPath.parse(searchSourceBuilder.toString());
        extractAggregation(context, "values-agg")
                .isEqualTo("Values{type=values, field=action, limit=10}");
        extractAggregation(context, "values-agg.time-agg")
                .isEqualTo("Time{type=time, field=timestamp, interval=AutoInterval{type=auto, scaling=1.0}}");
    }

    @Test
    public void rowPivotsShouldBeInOneScriptedTerm() {
        final String AGG_NAME = "agg-1";
        final String ROW1_NAME = "timestamp";
        final String ROW2_NAME = "action";

        final SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();

        when(queryContext.searchSourceBuilder(pivot)).thenReturn(searchSourceBuilder);
        when(queryContext.nextName()).thenReturn(AGG_NAME);

        final Time time = Time.builder().field(ROW1_NAME).interval(AutoInterval.create()).build();
        final Values values = Values.builder().field(ROW2_NAME).limit(10).build();
        when(pivot.rowGroups()).thenReturn(ImmutableList.of(time, values));

        this.esPivot.doGenerateQueryPart(job, query, pivot, queryContext);

        final DocumentContext context = JsonPath.parse(searchSourceBuilder.toString());
        extractScriptedTerm(context, AGG_NAME).isEqualTo("doc['" + ROW1_NAME + "'].value + '⸱' + doc['" + ROW2_NAME + "'].value");
    }

    @Test
    @Ignore
    // TODO: remove or change, test is no longer possible this way
    public void mixedPivotsShouldBeNested() {
        final SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        final ESPivotBucketSpecHandler<? extends BucketSpec, ? extends Aggregation> valuesBucketHandler = mock(ESValuesHandler.class);
        mockBucketSpecGeneratesComparableString(valuesBucketHandler);
        final ESPivotBucketSpecHandler<? extends BucketSpec, ? extends Aggregation> timeBucketHandler = mock(ESTimeHandler.class);
        mockBucketSpecGeneratesComparableString(timeBucketHandler);

        bucketHandlers.put(Values.NAME, valuesBucketHandler);
        bucketHandlers.put(Time.NAME, timeBucketHandler);

        when(queryContext.searchSourceBuilder(pivot)).thenReturn(searchSourceBuilder);
        when(queryContext.nextName()).thenReturn("time-agg", "values-agg");

        final Time time = Time.builder().field("timestamp").interval(AutoInterval.create()).build();
        final Values values = Values.builder().field("action").limit(10).build();
        when(pivot.rowGroups()).thenReturn(Collections.singletonList(time));
        when(pivot.columnGroups()).thenReturn(Collections.singletonList(values));

        this.esPivot.doGenerateQueryPart(job, query, pivot, queryContext);

        verify(valuesBucketHandler, times(1)).createAggregation(eq("values-agg"), eq(pivot), eq(values), eq(this.esPivot), eq(queryContext), eq(query));
        verify(timeBucketHandler, times(1)).createAggregation(eq("time-agg"), eq(pivot), eq(time), eq(this.esPivot), eq(queryContext), eq(query));

        final DocumentContext context = JsonPath.parse(searchSourceBuilder.toString());
        extractAggregation(context, "time-agg")
                .isEqualTo("Time{type=time, field=timestamp, interval=AutoInterval{type=auto, scaling=1.0}}");
        extractAggregation(context, "time-agg.values-agg")
                .isEqualTo("Values{type=values, field=action, limit=10}");
    }

    private void mockSeriesSpecGeneratesComparableString(ESPivotSeriesSpecHandler<? extends SeriesSpec, ? extends Aggregation> seriesHandler) {
        when(seriesHandler.createAggregation(any(), any(), any(), any(), any()))
                .thenAnswer(invocation -> Optional.of(AggregationBuilders.filter(invocation.getArgument(0), QueryBuilders.existsQuery(invocation.getArgument(2).toString()))));
    }

    @Test
    @Ignore
    // TODO: remove or change, test is no longer possible this way
    public void mixedPivotsAndSeriesShouldBeNested() {
        final SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        final ESPivotBucketSpecHandler<? extends BucketSpec, ? extends Aggregation> valuesBucketHandler = mock(ESValuesHandler.class);
        mockBucketSpecGeneratesComparableString(valuesBucketHandler);
        final ESPivotBucketSpecHandler<? extends BucketSpec, ? extends Aggregation> timeBucketHandler = mock(ESTimeHandler.class);
        mockBucketSpecGeneratesComparableString(timeBucketHandler);
        final ESPivotSeriesSpecHandler<? extends SeriesSpec, ? extends Aggregation> countHandler = mock(ESCountHandler.class);
        mockSeriesSpecGeneratesComparableString(countHandler);

        bucketHandlers.put(Values.NAME, valuesBucketHandler);
        bucketHandlers.put(Time.NAME, timeBucketHandler);

        seriesHandlers.put(Count.NAME, countHandler);

        when(queryContext.searchSourceBuilder(pivot)).thenReturn(searchSourceBuilder);
        when(queryContext.nextName()).thenReturn("rowPivot1", "rowPivot2", "columnPivot1", "columnPivot2");

        final BucketSpec rowPivot1 = Time.builder().field("timestamp").interval(AutoInterval.create()).build();
        final BucketSpec rowPivot2 = Values.builder().field("http_method").limit(10).build();
        final BucketSpec columnPivot1 = Values.builder().field("controller").limit(10).build();
        final BucketSpec columnPivot2 = Values.builder().field("action").limit(10).build();
        final Count count = Count.builder().build();
        when(pivot.rowGroups()).thenReturn(ImmutableList.of(rowPivot1, rowPivot2));
        when(pivot.columnGroups()).thenReturn(ImmutableList.of(columnPivot1, columnPivot2));
        when(pivot.series()).thenReturn(Collections.singletonList(count));
        when(pivot.rollup()).thenReturn(false);
        when(queryContext.seriesName(any(), any())).thenCallRealMethod();

        this.esPivot.doGenerateQueryPart(job, query, pivot, queryContext);

        verify(timeBucketHandler).createAggregation(eq("rowPivot1"), eq(pivot), eq(rowPivot1), eq(this.esPivot), eq(queryContext), eq(query));
        verify(valuesBucketHandler).createAggregation(eq("rowPivot2"), eq(pivot), eq(rowPivot2), eq(this.esPivot), eq(queryContext), eq(query));
        verify(valuesBucketHandler).createAggregation(eq("columnPivot1"), eq(pivot), eq(columnPivot1), eq(this.esPivot), eq(queryContext), eq(query));
        verify(valuesBucketHandler).createAggregation(eq("columnPivot2"), eq(pivot), eq(columnPivot2), eq(this.esPivot), eq(queryContext), eq(query));

        final DocumentContext context = JsonPath.parse(searchSourceBuilder.toString());
        extractAggregation(context, "rowPivot1")
                .isEqualTo("Time{type=time, field=timestamp, interval=AutoInterval{type=auto, scaling=1.0}}");
        extractAggregation(context, "rowPivot1.rowPivot2")
                .isEqualTo("Values{type=values, field=http_method, limit=10}");
        extractAggregation(context, "rowPivot1.rowPivot2.columnPivot1")
                .isEqualTo("Values{type=values, field=controller, limit=10}");
        extractAggregation(context, "rowPivot1.rowPivot2.columnPivot1.columnPivot2")
                .isEqualTo("Values{type=values, field=action, limit=10}");
        extractAggregation(context, "rowPivot1.rowPivot2.dummypivot-series-count()")
                .isEqualTo("Count{type=count, id=count(), field=null}");
        extractAggregation(context, "rowPivot1.rowPivot2.columnPivot1.columnPivot2.dummypivot-series-count()")
                .isEqualTo("Count{type=count, id=count(), field=null}");
    }

    private AbstractCharSequenceAssert<?, String> extractAggregation(DocumentContext context, String path) {
        final String fullPath = Stream.of(path.split("\\.")).map(s -> "['aggregations']['" + s + "']").reduce("$", (s1, s2) -> s1 + s2) + "['filter']['exists']['field']";
        return JsonPathAssert.assertThat(context).jsonPathAsString(fullPath);
    }

    private AbstractCharSequenceAssert<?, String> extractScriptedTerm(DocumentContext context, String path) {
        final String fullPath = Stream.of(path.split("\\.")).map(s -> "['aggregations']['" + s + "']").reduce("$", (s1, s2) -> s1 + s2) + "['terms']['script']['source']";
        return JsonPathAssert.assertThat(context).jsonPathAsString(fullPath);
    }

    @Test
    public void includesCustomNameinResultIfPresent() throws InvalidRangeParametersException {
        final ESPivot esPivot = new ESPivot(Collections.emptyMap(), Collections.emptyMap());
        final Pivot pivot = Pivot.builder()
                .id("somePivotId")
                .name("customPivot")
                .series(Collections.emptyList())
                .rollup(false)
                .build();
        final long documentCount = 424242;
        when(queryResult.getTotal()).thenReturn(documentCount);
        final MetricAggregation mockMetricAggregation = createTimestampRangeAggregations((double) new Date().getTime(), (double) new Date().getTime());
        when(queryResult.getAggregations()).thenReturn(mockMetricAggregation);
        when(query.effectiveTimeRange(pivot)).thenReturn(RelativeRange.create(300));
        when(queryContext.contextMap()).thenReturn(Collections.singletonMap("somePivotId-rows", "agg-name"));

        final SearchType.Result result = esPivot.doExtractResult(null, query, pivot, queryResult, null, queryContext);

        assertThat(result.name()).contains("customPivot");
    }

    @Test
    public void searchResultIncludesTimerangeOfPivot() throws InvalidRangeParametersException {
        DateTimeUtils.setCurrentMillisFixed(1578584665408L);
        final long documentCount = 424242;
        when(queryResult.getTotal()).thenReturn(documentCount);
        final MetricAggregation mockMetricAggregation = createTimestampRangeAggregations((double) new Date().getTime(), (double) new Date().getTime());
        when(queryResult.getAggregations()).thenReturn(mockMetricAggregation);
        when(query.effectiveTimeRange(pivot)).thenReturn(RelativeRange.create(300));

        final SearchType.Result result = this.esPivot.doExtractResult(job, query, pivot, queryResult, aggregations, queryContext);

        final PivotResult pivotResult = (PivotResult) result;

        assertThat(pivotResult.effectiveTimerange()).isEqualTo(AbsoluteRange.create(
                DateTime.parse("2020-01-09T15:39:25.408Z"),
                DateTime.parse("2020-01-09T15:44:25.408Z")
        ));
    }

    @Test
    public void searchResultForAllMessagesIncludesTimerangeOfDocuments() throws InvalidRangeParametersException {
        DateTimeUtils.setCurrentMillisFixed(1578584665408L);
        final long documentCount = 424242;
        when(queryResult.getTotal()).thenReturn(documentCount);
        final MetricAggregation mockMetricAggregation = createTimestampRangeAggregations(
                (double) new Date(1547303022000L).getTime(),
                (double) new Date(1578040943000L).getTime()
        );
        when(queryResult.getAggregations()).thenReturn(mockMetricAggregation);
        when(query.effectiveTimeRange(pivot)).thenReturn(RelativeRange.create(0));

        final SearchType.Result result = this.esPivot.doExtractResult(job, query, pivot, queryResult, aggregations, queryContext);

        final PivotResult pivotResult = (PivotResult) result;

        assertThat(pivotResult.effectiveTimerange()).isEqualTo(AbsoluteRange.create(
                DateTime.parse("2019-01-12T14:23:42.000Z"),
                DateTime.parse("2020-01-03T08:42:23.000Z")
        ));
        DateTimeUtils.setCurrentMillisSystem();
    }

    @Test
    public void searchResultForAllMessagesIncludesPivotTimerangeForNoDocuments() throws InvalidRangeParametersException {
        DateTimeUtils.setCurrentMillisFixed(1578584665408L);
        final long documentCount = 0;
        when(queryResult.getTotal()).thenReturn(documentCount);
        final MetricAggregation mockMetricAggregation = createTimestampRangeAggregations(null, null);
        when(queryResult.getAggregations()).thenReturn(mockMetricAggregation);
        when(query.effectiveTimeRange(pivot)).thenReturn(RelativeRange.create(0));

        final SearchType.Result result = this.esPivot.doExtractResult(job, query, pivot, queryResult, aggregations, queryContext);

        final PivotResult pivotResult = (PivotResult) result;

        assertThat(pivotResult.effectiveTimerange()).isEqualTo(AbsoluteRange.create(
                DateTime.parse("1970-01-01T00:00:00.000Z"),
                DateTime.parse("2020-01-09T15:44:25.408Z")
        ));
    }
}
