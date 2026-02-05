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
package org.graylog.storage.opensearch3.views.searchtypes.pivots;

import org.graylog.plugins.views.search.Query;
import org.graylog.plugins.views.search.SearchType;
import org.graylog.plugins.views.search.searchtypes.pivot.Pivot;
import org.graylog.plugins.views.search.searchtypes.pivot.PivotResult;
import org.graylog.storage.opensearch3.views.OSGeneratedQueryContext;
import org.graylog.storage.opensearch3.views.searchtypes.OSSearchTypeHandler;
import org.graylog.storage.opensearch3.views.searchtypes.pivot.EffectiveTimeRangeExtractor;
import org.graylog.storage.opensearch3.views.searchtypes.pivot.OSPivot;
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
import org.opensearch.client.json.JsonData;
import org.opensearch.client.opensearch._types.aggregations.Aggregate;
import org.opensearch.client.opensearch.core.msearch.MultiSearchItem;
import org.opensearch.client.opensearch.core.search.HitsMetadata;
import org.opensearch.client.opensearch.core.search.TotalHits;

import java.util.Collections;
import java.util.Date;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
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
    private MultiSearchItem<JsonData> queryResult;
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

    private Map<String, Aggregate> createTimestampRangeAggregations(Double min, Double max) {
        return Map.of(
                "timestamp-min", Aggregate.builder().min(m -> m.value(min)).build(),
                "timestamp-max", Aggregate.builder().max(m -> m.value(max)).build()
        );
    }

    private void returnDocumentCount(MultiSearchItem<JsonData> queryResult, long totalCount) {
        HitsMetadata<JsonData> hits = mock(HitsMetadata.class);
        TotalHits total = TotalHits.builder().value(totalCount).build();
        when(hits.total()).thenReturn(total);
        when(queryResult.hits()).thenReturn(hits);
    }

    @Test
    public void searchResultIncludesDocumentCount() {
        final long documentCount = 424242;
        returnDocumentCount(queryResult, documentCount);
        final Map<String, Aggregate> mockMetricAggregation = createTimestampRangeAggregations((double) new Date().getTime(), (double) new Date().getTime());
        when(queryResult.aggregations()).thenReturn(mockMetricAggregation);
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
        final Map<String, Aggregate> mockMetricAggregation = createTimestampRangeAggregations((double) new Date().getTime(), (double) new Date().getTime());
        when(queryResult.aggregations()).thenReturn(mockMetricAggregation);
        when(query.effectiveTimeRange(pivot)).thenReturn(RelativeRange.create(300));

        final SearchType.Result result = esPivot.doExtractResult(query, pivot, queryResult, null);

        assertThat(result.name()).contains("customPivot");
    }

    @Test
    public void searchResultIncludesTimerangeOfPivot() throws InvalidRangeParametersException {
        DateTimeUtils.setCurrentMillisFixed(1578584665408L);
        final long documentCount = 424242;
        returnDocumentCount(queryResult, documentCount);
        final Map<String, Aggregate> mockMetricAggregation = createTimestampRangeAggregations((double) new Date().getTime(), (double) new Date().getTime());
        when(queryResult.aggregations()).thenReturn(mockMetricAggregation);
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
        final Map<String, Aggregate> mockMetricAggregation = createTimestampRangeAggregations(
                (double) new Date(1547303022000L).getTime(),
                (double) new Date(1578040943000L).getTime()
        );
        when(queryResult.aggregations()).thenReturn(mockMetricAggregation);
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

}
