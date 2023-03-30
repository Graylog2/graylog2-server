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
import org.graylog.plugins.views.search.Query;
import org.graylog.plugins.views.search.SearchJob;
import org.graylog.plugins.views.search.SearchType;
import org.graylog.plugins.views.search.searchtypes.pivot.Pivot;
import org.graylog.plugins.views.search.searchtypes.pivot.PivotResult;
import org.graylog.plugins.views.search.searchtypes.pivot.SeriesSpec;
import org.graylog.shaded.opensearch2.org.apache.lucene.search.TotalHits;
import org.graylog.shaded.opensearch2.org.opensearch.action.search.SearchResponse;
import org.graylog.shaded.opensearch2.org.opensearch.search.SearchHit;
import org.graylog.shaded.opensearch2.org.opensearch.search.SearchHits;
import org.graylog.shaded.opensearch2.org.opensearch.search.aggregations.Aggregation;
import org.graylog.shaded.opensearch2.org.opensearch.search.aggregations.Aggregations;
import org.graylog.shaded.opensearch2.org.opensearch.search.aggregations.metrics.Max;
import org.graylog.shaded.opensearch2.org.opensearch.search.aggregations.metrics.Min;
import org.graylog.storage.opensearch2.views.OSGeneratedQueryContext;
import org.graylog.storage.opensearch2.views.searchtypes.OSSearchTypeHandler;
import org.graylog.storage.opensearch2.views.searchtypes.pivot.EffectiveTimeRangeExtractor;
import org.graylog.storage.opensearch2.views.searchtypes.pivot.OSPivotSeriesSpecHandler;
import org.graylog.storage.opensearch2.views.searchtypes.pivot.OSPivot;
import org.graylog.storage.opensearch2.views.searchtypes.pivot.buckets.OSTimeHandler;
import org.graylog2.plugin.indexer.searches.timeranges.AbsoluteRange;
import org.graylog2.plugin.indexer.searches.timeranges.InvalidRangeParametersException;
import org.graylog2.plugin.indexer.searches.timeranges.RelativeRange;
import org.graylog2.plugin.indexer.searches.timeranges.TimeRange;
import org.graylog2.storage.SearchVersion;
import org.joda.time.DateTime;
import org.joda.time.DateTimeUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class OSPivotTest {
    @Rule
    public final MockitoRule mockitoRule = MockitoJUnit.rule();
    @Mock
    private SearchJob job;
    @Mock
    private Query query;
    @Mock
    private Pivot pivot;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private SearchResponse queryResult;
    @Mock
    private Aggregations aggregations;
    @Mock
    private OSGeneratedQueryContext queryContext;

    private OSSearchTypeHandler<Pivot> esPivot;

    @Before
    public void setUp() throws Exception {
        this.esPivot = new OSPivot(Collections.emptyMap(), Collections.emptyMap(), new EffectiveTimeRangeExtractor());
        when(pivot.id()).thenReturn("dummypivot");
    }

    @After
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

        final SearchType.Result result = this.esPivot.doExtractResult(job, query, pivot, queryResult, aggregations, queryContext);

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

        final SearchType.Result result = esPivot.doExtractResult(null, query, pivot, queryResult, null, null);

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
        returnDocumentCount(queryResult, documentCount);
        final Aggregations mockMetricAggregation = createTimestampRangeAggregations(
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
        returnDocumentCount(queryResult, 0);
        final TimeRange pivotRange = AbsoluteRange.create(DateTime.parse("1970-01-01T00:00:00.000Z"), DateTime.parse("2020-01-09T15:44:25.408Z"));
        when(query.effectiveTimeRange(pivot)).thenReturn(pivotRange);
        final SearchType.Result result = this.esPivot.doExtractResult(job, query, pivot, queryResult, aggregations, queryContext);
        final PivotResult pivotResult = (PivotResult) result;
        assertThat(pivotResult.effectiveTimerange()).isEqualTo(AbsoluteRange.create(
                DateTime.parse("1970-01-01T00:00:00.000Z"),
                DateTime.parse("2020-01-09T15:44:25.408Z")
        ));
    }

}
