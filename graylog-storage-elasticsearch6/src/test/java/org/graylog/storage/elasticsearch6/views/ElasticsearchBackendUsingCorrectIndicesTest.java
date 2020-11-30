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
package org.graylog.storage.elasticsearch6.views;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import io.searchbox.client.http.JestHttpClient;
import io.searchbox.core.MultiSearch;
import org.graylog.plugins.views.search.Query;
import org.graylog.plugins.views.search.Search;
import org.graylog.plugins.views.search.SearchJob;
import org.graylog.plugins.views.search.SearchType;
import org.graylog.plugins.views.search.elasticsearch.QueryStringDecorators;
import org.graylog.plugins.views.search.elasticsearch.ElasticsearchQueryString;
import org.graylog.plugins.views.search.elasticsearch.FieldTypesLookup;
import org.graylog.plugins.views.search.elasticsearch.IndexLookup;
import org.graylog.plugins.views.search.elasticsearch.QueryStringParser;
import org.graylog.storage.elasticsearch6.views.searchtypes.ESMessageList;
import org.graylog.storage.elasticsearch6.views.searchtypes.ESSearchTypeHandler;
import org.graylog.plugins.views.search.filter.AndFilter;
import org.graylog.plugins.views.search.filter.StreamFilter;
import org.graylog.plugins.views.search.searchtypes.MessageList;
import org.graylog2.plugin.indexer.searches.timeranges.RelativeRange;
import org.graylog2.plugin.indexer.searches.timeranges.TimeRange;
import org.joda.time.DateTimeUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import javax.inject.Provider;
import java.util.Collections;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ElasticsearchBackendUsingCorrectIndicesTest extends ElasticsearchBackendTestBase {
    private static Map<String, Provider<ESSearchTypeHandler<? extends SearchType>>> handlers = ImmutableMap.of(
            MessageList.NAME, () -> new ESMessageList(new QueryStringDecorators.Fake())
    );
    private static final QueryStringParser queryStringParser = new QueryStringParser();

    @Rule
    public MockitoRule rule = MockitoJUnit.rule();

    @Mock
    private IndexLookup indexLookup;

    @Mock
    private JestHttpClient jestClient;

    @Captor
    private ArgumentCaptor<MultiSearch> clientRequestCaptor;

    private SearchJob job;
    private Query query;

    private ElasticsearchBackend backend;

    @Before
    public void setupSUT() throws Exception {
        when(jestClient.execute(any(), any())).thenReturn(resultFor(resourceFile("successfulResponseWithSingleQuery.json")));

        final FieldTypesLookup fieldTypesLookup = mock(FieldTypesLookup.class);
        this.backend = new ElasticsearchBackend(handlers,
                queryStringParser,
                jestClient,
                indexLookup,
                new QueryStringDecorators.Fake(),
                (elasticsearchBackend, ssb, job, query, results) -> new ESGeneratedQueryContext(elasticsearchBackend, ssb, job, query, results, fieldTypesLookup),
                false);
    }

    @Before
    public void before() throws Exception {
        this.query = Query.builder()
                .id("query1")
                .timerange(RelativeRange.create(600))
                .query(ElasticsearchQueryString.builder().queryString("*").build())
                .searchTypes(ImmutableSet.of(MessageList.builder().id("1").build()))
                .build();
        final Search search = Search.builder()
                .id("search1")
                .queries(ImmutableSet.of(query))
                .build();
        this.job = new SearchJob("job1", search, "admin");
    }

    @After
    public void tearDown() {
        // Some tests modify the time so we make sure to reset it after each test even if assertions fail
        DateTimeUtils.setCurrentMillisSystem();
    }

    @Test
    public void queryDoesNotFallBackToUsingAllIndicesWhenNoIndexRangesAreReturned() throws Exception {
        final ESGeneratedQueryContext context = backend.generate(job, query, Collections.emptySet());
        backend.doRun(job, query, context, Collections.emptySet());

        verify(jestClient, times(1)).execute(clientRequestCaptor.capture(), any());

        final MultiSearch clientRequest = clientRequestCaptor.getValue();
        assertThat(clientRequest).isNotNull();
        assertThat(indicesOf(clientRequest).get(0)).isEqualTo("");
    }

    @Test
    public void queryUsesCorrectTimerangeWhenDeterminingIndexRanges() throws Exception {
        final long datetimeFixture = 1530194810;
        DateTimeUtils.setCurrentMillisFixed(datetimeFixture);

        final ESGeneratedQueryContext context = backend.generate(job, query, Collections.emptySet());
        backend.doRun(job, query, context, Collections.emptySet());

        ArgumentCaptor<TimeRange> captor = ArgumentCaptor.forClass(TimeRange.class);

        verify(indexLookup, times(1))
                .indexNamesForStreamsInTimeRange(any(), captor.capture());

        assertThat(captor.getValue()).isEqualTo(RelativeRange.create(600));
    }

    private Query dummyQuery(TimeRange timeRange) {
        return Query.builder()
                .id("query1")
                .timerange(timeRange)
                .query(ElasticsearchQueryString.builder().queryString("*").build())
                .searchTypes(ImmutableSet.of(MessageList.builder().id("1").build()))
                .build();
    }
    private Search dummySearch(Query... queries) {
        return Search.builder()
                .id("search1")
                .queries(ImmutableSet.copyOf(queries))
                .build();
    }

    @Test
    public void queryUsesOnlyIndicesIncludingTimerangeAndStream() throws Exception {
        final String streamId = "streamId";

        final Query query = dummyQuery(RelativeRange.create(600))
                .toBuilder()
                .filter(StreamFilter.ofId(streamId))
                .build();
        final Search search = dummySearch(query);
        final SearchJob job = new SearchJob("job1", search, "admin");
        final ESGeneratedQueryContext context = backend.generate(job, query, Collections.emptySet());

        when(indexLookup.indexNamesForStreamsInTimeRange(ImmutableSet.of("streamId"), RelativeRange.create(600)))
                .thenReturn(ImmutableSet.of("index1", "index2"));

        backend.doRun(job, query, context, Collections.emptySet());

        verify(jestClient, times(1)).execute(clientRequestCaptor.capture(), any());

        final MultiSearch clientRequest = clientRequestCaptor.getValue();
        assertThat(clientRequest).isNotNull();
        assertThat(indicesOf(clientRequest).get(0)).isEqualTo("index1,index2");
    }

    @Test
    public void queryUsesOnlyIndicesBelongingToStream() throws Exception {

        final Query query = dummyQuery(RelativeRange.create(600)).toBuilder()
                .filter(AndFilter.and(StreamFilter.ofId("stream1"), StreamFilter.ofId("stream2")))
                .build();
        final Search search = dummySearch(query);
        final SearchJob job = new SearchJob("job1", search, "admin");
        final ESGeneratedQueryContext context = backend.generate(job, query, Collections.emptySet());

        when(indexLookup.indexNamesForStreamsInTimeRange(ImmutableSet.of("stream1", "stream2"), RelativeRange.create(600)))
                .thenReturn(ImmutableSet.of("index1", "index2"));

        backend.doRun(job, query, context, Collections.emptySet());

        verify(jestClient, times(1)).execute(clientRequestCaptor.capture(), any());

        final MultiSearch clientRequest = clientRequestCaptor.getValue();
        assertThat(clientRequest).isNotNull();
        assertThat(indicesOf(clientRequest).get(0)).isEqualTo("index1,index2");
    }
}
