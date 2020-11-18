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

import com.google.common.collect.ImmutableSet;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import io.searchbox.client.JestClient;
import org.graylog.plugins.views.search.Query;
import org.graylog.plugins.views.search.Search;
import org.graylog.plugins.views.search.SearchJob;
import org.graylog.plugins.views.search.SearchType;
import org.graylog.plugins.views.search.elasticsearch.ElasticsearchQueryString;
import org.graylog.plugins.views.search.elasticsearch.FieldTypesLookup;
import org.graylog.plugins.views.search.elasticsearch.IndexLookup;
import org.graylog.plugins.views.search.elasticsearch.QueryStringDecorators;
import org.graylog.plugins.views.search.elasticsearch.QueryStringParser;
import org.graylog.plugins.views.search.engine.QueryStringDecorator;
import org.graylog2.plugin.indexer.searches.timeranges.InvalidRangeParametersException;
import org.graylog2.plugin.indexer.searches.timeranges.RelativeRange;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;

import static com.revinate.assertj.json.JsonPathAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ElasticsearchBackendQueryStringDecoratorsTest {
    public static final String PATH_TO_QUERY_STRING = "$.query.bool.must[0].bool.filter[0].query_string.query";
    private ElasticsearchBackend backend;

    @BeforeEach
    void setUp() {
        final QueryStringDecorator decorator = (queryString, job, query, results) -> "decorated";
        final Set<QueryStringDecorator> decorators = Collections.singleton(decorator);
        final FieldTypesLookup fieldTypesLookup = mock(FieldTypesLookup.class);
        this.backend = new ElasticsearchBackend(
                Collections.emptyMap(),
                new QueryStringParser(),
                mock(JestClient.class),
                mock(IndexLookup.class),
                new QueryStringDecorators(decorators),
                (elasticsearchBackend, ssb, job, query, results) -> new ESGeneratedQueryContext(elasticsearchBackend, ssb, job, query, results, fieldTypesLookup),
                true
        );
    }

    @Test
    void generateAppliesQueryStringDecorators() throws Exception {
        final Query query = mock(Query.class);
        final SearchJob searchJob = searchJobWithRootQueryString(query);

        final DocumentContext request = generateJsonRequest(query, searchJob);

        assertThat(request).jsonPathAsString(PATH_TO_QUERY_STRING).isEqualTo("decorated");
    }

    private DocumentContext generateJsonRequest(Query query, SearchJob searchJob) {
        final ESGeneratedQueryContext context = this.backend.generate(searchJob, query, Collections.emptySet());

        final String request = context.searchTypeQueries().get("testSearchtype").toString();
        return JsonPath.parse(request);
    }

    @Test
    void generateAppliesQueryStringDecoratorsOnSearchTypes() throws Exception {
        final Query query = mock(Query.class);
        final SearchJob searchJob = searchJobWithSearchTypeQueryString(query);

        final DocumentContext request = generateJsonRequest(query, searchJob);

        assertThat(request).jsonPathAsString(PATH_TO_QUERY_STRING).isEqualTo("decorated");
    }

    private SearchJob searchJobWithRootQueryString(Query query) throws InvalidRangeParametersException {
        final SearchType searchType = basicSearchType();
        final SearchJob searchJob = basicSearchJob(query, searchType);

        when(query.query()).thenReturn(ElasticsearchQueryString.builder().queryString("*").build());

        return searchJob;
    }

    private SearchJob searchJobWithSearchTypeQueryString(Query query) throws InvalidRangeParametersException {
        final SearchType searchType = basicSearchType();
        final SearchJob searchJob = basicSearchJob(query, searchType);

        when(query.query()).thenReturn(ElasticsearchQueryString.builder().queryString("*").build());
        when(searchType.query()).thenReturn(Optional.of(ElasticsearchQueryString.builder().queryString("Should never show up").build()));

        return searchJob;
    }

    @Nonnull
    private SearchType basicSearchType() {
        final SearchType searchType = mock(SearchType.class);

        when(searchType.id()).thenReturn("testSearchtype");
        return searchType;
    }

    @Nonnull
    private SearchJob basicSearchJob(Query query, SearchType searchType) throws InvalidRangeParametersException {
        final SearchJob searchJob = mock(SearchJob.class);
        final Search search = mock(Search.class);
        when(searchJob.getSearch()).thenReturn(search);
        when(search.queries()).thenReturn(ImmutableSet.of(query));
        when(query.effectiveTimeRange(any())).thenReturn(RelativeRange.create(300));

        when(query.searchTypes()).thenReturn(ImmutableSet.of(searchType));


        return searchJob;
    }
}
