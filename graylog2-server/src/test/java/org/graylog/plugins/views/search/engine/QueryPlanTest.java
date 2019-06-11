package org.graylog.plugins.views.search.engine;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import org.graylog.plugins.views.search.Parameter;
import org.graylog.plugins.views.search.Query;
import org.graylog.plugins.views.search.Search;
import org.graylog.plugins.views.search.SearchJob;
import org.graylog.plugins.views.search.SearchType;
import org.graylog.plugins.views.search.elasticsearch.ESQueryDecorators;
import org.graylog.plugins.enterprise.search.elasticsearch.ElasticsearchBackend;
import org.graylog.plugins.views.search.elasticsearch.ElasticsearchQueryString;
import org.graylog.plugins.views.search.elasticsearch.QueryStringParser;
import org.graylog.plugins.views.search.elasticsearch.searchtypes.ESDateHistogram;
import org.graylog.plugins.views.search.elasticsearch.searchtypes.ESMessageList;
import org.graylog.plugins.views.search.elasticsearch.searchtypes.ESSearchTypeHandler;
import org.graylog.plugins.views.search.engine.QueryEngine;
import org.graylog.plugins.views.search.engine.QueryPlan;
import org.graylog.plugins.views.search.searchtypes.DateHistogram;
import org.graylog.plugins.views.search.searchtypes.MessageList;
import org.graylog2.indexer.ranges.IndexRangeService;
import org.graylog2.plugin.indexer.searches.timeranges.InvalidRangeParametersException;
import org.graylog2.plugin.indexer.searches.timeranges.RelativeRange;
import org.graylog2.streams.StreamService;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Provider;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;

import static com.google.common.collect.ImmutableSet.of;
import static org.assertj.core.api.Assertions.assertThat;
import static org.graylog.plugins.enterprise.search.params.ValueBinding.bindToValue;
import static org.mockito.Mockito.mock;

public class QueryPlanTest {
    private final RelativeRange timerange;
    private final QueryEngine queryEngine;

    public QueryPlanTest() throws InvalidRangeParametersException {
        timerange = RelativeRange.create(60);
        Map<String, Provider<ESSearchTypeHandler<? extends SearchType>>> handlers = Maps.newHashMap();
        handlers.put(MessageList.NAME, () -> new ESMessageList(new ESQueryDecorators.Fake()));
        handlers.put(DateHistogram.NAME, ESDateHistogram::new);

        final QueryStringParser queryStringParser = new QueryStringParser();
        ElasticsearchBackend backend = new ElasticsearchBackend(handlers, queryStringParser, null, mock(IndexRangeService.class), mock(StreamService.class), new ESQueryDecorators.Fake());
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

    @Test
    public void singleQueryValueParam() {
        Search search = Search.builder()
                .queries(of(wildcardQueryBuilder().build()))
                .parameters(of(Parameter.builder()
                        .name("PARAM1")
                        .dataType("string")
                        .binding(bindToValue("hello parameter"))
                        .build()))
                .build();
        SearchJob job = new SearchJob(randomUUID(), search, "admin");
        final QueryPlan queryPlan = new QueryPlan(queryEngine, job);

        ImmutableList<Query> elements = queryPlan.queries();
        assertThat(elements).doesNotContain(Query.emptyRoot());
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
