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
package org.graylog.plugins.views.search.elasticsearch;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import org.graylog.plugins.views.search.Query;
import org.graylog.plugins.views.search.QueryMetadata;
import org.graylog.plugins.views.search.QueryResult;
import org.graylog.plugins.views.search.Search;
import org.graylog.plugins.views.search.SearchJob;
import org.graylog.plugins.views.search.SearchType;
import org.graylog.plugins.views.search.elasticsearch.searchtypes.ESMessageList;
import org.graylog.plugins.views.search.elasticsearch.searchtypes.ESSearchTypeHandler;
import org.graylog.plugins.views.search.filter.AndFilter;
import org.graylog.plugins.views.search.filter.QueryStringFilter;
import org.graylog.plugins.views.search.searchtypes.MessageList;
import org.graylog.plugins.views.search.searchtypes.pivot.Pivot;
import org.graylog2.indexer.ranges.IndexRangeService;
import org.graylog2.plugin.indexer.searches.timeranges.RelativeRange;
import org.graylog2.streams.StreamService;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.inject.Provider;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

public class ElasticsearchBackendTest {

    private static ElasticsearchBackend backend;

    @BeforeClass
    public static void setup() {
        Map<String, Provider<ESSearchTypeHandler<? extends SearchType>>> handlers = Maps.newHashMap();
        handlers.put(MessageList.NAME, () -> new ESMessageList(new ESQueryDecorators.Fake()));

        final FieldTypesLookup fieldTypesLookup = mock(FieldTypesLookup.class);
        final QueryStringParser queryStringParser = new QueryStringParser();
        backend = new ElasticsearchBackend(handlers,
                queryStringParser,
                null,
                mock(IndexRangeService.class),
                mock(StreamService.class),
                new ESQueryDecorators.Fake(),
                (elasticsearchBackend, ssb, job, query, results) -> new ESGeneratedQueryContext(elasticsearchBackend, ssb, job, query, results, fieldTypesLookup));
    }

    @Test
    public void parse() throws Exception {
        final QueryMetadata queryMetadata = backend.parse(ImmutableSet.of(), Query.builder()
                .id("abc123")
                .query(ElasticsearchQueryString.builder().queryString("user_name:$username$ http_method:$foo$").build())
                .timerange(RelativeRange.create(600))
                .build());

        assertThat(queryMetadata.usedParameterNames())
                .containsOnly("username", "foo");
    }

    @Test
    public void parseAlsoConsidersWidgetFilters() throws Exception {
        final SearchType searchType1 = Pivot.builder()
                .id("searchType1")
                .filter(QueryStringFilter.builder().query("source:$bar$").build())
                .series(new ArrayList<>())
                .rollup(false)
                .build();
        final SearchType searchType2 = Pivot.builder()
                .id("searchType2")
                .filter(AndFilter.builder().filters(ImmutableSet.of(
                        QueryStringFilter.builder().query("http_action:$baz$").build(),
                        QueryStringFilter.builder().query("source:localhost").build()
                )).build())
                .series(new ArrayList<>())
                .rollup(false)
                .build();
        final QueryMetadata queryMetadata = backend.parse(ImmutableSet.of(), Query.builder()
                .id("abc123")
                .query(ElasticsearchQueryString.builder().queryString("user_name:$username$ http_method:$foo$").build())
                .timerange(RelativeRange.create(600))
                .searchTypes(ImmutableSet.of(searchType1, searchType2))
                .build());

        assertThat(queryMetadata.usedParameterNames())
                .containsOnly("username", "foo", "bar", "baz");
    }

    @Test
    public void generatesSearchForEmptySearchTypes() throws Exception {
        final Query query = Query.builder()
                .id("query1")
                .query(ElasticsearchQueryString.builder().queryString("").build())
                .timerange(RelativeRange.create(300))
                .build();
        final Search search = Search.builder().queries(ImmutableSet.of(query)).build();
        final SearchJob job = new SearchJob("deadbeef", search, "admin");

        backend.generate(job, query, Collections.emptySet());
    }

    @Test
    public void executesSearchForEmptySearchTypes() throws Exception {
        final Query query = Query.builder()
                .id("query1")
                .query(ElasticsearchQueryString.builder().queryString("").build())
                .timerange(RelativeRange.create(300))
                .build();
        final Search search = Search.builder().queries(ImmutableSet.of(query)).build();
        final SearchJob job = new SearchJob("deadbeef", search, "admin");

        final ESGeneratedQueryContext queryContext = mock(ESGeneratedQueryContext.class);

        final QueryResult queryResult = backend.doRun(job, query, queryContext, Collections.emptySet());

        assertThat(queryResult).isNotNull();
        assertThat(queryResult.searchTypes()).isEmpty();
        assertThat(queryResult.executionStats()).isNotNull();
        assertThat(queryResult.errors()).isEmpty();
    }
}
