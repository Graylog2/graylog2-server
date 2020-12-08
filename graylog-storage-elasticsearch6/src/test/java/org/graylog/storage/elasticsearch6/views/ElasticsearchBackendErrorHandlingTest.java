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

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import io.searchbox.client.JestClient;
import io.searchbox.core.MultiSearchResult;
import org.graylog.shaded.elasticsearch5.org.elasticsearch.search.builder.SearchSourceBuilder;
import org.graylog.plugins.views.search.Query;
import org.graylog.plugins.views.search.QueryResult;
import org.graylog.plugins.views.search.Search;
import org.graylog.plugins.views.search.SearchJob;
import org.graylog.plugins.views.search.SearchType;
import org.graylog.plugins.views.search.elasticsearch.QueryStringDecorators;
import org.graylog.plugins.views.search.elasticsearch.ElasticsearchQueryString;
import org.graylog.plugins.views.search.elasticsearch.FieldTypesLookup;
import org.graylog.plugins.views.search.elasticsearch.IndexLookup;
import org.graylog.plugins.views.search.elasticsearch.QueryStringParser;
import org.graylog.storage.elasticsearch6.views.searchtypes.ESSearchTypeHandler;
import org.graylog.plugins.views.search.errors.SearchError;
import org.graylog2.indexer.ElasticsearchException;
import org.graylog2.plugin.indexer.searches.timeranges.RelativeRange;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.io.IOException;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ElasticsearchBackendErrorHandlingTest extends ElasticsearchBackendTestBase {
    @Rule
    public MockitoRule rule = MockitoJUnit.rule();

    @Mock
    private JestClient jestClient;

    @Mock
    protected IndexLookup indexLookup;

    @Mock
    private MultiSearchResult result;

    private ElasticsearchBackend backend;
    private SearchJob searchJob;
    private Query query;
    private ESGeneratedQueryContext queryContext;

    static abstract class DummyHandler implements ESSearchTypeHandler<SearchType> {}

    @Before
    public void setUp() throws Exception {
        final FieldTypesLookup fieldTypesLookup = mock(FieldTypesLookup.class);
        this.backend = new ElasticsearchBackend(
                ImmutableMap.of(
                        "dummy", () -> mock(DummyHandler.class)
                ),
                new QueryStringParser(),
                jestClient,
                indexLookup,
                new QueryStringDecorators(Collections.emptySet()),
                (elasticsearchBackend, ssb, job, query, results) -> new ESGeneratedQueryContext(elasticsearchBackend, ssb, job, query, results, fieldTypesLookup),
                false
        );
        when(indexLookup.indexNamesForStreamsInTimeRange(any(), any())).thenReturn(Collections.emptySet());

        final SearchType searchType1 = mock(SearchType.class);
        when(searchType1.id()).thenReturn("deadbeef");
        when(searchType1.type()).thenReturn("dummy");
        final SearchType searchType2 = mock(SearchType.class);
        when(searchType2.id()).thenReturn("cafeaffe");
        when(searchType2.type()).thenReturn("dummy");

        final Set<SearchType> searchTypes = ImmutableSet.of(searchType1, searchType2);
        this.query = Query.builder()
                .id("query1")
                .timerange(RelativeRange.create(300))
                .query(ElasticsearchQueryString.builder().queryString("*").build())
                .searchTypes(searchTypes)
                .build();
        final Search search = Search.builder()
                .id("search1")
                .queries(ImmutableSet.of(query))
                .build();

        this.searchJob = new SearchJob("job1", search, "admin");

        this.queryContext = new ESGeneratedQueryContext(
                this.backend,
                new SearchSourceBuilder(),
                searchJob,
                query,
                Collections.emptySet(),
                mock(FieldTypesLookup.class)
        );

        searchTypes.forEach(queryContext::searchSourceBuilder);

        when(jestClient.execute(any())).thenReturn(result);
    }

    @Test
    public void deduplicatesShardErrorsOnQueryLevel() throws IOException {
        when(result.isSucceeded()).thenReturn(false);
        final JsonNode resultObject = objectMapper
                .readTree(resourceFile("errorhandling/failureOnQueryLevel.json"));
        when(result.getJsonObject()).thenReturn(resultObject);

        assertThatExceptionOfType(ElasticsearchException.class)
                .isThrownBy(() -> this.backend.doRun(searchJob, query, queryContext, Collections.emptySet()))
                .satisfies(ex -> {
                    assertThat(ex.getErrorDetails()).hasSize(1);
                    assertThat(ex.getErrorDetails()).containsExactly("Something went wrong");
                });
    }

    @Test
    public void deduplicateShardErrorsOnSearchTypeLevel() throws IOException {
        final MultiSearchResult multiSearchResult = searchResultFromFixture("errorhandling/failureOnSearchTypeLevel.json");
        when(jestClient.execute(any())).thenReturn(multiSearchResult);

        final QueryResult queryResult = this.backend.doRun(searchJob, query, queryContext, Collections.emptySet());

        final Set<SearchError> errors = queryResult.errors();

        assertThat(errors).isNotNull();
        assertThat(errors).hasSize(1);
        assertThat(errors.stream().map(SearchError::description).collect(Collectors.toList()))
                .containsExactly("Unable to perform search query: \n\nFailed to parse query [[].");
    }

    @Test
    public void deduplicateNumericShardErrorsOnSearchTypeLevel() throws IOException {
        final MultiSearchResult multiSearchResult = searchResultFromFixture("errorhandling/numericFailureOnSearchTypeLevel.json");
        when(jestClient.execute(any())).thenReturn(multiSearchResult);

        final QueryResult queryResult = this.backend.doRun(searchJob, query, queryContext, Collections.emptySet());

        final Set<SearchError> errors = queryResult.errors();

        assertThat(errors).isNotNull();
        assertThat(errors).hasSize(1);
        assertThat(errors.stream().map(SearchError::description).collect(Collectors.toList()))
                .containsExactly("Unable to perform search query: \n\nExpected numeric type on field [facility], but got [keyword].");
    }

    private MultiSearchResult searchResultFromFixture(String filename) throws IOException {
        final JsonNode resultObject = objectMapper
                .readTree(resourceFile(filename));
        final MultiSearchResult multiSearchResult = new MultiSearchResult(objectMapper);
        multiSearchResult.setJsonObject(resultObject);
        multiSearchResult.setSucceeded(true);

        return multiSearchResult;
    }
}
