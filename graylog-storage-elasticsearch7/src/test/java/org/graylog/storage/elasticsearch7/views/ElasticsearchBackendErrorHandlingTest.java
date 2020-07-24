/**
 * This file is part of Graylog.
 *
 * Graylog is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.graylog.storage.elasticsearch7.views;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import org.graylog.plugins.views.search.Query;
import org.graylog.plugins.views.search.QueryResult;
import org.graylog.plugins.views.search.Search;
import org.graylog.plugins.views.search.SearchJob;
import org.graylog.plugins.views.search.SearchType;
import org.graylog.plugins.views.search.elasticsearch.ElasticsearchQueryString;
import org.graylog.plugins.views.search.elasticsearch.FieldTypesLookup;
import org.graylog.plugins.views.search.elasticsearch.IndexLookup;
import org.graylog.plugins.views.search.elasticsearch.QueryStringDecorators;
import org.graylog.plugins.views.search.elasticsearch.QueryStringParser;
import org.graylog.plugins.views.search.errors.SearchError;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.action.search.MultiSearchResponse;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.client.RestHighLevelClient;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.common.xcontent.LoggingDeprecationHandler;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.common.xcontent.NamedXContentRegistry;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.common.xcontent.XContentBuilder;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.common.xcontent.XContentFactory;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.common.xcontent.XContentParser;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.common.xcontent.XContentType;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.search.builder.SearchSourceBuilder;
import org.graylog.storage.elasticsearch7.ElasticsearchClient;
import org.graylog.storage.elasticsearch7.views.searchtypes.ESSearchTypeHandler;
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
    private RestHighLevelClient restHighLevelClient;

    private ElasticsearchClient client;

    @Mock
    protected IndexLookup indexLookup;

    private ElasticsearchBackend backend;
    private SearchJob searchJob;
    private Query query;
    private ESGeneratedQueryContext queryContext;

    static abstract class DummyHandler implements ESSearchTypeHandler<SearchType> {}

    @Before
    public void setUp() throws Exception {
        this.client = new ElasticsearchClient(restHighLevelClient);
        final FieldTypesLookup fieldTypesLookup = mock(FieldTypesLookup.class);
        this.backend = new ElasticsearchBackend(
                ImmutableMap.of(
                        "dummy", () -> mock(DummyHandler.class)
                ),
                new QueryStringParser(),
                client,
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
    }

    @Test
    public void deduplicatesShardErrorsOnQueryLevel() throws IOException {
        final MultiSearchResponse response = searchResultFromFixture("errorhandling/failureOnQueryLevel.json");
        when(restHighLevelClient.msearch(any(), any())).thenReturn(response);

        assertThatExceptionOfType(ElasticsearchException.class)
                .isThrownBy(() -> this.backend.doRun(searchJob, query, queryContext, Collections.emptySet()))
                .satisfies(ex -> {
                    assertThat(ex.getErrorDetails()).hasSize(1);
                    assertThat(ex.getErrorDetails()).containsExactly("Something went wrong");
                });
    }

    @Test
    public void deduplicateShardErrorsOnSearchTypeLevel() throws IOException {
        final MultiSearchResponse multiSearchResult = searchResultFromFixture("errorhandling/failureOnSearchTypeLevel.json");
        when(restHighLevelClient.msearch(any(), any())).thenReturn(multiSearchResult);

        final QueryResult queryResult = this.backend.doRun(searchJob, query, queryContext, Collections.emptySet());

        final Set<SearchError> errors = queryResult.errors();

        assertThat(errors).isNotNull();
        assertThat(errors).hasSize(1);
        assertThat(errors.stream().map(SearchError::description).collect(Collectors.toList()))
                .containsExactly("Unable to perform search query: \n\nFailed to parse query [[].");
    }

    @Test
    public void deduplicateNumericShardErrorsOnSearchTypeLevel() throws IOException {
        final MultiSearchResponse multiSearchResult = searchResultFromFixture("errorhandling/numericFailureOnSearchTypeLevel.json");
        when(restHighLevelClient.msearch(any(), any())).thenReturn(multiSearchResult);

        final QueryResult queryResult = this.backend.doRun(searchJob, query, queryContext, Collections.emptySet());

        final Set<SearchError> errors = queryResult.errors();

        assertThat(errors).isNotNull();
        assertThat(errors).hasSize(1);
        assertThat(errors.stream().map(SearchError::description).collect(Collectors.toList()))
                .containsExactly("Unable to perform search query: \n\nExpected numeric type on field [facility], but got [keyword].");
    }

    private MultiSearchResponse searchResultFromFixture(String filename) throws IOException {
        XContentBuilder b = XContentFactory.jsonBuilder().prettyPrint();
        try (XContentParser p = XContentFactory.xContent(XContentType.JSON)
                .createParser(
                        NamedXContentRegistry.EMPTY,
                        LoggingDeprecationHandler.INSTANCE,
                        resourceFile(filename))) {
            b.copyCurrentStructure(p);

            return MultiSearchResponse.fromXContext(p);
        }
    }
}
