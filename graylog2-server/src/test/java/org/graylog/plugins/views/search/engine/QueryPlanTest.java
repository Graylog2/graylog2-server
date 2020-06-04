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
package org.graylog.plugins.views.search.engine;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import org.graylog.plugins.views.search.Query;
import org.graylog.plugins.views.search.Search;
import org.graylog.plugins.views.search.SearchJob;
import org.graylog.plugins.views.search.SearchType;
import org.graylog.plugins.views.search.elasticsearch.ESGeneratedQueryContext;
import org.graylog.plugins.views.search.elasticsearch.ESQueryDecorators;
import org.graylog.plugins.views.search.elasticsearch.ElasticsearchBackend;
import org.graylog.plugins.views.search.elasticsearch.ElasticsearchQueryString;
import org.graylog.plugins.views.search.elasticsearch.FieldTypesLookup;
import org.graylog.plugins.views.search.elasticsearch.IndexLookup;
import org.graylog.plugins.views.search.elasticsearch.QueryStringParser;
import org.graylog.storage.elasticsearch6.views.searchtypes.ESMessageList;
import org.graylog.storage.elasticsearch6.views.searchtypes.ESSearchTypeHandler;
import org.graylog.plugins.views.search.searchtypes.MessageList;
import org.graylog2.plugin.indexer.searches.timeranges.InvalidRangeParametersException;
import org.graylog2.plugin.indexer.searches.timeranges.RelativeRange;
import org.junit.Test;

import javax.inject.Provider;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;

import static com.google.common.collect.ImmutableSet.of;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

public class QueryPlanTest {
    private final RelativeRange timerange;
    private final QueryEngine queryEngine;

    public QueryPlanTest() throws InvalidRangeParametersException {
        timerange = RelativeRange.create(60);
        Map<String, Provider<ESSearchTypeHandler<? extends SearchType>>> handlers = Maps.newHashMap();
        handlers.put(MessageList.NAME, () -> new ESMessageList(new ESQueryDecorators.Fake()));

        final FieldTypesLookup fieldTypesLookup = mock(FieldTypesLookup.class);
        final QueryStringParser queryStringParser = new QueryStringParser();
        ElasticsearchBackend backend = new ElasticsearchBackend(handlers,
                queryStringParser,
                null,
                mock(IndexLookup.class),
                new ESQueryDecorators.Fake(),
                (elasticsearchBackend, ssb, job, query, results) -> new ESGeneratedQueryContext(elasticsearchBackend, ssb, job, query, results, fieldTypesLookup),
                false);
        queryEngine = new QueryEngine(ImmutableMap.of("elasticsearch", backend), Collections.emptySet());
    }

    private static String randomUUID() {
        return UUID.randomUUID().toString();
    }

    @Test
    public void singleQuery() {
        Search search = Search.builder()
                .queries(of(wildcardQueryBuilder()
                        .build()))
                .build();
        SearchJob job = new SearchJob(randomUUID(), search, "admin");
        final QueryPlan queryPlan = new QueryPlan(queryEngine, job);

        ImmutableList<Query> queries = queryPlan.queries();
        assertThat(queries).doesNotContain(Query.emptyRoot());
    }

    private Query.Builder stringQueryBuilder(String queryString, String id) {
        return Query.builder()
                .id(id == null ? randomUUID() : id)
                .timerange(timerange)
                .query(ElasticsearchQueryString.builder().queryString(queryString).build());
    }

    private Query.Builder wildcardQueryBuilder() {
        return wildcardQueryBuilder(null);
    }

    private Query.Builder wildcardQueryBuilder(String id) {
        return stringQueryBuilder("*", id);
    }
}
