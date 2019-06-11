package org.graylog.plugins.enterprise.search.engine;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import io.searchbox.client.JestClient;
import org.graylog.plugins.enterprise.search.Parameter;
import org.graylog.plugins.enterprise.search.Query;
import org.graylog.plugins.enterprise.search.QueryResult;
import org.graylog.plugins.enterprise.search.Search;
import org.graylog.plugins.enterprise.search.SearchJob;
import org.graylog.plugins.enterprise.search.SearchType;
import org.graylog.plugins.enterprise.search.elasticsearch.ESQueryDecorators;
import org.graylog.plugins.enterprise.search.elasticsearch.ElasticsearchBackend;
import org.graylog.plugins.enterprise.search.elasticsearch.ElasticsearchQueryString;
import org.graylog.plugins.enterprise.search.elasticsearch.QueryStringParser;
import org.graylog.plugins.enterprise.search.elasticsearch.searchtypes.ESDateHistogram;
import org.graylog.plugins.enterprise.search.elasticsearch.searchtypes.ESMessageList;
import org.graylog.plugins.enterprise.search.elasticsearch.searchtypes.ESSearchTypeHandler;
import org.graylog.plugins.enterprise.search.searchtypes.DateHistogram;
import org.graylog.plugins.enterprise.search.searchtypes.MessageList;
import org.graylog2.indexer.ranges.IndexRangeService;
import org.graylog2.plugin.indexer.searches.timeranges.InvalidRangeParametersException;
import org.graylog2.plugin.indexer.searches.timeranges.RelativeRange;
import org.graylog2.rest.models.messages.responses.ResultMessageSummary;
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
import static org.graylog.plugins.enterprise.search.params.QueryReferenceBinding.bindToQueryRef;
import static org.graylog.plugins.enterprise.search.params.ValueBinding.bindToValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

public class QueryPlanTest {
    private static final Logger LOG = LoggerFactory.getLogger(QueryPlanTest.class);

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

    @Test
    public void referenceParamBinding() {
        final Query topLevelQuery = wildcardQueryBuilder()
                .searchTypes(of(MessageList.builder().id("messages").limit(10).offset(0).build()))
                .build();
        final Query subQuery = stringQueryBuilder("user_id:$USER_ID$").build();
        Search search = Search.builder()
                .queries(of(subQuery, topLevelQuery)) // reverse the actual order to make it non-obvious that subQuery depends on topLevelQuery
                .parameters(of(Parameter.builder()
                        .name("USER_ID")
                        .dataType("string")
                        .binding(bindToQueryRef(topLevelQuery.id(),
                                "messages",
                                "$.messages[0].message.user_id"))
                        .build()))
                .build();
        SearchJob job = new SearchJob(randomUUID(), search, "admin");
        final QueryPlan queryPlan = new QueryPlan(queryEngine, job);

        ImmutableList<Query> elements = queryPlan.queries();
        LOG.warn(queryPlan.toString());
        // check that the subquery isn't scheduled before the topLevelQuery
        assertThat(elements).containsExactly(topLevelQuery, subQuery);
    }

    @Test
    public void executeQueryRefPlan() {
        final Query topLevelQuery = wildcardQueryBuilder("top")
                .searchTypes(of(MessageList.builder().id("messages").limit(10).offset(0).build()))
                .build();
        final Query subQuery = stringQueryBuilder("user_id:$USER_ID$", "sub")
                .searchTypes(of(MessageList.builder().id("messages").limit(10).offset(0).build()))
                .build();
        Search search = Search.builder()
                .queries(of(subQuery, topLevelQuery)) // reverse the actual order to make it non-obvious that subQuery depends on topLevelQuery
                .parameters(of(Parameter.builder()
                        .name("USER_ID")
                        .dataType("string")
                        .binding(bindToQueryRef("top","messages",
                                // TODO this is ugly because it relies on `ResultMessage` objects :/
                                "$.messages[0].message.user_id")) // selects the first message's user_id field
                        .build()))
                .build();
        SearchJob job = new SearchJob(randomUUID(), search, "admin");

        final Map<String, Provider<ESSearchTypeHandler<? extends SearchType>>> handlers =
                ImmutableMap.of(MessageList.NAME, () -> new ESMessageList(new ESQueryDecorators.Fake()));

        final QueryStringParser queryStringParser = new QueryStringParser();
        final ElasticsearchBackend esBackend = spy(new ElasticsearchBackend(handlers, queryStringParser, mock(JestClient.class), mock(IndexRangeService.class), mock(StreamService.class), new ESQueryDecorators.Fake()));

        doReturn(QueryResult.builder()
                .searchTypes(ImmutableMap.of("messages", MessageList.Result.builder()
                        .id("messages")
                        .totalResults(1)
                        .messages(Collections.singletonList(ResultMessageSummary.create(
                                null,
                                ImmutableMap.of("user_id", 123456789),
                                "test_1")))
                        .build()))
                .query(topLevelQuery)
                .build())
                .when(esBackend).run(eq(job), argThat(argument -> topLevelQuery.id().equals(argument.id())), any(), any());
        doReturn(QueryResult.emptyResult())
                .when(esBackend).run(eq(job), argThat(argument -> subQuery.id().equals(argument.id())), any(), any());

        final ImmutableMap<String, QueryBackend<?>> elasticsearch = ImmutableMap.of("elasticsearch", esBackend);
        QueryEngine engine = new QueryEngine(elasticsearch, Collections.emptySet());

        final SearchJob execute = engine.execute(job);
        execute.getResultFuture().join();
        assertThat(execute.getResultFuture()).isCompleted();
    }

    private Query.Builder stringQueryBuilder(String queryString) {
        return stringQueryBuilder(queryString, null);
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
