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
package org.graylog.storage.elasticsearch7.views;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import jakarta.inject.Provider;
import org.graylog.plugins.views.search.LegacyDecoratorProcessor;
import org.graylog.plugins.views.search.Query;
import org.graylog.plugins.views.search.Search;
import org.graylog.plugins.views.search.SearchJob;
import org.graylog.plugins.views.search.SearchType;
import org.graylog.plugins.views.search.elasticsearch.ElasticsearchQueryString;
import org.graylog.plugins.views.search.elasticsearch.IndexLookup;
import org.graylog.plugins.views.search.engine.monitoring.collection.NoOpStatsCollector;
import org.graylog.plugins.views.search.filter.AndFilter;
import org.graylog.plugins.views.search.filter.StreamFilter;
import org.graylog.plugins.views.search.searchtypes.MessageList;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.action.search.MultiSearchResponse;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.action.search.SearchRequest;
import org.graylog.storage.elasticsearch7.testing.TestMultisearchResponse;
import org.graylog.storage.elasticsearch7.views.searchtypes.ESMessageList;
import org.graylog.storage.elasticsearch7.views.searchtypes.ESSearchTypeHandler;
import org.graylog2.indexer.results.TestResultMessageFactory;
import org.graylog2.plugin.indexer.searches.timeranges.RelativeRange;
import org.graylog2.plugin.indexer.searches.timeranges.TimeRange;
import org.graylog2.streams.StreamService;
import org.joda.time.DateTimeUtils;
import org.joda.time.DateTimeZone;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.graylog.storage.elasticsearch7.views.ViewsUtils.indicesOf;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ElasticsearchBackendUsingCorrectIndicesTest extends ElasticsearchMockedClientTestBase {
    private static Map<String, Provider<ESSearchTypeHandler<? extends SearchType>>> handlers = ImmutableMap.of(
            MessageList.NAME, () -> new ESMessageList(new LegacyDecoratorProcessor.Fake(),
                    new TestResultMessageFactory(), false)
    );

    @Rule
    public MockitoRule rule = MockitoJUnit.rule();

    @Mock
    private IndexLookup indexLookup;

    @Captor
    private ArgumentCaptor<List<SearchRequest>> clientRequestCaptor;

    private SearchJob job;
    private Query query;

    private ElasticsearchBackend backend;

    @Before
    public void setupSUT() throws Exception {
        final MultiSearchResponse response = TestMultisearchResponse.fromFixture("successfulResponseWithSingleQuery.json");
        mockCancellableMSearch(response);

        this.backend = new ElasticsearchBackend(handlers,
                client,
                indexLookup,
                ViewsUtils.createTestContextFactory(),
                usedSearchFilters -> Collections.emptySet(),
                new NoOpStatsCollector<>(),
                mock(StreamService.class),
                false);
    }

    @Before
    public void before() throws Exception {
        this.query = Query.builder()
                .id("query1")
                .timerange(RelativeRange.create(600))
                .query(ElasticsearchQueryString.of("*"))
                .searchTypes(ImmutableSet.of(MessageList.builder().id("1").build()))
                .build();
        final Search search = Search.builder()
                .id("search1")
                .queries(ImmutableSet.of(query))
                .build();
        this.job = new SearchJob("job1", search, "admin", "test-node-id");
    }

    @After
    public void tearDown() {
        // Some tests modify the time so we make sure to reset it after each test even if assertions fail
        DateTimeUtils.setCurrentMillisSystem();
    }

    @Test
    public void queryDoesNotFallBackToUsingAllIndicesWhenNoIndexRangesAreReturned() throws Exception {
        final ESGeneratedQueryContext context = createContext(query);
        backend.doRun(job, query, context);

        verify(client).cancellableMsearch(clientRequestCaptor.capture());

        final List<SearchRequest> clientRequest = clientRequestCaptor.getValue();
        assertThat(clientRequest).isNotNull();
        assertThat(indicesOf(clientRequest).get(0)).isEqualTo("");
    }

    @Test
    public void queryUsesCorrectTimerangeWhenDeterminingIndexRanges() {
        final long datetimeFixture = 1530194810;
        DateTimeUtils.setCurrentMillisFixed(datetimeFixture);

        final ESGeneratedQueryContext context = createContext(query);
        backend.doRun(job, query, context);

        ArgumentCaptor<TimeRange> captor = ArgumentCaptor.forClass(TimeRange.class);

        verify(indexLookup, times(1))
                .indexNamesForStreamsInTimeRange(any(), captor.capture());

        assertThat(captor.getValue()).isEqualTo(RelativeRange.create(600));
    }

    private Query dummyQuery(TimeRange timeRange) {
        return Query.builder()
                .id("query1")
                .timerange(timeRange)
                .query(ElasticsearchQueryString.of("*"))
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
    public void queryUsesOnlyIndicesIncludingTimerangeAndStream() {
        final String streamId = "streamId";

        final Query query = dummyQuery(RelativeRange.create(600))
                .toBuilder()
                .filter(StreamFilter.ofId(streamId))
                .build();
        final Search search = dummySearch(query);
        final SearchJob job = new SearchJob("job1", search, "admin", "test-node-id");
        final ESGeneratedQueryContext context = createContext(query);

        when(indexLookup.indexNamesForStreamsInTimeRange(ImmutableSet.of("streamId"), RelativeRange.create(600)))
                .thenReturn(ImmutableSet.of("index1", "index2"));

        backend.doRun(job, query, context);

        verify(client).cancellableMsearch(clientRequestCaptor.capture());

        final List<SearchRequest> clientRequest = clientRequestCaptor.getValue();
        assertThat(clientRequest).isNotNull();
        assertThat(indicesOf(clientRequest).get(0)).isEqualTo("index1,index2");
    }

    @Test
    public void queryUsesOnlyIndicesBelongingToStream() throws Exception {
        final Query query = dummyQuery(RelativeRange.create(600)).toBuilder()
                .filter(AndFilter.and(StreamFilter.ofId("stream1"), StreamFilter.ofId("stream2")))
                .build();
        final Search search = dummySearch(query);
        final SearchJob job = new SearchJob("job1", search, "admin", "test-node-id");
        final ESGeneratedQueryContext context = createContext(query);

        when(indexLookup.indexNamesForStreamsInTimeRange(ImmutableSet.of("stream1", "stream2"), RelativeRange.create(600)))
                .thenReturn(ImmutableSet.of("index1", "index2"));

        backend.doRun(job, query, context);

        verify(client).cancellableMsearch(clientRequestCaptor.capture());

        final List<SearchRequest> clientRequest = clientRequestCaptor.getValue();
        assertThat(clientRequest).isNotNull();
        assertThat(indicesOf(clientRequest).get(0)).isEqualTo("index1,index2");
    }

    private ESGeneratedQueryContext createContext(Query query) {
        return backend.generate(query, Collections.emptySet(), DateTimeZone.UTC);
    }
}
