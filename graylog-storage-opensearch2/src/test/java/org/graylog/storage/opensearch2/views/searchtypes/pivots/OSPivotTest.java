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
package org.graylog.storage.opensearch2.views.searchtypes.pivots;

import com.google.common.collect.ImmutableList;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import org.graylog.plugins.views.search.Query;
import org.graylog.plugins.views.search.SearchType;
import org.graylog.plugins.views.search.engine.IndexerGeneratedQueryContext;
import org.graylog.plugins.views.search.searchtypes.pivot.BucketSpec;
import org.graylog.plugins.views.search.searchtypes.pivot.BucketSpecHandler;
import org.graylog.plugins.views.search.searchtypes.pivot.Pivot;
import org.graylog.plugins.views.search.searchtypes.pivot.PivotResult;
import org.graylog.plugins.views.search.searchtypes.pivot.PivotSpec;
import org.graylog.plugins.views.search.searchtypes.pivot.SeriesSpec;
import org.graylog.plugins.views.search.searchtypes.pivot.SeriesSpecHandler;
import org.graylog.plugins.views.search.searchtypes.pivot.buckets.Values;
import org.graylog.plugins.views.search.searchtypes.pivot.series.Count;
import org.graylog.shaded.opensearch2.org.apache.lucene.search.TotalHits;
import org.graylog.shaded.opensearch2.org.opensearch.action.search.SearchResponse;
import org.graylog.shaded.opensearch2.org.opensearch.search.SearchHit;
import org.graylog.shaded.opensearch2.org.opensearch.search.SearchHits;
import org.graylog.shaded.opensearch2.org.opensearch.search.aggregations.Aggregation;
import org.graylog.shaded.opensearch2.org.opensearch.search.aggregations.AggregationBuilder;
import org.graylog.shaded.opensearch2.org.opensearch.search.aggregations.AggregationBuilders;
import org.graylog.shaded.opensearch2.org.opensearch.search.aggregations.Aggregations;
import org.graylog.shaded.opensearch2.org.opensearch.search.aggregations.HasAggregations;
import org.graylog.shaded.opensearch2.org.opensearch.search.aggregations.bucket.MultiBucketsAggregation;
import org.graylog.shaded.opensearch2.org.opensearch.search.aggregations.metrics.Max;
import org.graylog.shaded.opensearch2.org.opensearch.search.aggregations.metrics.Min;
import org.graylog.shaded.opensearch2.org.opensearch.search.builder.SearchSourceBuilder;
import org.graylog.storage.opensearch2.views.OSGeneratedQueryContext;
import org.graylog.storage.opensearch2.views.searchtypes.OSSearchTypeHandler;
import org.graylog.storage.opensearch2.views.searchtypes.pivot.EffectiveTimeRangeExtractor;
import org.graylog.storage.opensearch2.views.searchtypes.pivot.OSPivot;
import org.graylog.storage.opensearch2.views.searchtypes.pivot.OSPivotBucketSpecHandler;
import org.graylog.storage.opensearch2.views.searchtypes.pivot.OSPivotSeriesSpecHandler;
import org.graylog.storage.opensearch2.views.searchtypes.pivot.PivotBucket;
import org.graylog.storage.opensearch2.views.searchtypes.pivot.SeriesAggregationBuilder;
import org.graylog.testing.jsonpath.JsonPathAssert;
import org.graylog2.plugin.indexer.searches.timeranges.AbsoluteRange;
import org.graylog2.plugin.indexer.searches.timeranges.InvalidRangeParametersException;
import org.graylog2.plugin.indexer.searches.timeranges.RelativeRange;
import org.graylog2.plugin.indexer.searches.timeranges.TimeRange;
import org.joda.time.DateTime;
import org.joda.time.DateTimeUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.WARN)
public class OSPivotTest {
    @Mock
    private Query query;
    @Mock
    private Pivot pivot;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private SearchResponse queryResult;
    @Mock
    private OSGeneratedQueryContext queryContext;

    private OSSearchTypeHandler<Pivot> esPivot;

    @BeforeEach
    public void setUp() throws Exception {
        this.esPivot = new OSPivot(Collections.emptyMap(), Collections.emptyMap(), new EffectiveTimeRangeExtractor());
        when(pivot.id()).thenReturn("dummypivot");
    }

    @AfterEach
    public void tearDown() throws Exception {
        // Some tests modify the time so we make sure to reset it after each test even if assertions fail
        DateTimeUtils.setCurrentMillisSystem();
    }

    private Aggregations createTimestampRangeAggregations(Double min, Double max) {
        final Min timestampMinAggregation = mock(Min.class);
        when(timestampMinAggregation.getValue()).thenReturn(min);
        when(timestampMinAggregation.getName()).thenReturn("timestamp-min");
        final Max timestampMaxAggregation = mock(Max.class);
        when(timestampMaxAggregation.getValue()).thenReturn(max);
        when(timestampMaxAggregation.getName()).thenReturn("timestamp-max");

        return new Aggregations(ImmutableList.of(timestampMinAggregation, timestampMaxAggregation));
    }

    private void returnDocumentCount(SearchResponse queryResult, long totalCount) {
        final TotalHits totalHits = new TotalHits(totalCount, TotalHits.Relation.EQUAL_TO);
        final SearchHits searchHits = new SearchHits(new SearchHit[0], totalHits, 0.0f);
        when(queryResult.getHits()).thenReturn(searchHits);
    }

    @Test
    public void searchResultIncludesDocumentCount() {
        final long documentCount = 424242;
        returnDocumentCount(queryResult, documentCount);
        final Aggregations mockMetricAggregation = createTimestampRangeAggregations((double) new Date().getTime(), (double) new Date().getTime());
        when(queryResult.getAggregations()).thenReturn(mockMetricAggregation);
        when(query.effectiveTimeRange(pivot)).thenReturn(RelativeRange.create(300));

        final SearchType.Result result = this.esPivot.doExtractResult(query, pivot, queryResult, queryContext);

        final PivotResult pivotResult = (PivotResult)result;

        assertThat(pivotResult.total()).isEqualTo(documentCount);
    }

    @Test
    public void includesCustomNameinResultIfPresent() throws InvalidRangeParametersException {
        final Pivot pivot = Pivot.builder()
                .id("somePivotId")
                .name("customPivot")
                .series(Collections.emptyList())
                .rollup(false)
                .build();
        final long documentCount = 424242;
        returnDocumentCount(queryResult, documentCount);
        final Aggregations mockMetricAggregation = createTimestampRangeAggregations((double) new Date().getTime(), (double) new Date().getTime());
        when(queryResult.getAggregations()).thenReturn(mockMetricAggregation);
        when(query.effectiveTimeRange(pivot)).thenReturn(RelativeRange.create(300));

        final SearchType.Result result = esPivot.doExtractResult(query, pivot, queryResult, null);

        assertThat(result.name()).contains("customPivot");
    }

    @Test
    public void searchResultIncludesTimerangeOfPivot() throws InvalidRangeParametersException {
        DateTimeUtils.setCurrentMillisFixed(1578584665408L);
        final long documentCount = 424242;
        returnDocumentCount(queryResult, documentCount);
        final Aggregations mockMetricAggregation = createTimestampRangeAggregations((double) new Date().getTime(), (double) new Date().getTime());
        when(queryResult.getAggregations()).thenReturn(mockMetricAggregation);
        when(query.effectiveTimeRange(pivot)).thenReturn(RelativeRange.create(300));

        final SearchType.Result result = this.esPivot.doExtractResult(query, pivot, queryResult, queryContext);

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
        returnDocumentCount(queryResult, documentCount);
        final Aggregations mockMetricAggregation = createTimestampRangeAggregations(
                (double) new Date(1547303022000L).getTime(),
                (double) new Date(1578040943000L).getTime()
        );
        when(queryResult.getAggregations()).thenReturn(mockMetricAggregation);
        when(query.effectiveTimeRange(pivot)).thenReturn(RelativeRange.create(0));

        final SearchType.Result result = this.esPivot.doExtractResult(query, pivot, queryResult, queryContext);

        final PivotResult pivotResult = (PivotResult) result;

        assertThat(pivotResult.effectiveTimerange()).isEqualTo(AbsoluteRange.create(
                DateTime.parse("2019-01-12T14:23:42.000Z"),
                DateTime.parse("2020-01-03T08:42:23.000Z")
        ));
        DateTimeUtils.setCurrentMillisSystem();
    }

    @Test
    public void searchResultForAllMessagesIncludesPivotTimerangeForNoDocuments() throws InvalidRangeParametersException {
        returnDocumentCount(queryResult, 0);
        final TimeRange pivotRange = AbsoluteRange.create(DateTime.parse("1970-01-01T00:00:00.000Z"), DateTime.parse("2020-01-09T15:44:25.408Z"));
        when(query.effectiveTimeRange(pivot)).thenReturn(pivotRange);
        final SearchType.Result result = this.esPivot.doExtractResult(query, pivot, queryResult, queryContext);
        final PivotResult pivotResult = (PivotResult) result;
        assertThat(pivotResult.effectiveTimerange()).isEqualTo(AbsoluteRange.create(
                DateTime.parse("1970-01-01T00:00:00.000Z"),
                DateTime.parse("2020-01-09T15:44:25.408Z")
        ));
    }

    // ----------------------------------------------------------------------------------------------------------------
    // Query generation (doGenerateQueryPart)
    // ----------------------------------------------------------------------------------------------------------------

    @Test
    public void alwaysAddsTimestampMinMaxAggregations() {
        final Pivot pivot = Pivot.builder().id("p").series().rollup(false).build();

        final DocumentContext doc = generateQueryPart(pivot, Map.of(), Map.of());

        JsonPathAssert.assertThat(doc).jsonPathAsString("$.aggregations['timestamp-min'].min.field").isEqualTo("timestamp");
        JsonPathAssert.assertThat(doc).jsonPathAsString("$.aggregations['timestamp-max'].max.field").isEqualTo("timestamp");
    }

    @Test
    public void generatesAggregationForRowGroup() {
        final Pivot pivot = Pivot.builder().id("p").rowGroups(values("field1")).series().rollup(false).build();

        final DocumentContext doc = generateQueryPart(pivot, Map.of(Values.NAME, new FakeBucketHandler()), Map.of());

        JsonPathAssert.assertThat(doc).jsonPathAsString("$.aggregations.agg.terms.field").isEqualTo("field1");
    }

    @Test
    public void nestsMultipleRowGroups() {
        final Pivot pivot = Pivot.builder().id("p").rowGroups(values("field1"), values("field2")).series().rollup(false).build();

        final DocumentContext doc = generateQueryPart(pivot, Map.of(Values.NAME, new FakeBucketHandler()), Map.of());

        JsonPathAssert.assertThat(doc).jsonPathAsString("$.aggregations.agg.terms.field").isEqualTo("field1");
        JsonPathAssert.assertThat(doc).jsonPathAsString("$.aggregations.agg.aggregations.agg.terms.field").isEqualTo("field2");
    }

    @Test
    public void attachesMetricSeriesAsSubAggregationOfRowLeaf() {
        final Pivot pivot = Pivot.builder().id("p").rowGroups(values("field1")).series(count()).rollup(false).build();

        final DocumentContext doc = generateQueryPart(pivot,
                Map.of(Values.NAME, new FakeBucketHandler()),
                Map.of(Count.NAME, new FakeSeriesHandler(SeriesAggregationBuilder::metric, List.of())));

        JsonPathAssert.assertThat(doc).jsonPathAsString("$.aggregations.agg.aggregations.metricseries.max.field").isEqualTo("value");
    }

    @Test
    public void attachesGlobalRollupMetricSeriesAtRootWhenNoGroupsPresent() {
        final Pivot pivot = Pivot.builder().id("p").series(count()).rollup(false).build();

        final DocumentContext doc = generateQueryPart(pivot,
                Map.of(),
                Map.of(Count.NAME, new FakeSeriesHandler(SeriesAggregationBuilder::metric, List.of())));

        JsonPathAssert.assertThat(doc).jsonPathAsString("$.aggregations.metricseries.max.field").isEqualTo("value");
    }

    @Test
    public void attachesColumnGroupUnderRowLeaf() {
        final Pivot pivot = Pivot.builder()
                .id("p")
                .rowGroups(values("row1"))
                .columnGroups(values("col1"))
                .series()
                .rollup(false)
                .build();

        final DocumentContext doc = generateQueryPart(pivot, Map.of(Values.NAME, new FakeBucketHandler()), Map.of());

        JsonPathAssert.assertThat(doc).jsonPathAsString("$.aggregations.agg.terms.field").isEqualTo("row1");
        JsonPathAssert.assertThat(doc).jsonPathAsString("$.aggregations.agg.aggregations.agg.terms.field").isEqualTo("col1");
    }

    @Test
    public void attachesColumnGroupAtRootWhenNoRowGroupsPresent() {
        final Pivot pivot = Pivot.builder()
                .id("p")
                .columnGroups(values("col1"))
                .series()
                .rollup(false)
                .build();

        final DocumentContext doc = generateQueryPart(pivot, Map.of(Values.NAME, new FakeBucketHandler()), Map.of());

        JsonPathAssert.assertThat(doc).jsonPathAsString("$.aggregations.agg.terms.field").isEqualTo("col1");
    }

    @Test
    public void attachesRootPlacedSeriesAtRootWhenNotGeneratingRollups() {
        final Pivot pivot = Pivot.builder().id("p").rowGroups(values("row1")).series(count()).rollup(false).build();

        final DocumentContext doc = generateQueryPart(pivot,
                Map.of(Values.NAME, new FakeBucketHandler()),
                Map.of(Count.NAME, new FakeSeriesHandler(SeriesAggregationBuilder::root, List.of())));

        JsonPathAssert.assertThat(doc).jsonPathAsString("$.aggregations.metricseries.max.field").isEqualTo("value");
    }

    @Test
    public void throwsWhenNoSeriesHandlerIsRegistered() {
        final Pivot pivot = Pivot.builder().id("p").series(count()).rollup(false).build();

        final SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        final OSGeneratedQueryContext context = mock(OSGeneratedQueryContext.class);
        when(context.searchSourceBuilder(any())).thenReturn(searchSourceBuilder);
        final OSPivot sut = new OSPivot(Map.of(), Map.of(), new EffectiveTimeRangeExtractor());

        assertThatThrownBy(() -> sut.doGenerateQueryPart(query, pivot, context))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("No series handler registered for: " + Count.NAME);
    }

    // ----------------------------------------------------------------------------------------------------------------
    // Result extraction (doExtractResult) with row/column/series handling
    // ----------------------------------------------------------------------------------------------------------------

    @Test
    public void extractsOneLeafRowPerRowGroupBucket() {
        stubResultMetadata();
        final Pivot pivot = Pivot.builder().id("p").rowGroups(values("field1")).series(count()).rollup(false).build();

        final FakeBucketHandler bucketHandler = new FakeBucketHandler(Map.of("field1", List.of(
                PivotBucket.create(ImmutableList.of("a"), mock(MultiBucketsAggregation.Bucket.class)),
                PivotBucket.create(ImmutableList.of("b"), mock(MultiBucketsAggregation.Bucket.class))
        )));
        final FakeSeriesHandler seriesHandler = new FakeSeriesHandler(SeriesAggregationBuilder::metric,
                List.of(SeriesSpecHandler.Value.create("count()", Count.NAME, 42L)));
        final OSPivot sut = new OSPivot(Map.of(Values.NAME, bucketHandler), Map.of(Count.NAME, seriesHandler), new EffectiveTimeRangeExtractor());

        final PivotResult result = (PivotResult) sut.doExtractResult(query, pivot, queryResult, queryContext);

        assertThat(result.rows()).hasSize(2);
        assertThat(result.rows().get(0).key()).containsExactly("a");
        assertThat(result.rows().get(0).source()).isEqualTo("leaf");
        assertThat(result.rows().get(0).values()).hasSize(1);
        final PivotResult.Value value = result.rows().get(0).values().get(0);
        assertThat(value.key()).containsExactly("count()");
        assertThat(value.value()).isEqualTo(42L);
        assertThat(value.rollup()).isTrue();
        assertThat(value.source()).isEqualTo("row-leaf");
        assertThat(result.rows().get(1).key()).containsExactly("b");
    }

    @Test
    public void addsRollupRowWhenRowGroupsPresentAndRollupEnabled() {
        stubResultMetadata();
        final Pivot pivot = Pivot.builder().id("p").rowGroups(values("field1")).series(count()).rollup(true).build();

        final FakeBucketHandler bucketHandler = new FakeBucketHandler(Map.of("field1", List.of(
                PivotBucket.create(ImmutableList.of("a"), mock(MultiBucketsAggregation.Bucket.class))
        )));
        final FakeSeriesHandler seriesHandler = new FakeSeriesHandler(SeriesAggregationBuilder::metric,
                List.of(SeriesSpecHandler.Value.create("count()", Count.NAME, 7L)));
        final OSPivot sut = new OSPivot(Map.of(Values.NAME, bucketHandler), Map.of(Count.NAME, seriesHandler), new EffectiveTimeRangeExtractor());

        final PivotResult result = (PivotResult) sut.doExtractResult(query, pivot, queryResult, queryContext);

        assertThat(result.rows()).hasSize(2);
        final PivotResult.Row rollupRow = result.rows().get(1);
        assertThat(rollupRow.key()).isEmpty();
        assertThat(rollupRow.source()).isEqualTo("non-leaf");
        assertThat(rollupRow.values()).hasSize(1);
        assertThat(rollupRow.values().get(0).source()).isEqualTo("row-inner");
    }

    @Test
    public void extractsNestedColumnValues() {
        stubResultMetadata();
        final Pivot pivot = Pivot.builder()
                .id("p")
                .rowGroups(values("row1"))
                .columnGroups(values("col1"))
                .series(count())
                .rollup(false)
                .build();

        final FakeBucketHandler bucketHandler = new FakeBucketHandler(Map.of(
                "row1", List.of(PivotBucket.create(ImmutableList.of("a"), mock(MultiBucketsAggregation.Bucket.class))),
                "col1", List.of(PivotBucket.create(ImmutableList.of("x"), mock(MultiBucketsAggregation.Bucket.class)))
        ));
        final FakeSeriesHandler seriesHandler = new FakeSeriesHandler(SeriesAggregationBuilder::metric,
                List.of(SeriesSpecHandler.Value.create("count()", Count.NAME, 5L)));
        when(queryContext.withRowBucket(any())).thenReturn(queryContext);
        final OSPivot sut = new OSPivot(Map.of(Values.NAME, bucketHandler), Map.of(Count.NAME, seriesHandler), new EffectiveTimeRangeExtractor());

        final PivotResult result = (PivotResult) sut.doExtractResult(query, pivot, queryResult, queryContext);

        assertThat(result.rows()).hasSize(1);
        final PivotResult.Row row = result.rows().get(0);
        assertThat(row.key()).containsExactly("a");
        assertThat(row.values()).hasSize(1);
        final PivotResult.Value value = row.values().get(0);
        assertThat(value.key()).containsExactly("x", "count()");
        assertThat(value.value()).isEqualTo(5L);
        assertThat(value.source()).isEqualTo("col-leaf");
    }

    @Test
    public void buildsColumnNamesFromRowFieldsAndColumnKeys() {
        stubResultMetadata();
        final Pivot pivot = Pivot.builder()
                .id("p")
                .rowGroups(values("row1"))
                .columnGroups(values("col1"))
                .series(count())
                .rollup(false)
                .build();

        final FakeBucketHandler bucketHandler = new FakeBucketHandler(Map.of(
                "row1", List.of(PivotBucket.create(ImmutableList.of("a"), mock(MultiBucketsAggregation.Bucket.class))),
                "col1", List.of(PivotBucket.create(ImmutableList.of("x"), mock(MultiBucketsAggregation.Bucket.class)))
        ));
        final FakeSeriesHandler seriesHandler = new FakeSeriesHandler(SeriesAggregationBuilder::metric,
                List.of(SeriesSpecHandler.Value.create("count()", Count.NAME, 5L)));
        when(queryContext.withRowBucket(any())).thenReturn(queryContext);
        final OSPivot sut = new OSPivot(Map.of(Values.NAME, bucketHandler), Map.of(Count.NAME, seriesHandler), new EffectiveTimeRangeExtractor());

        final PivotResult result = (PivotResult) sut.doExtractResult(query, pivot, queryResult, queryContext);

        assertThat(result.columnNames()).containsExactly("row1", "x, count()");
    }

    // ----------------------------------------------------------------------------------------------------------------
    // Helpers
    // ----------------------------------------------------------------------------------------------------------------

    private DocumentContext generateQueryPart(Pivot pivot,
                                              Map<String, OSPivotBucketSpecHandler<? extends BucketSpec>> bucketHandlers,
                                              Map<String, OSPivotSeriesSpecHandler<? extends SeriesSpec, ? extends Aggregation>> seriesHandlers) {
        final SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        final OSGeneratedQueryContext context = mock(OSGeneratedQueryContext.class);
        when(context.searchSourceBuilder(any())).thenReturn(searchSourceBuilder);
        when(context.seriesName(any(), any())).thenReturn("metricseries");

        final OSPivot sut = new OSPivot(bucketHandlers, seriesHandlers, new EffectiveTimeRangeExtractor());
        sut.doGenerateQueryPart(query, pivot, context);

        return JsonPath.parse(searchSourceBuilder.toString());
    }

    private void stubResultMetadata() {
        returnDocumentCount(queryResult, 10);
        final Aggregations timestampAggregations = createTimestampRangeAggregations(
                (double) new Date().getTime(), (double) new Date().getTime());
        when(queryResult.getAggregations()).thenReturn(timestampAggregations);
        when(query.effectiveTimeRange(any())).thenReturn(RelativeRange.create(300));
    }

    private static Values values(String field) {
        return Values.builder().field(field).build();
    }

    private static Count count() {
        return Count.builder().id("count()").build();
    }

    private static class FakeBucketHandler extends OSPivotBucketSpecHandler<Values> {
        private final Map<String, List<PivotBucket>> bucketsByField;

        private FakeBucketHandler() {
            this(Map.of());
        }

        private FakeBucketHandler(Map<String, List<PivotBucket>> bucketsByField) {
            this.bucketsByField = bucketsByField;
        }

        @Override
        public CreatedAggregations<AggregationBuilder> doCreateAggregation(Direction direction, String name, Pivot pivot, Values bucketSpec, OSGeneratedQueryContext queryContext, Query query) {
            return CreatedAggregations.create(AggregationBuilders.terms(name).field(bucketSpec.fields().get(0)));
        }

        @Override
        public Stream<PivotBucket> extractBuckets(Pivot pivot, BucketSpec bucketSpecs, PivotBucket previousBucket) {
            return bucketsByField.getOrDefault(bucketSpecs.fields().get(0), List.of()).stream();
        }
    }

    private static class FakeSeriesHandler extends OSPivotSeriesSpecHandler<Count, Aggregation> {
        private final Function<AggregationBuilder, SeriesAggregationBuilder> placement;
        private final List<SeriesSpecHandler.Value> resultValues;

        private FakeSeriesHandler(Function<AggregationBuilder, SeriesAggregationBuilder> placement, List<SeriesSpecHandler.Value> resultValues) {
            this.placement = placement;
            this.resultValues = resultValues;
        }

        @Override
        public List<SeriesAggregationBuilder> doCreateAggregation(String name, Pivot pivot, Count seriesSpec, OSGeneratedQueryContext queryContext) {
            return List.of(placement.apply(AggregationBuilders.max(name).field("value")));
        }

        @Override
        public Aggregation extractAggregationFromResult(Pivot pivot, PivotSpec spec, HasAggregations currentAggregationOrBucket, IndexerGeneratedQueryContext<?> queryContext) {
            return null;
        }

        @Override
        public Stream<SeriesSpecHandler.Value> doHandleResult(Pivot pivot, Count seriesSpec, SearchResponse searchResult, Aggregation aggregationResult, OSGeneratedQueryContext queryContext) {
            return resultValues.stream();
        }
    }
}
