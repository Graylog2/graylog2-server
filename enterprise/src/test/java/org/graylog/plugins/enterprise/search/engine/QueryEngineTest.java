package org.graylog.plugins.enterprise.search.engine;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import org.elasticsearch.search.sort.SortOrder;
import org.graylog.plugins.enterprise.search.Query;
import org.graylog.plugins.enterprise.search.Search;
import org.graylog.plugins.enterprise.search.SearchJob;
import org.graylog.plugins.enterprise.search.SearchType;
import org.graylog.plugins.enterprise.search.elasticsearch.ElasticsearchBackend;
import org.graylog.plugins.enterprise.search.elasticsearch.ElasticsearchQueryGenerator;
import org.graylog.plugins.enterprise.search.elasticsearch.ElasticsearchQueryString;
import org.graylog.plugins.enterprise.search.elasticsearch.searchtypes.ESDateHistogram;
import org.graylog.plugins.enterprise.search.elasticsearch.searchtypes.ESMessageList;
import org.graylog.plugins.enterprise.search.elasticsearch.searchtypes.ESSearchTypeHandler;
import org.graylog.plugins.enterprise.search.filter.OrFilter;
import org.graylog.plugins.enterprise.search.filter.StreamFilter;
import org.graylog.plugins.enterprise.search.searchtypes.DateHistogram;
import org.graylog.plugins.enterprise.search.searchtypes.MessageList;
import org.graylog.plugins.enterprise.search.searchtypes.Sort;
import org.graylog2.plugin.Message;
import org.graylog2.plugin.indexer.searches.timeranges.RelativeRange;
import org.graylog2.plugin.streams.Stream;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

public class QueryEngineTest {
    private static final Logger LOG = LoggerFactory.getLogger(QueryEngineTest.class);
    private static ImmutableMap<String, QueryBackend> queryGenerators;


    @BeforeClass
    public static void setup() {
        Map<String, ESSearchTypeHandler<? extends SearchType>> searchTypeHandlers = Maps.newHashMap();
        searchTypeHandlers.put(MessageList.NAME, new ESMessageList());
        searchTypeHandlers.put(DateHistogram.NAME, new ESDateHistogram());

        queryGenerators = ImmutableMap.<String, QueryBackend>builder()
                .put(ElasticsearchQueryString.NAME,
                        new ElasticsearchBackend(searchTypeHandlers,
                                new ElasticsearchQueryGenerator(searchTypeHandlers),
                                null))
                .build();
    }

    @Ignore("Fails with a null jestClient -- test not ready yet?")
    @Test
    public void test() throws ExecutionException, InterruptedException {

        final Query query = Query.builder()
                .id(randomId())
                .query(ElasticsearchQueryString.builder().queryString("_exists_:message").build())
                .timerange(RelativeRange.builder().type(RelativeRange.RELATIVE).range(600).build())
                .filter(OrFilter.or(
                        StreamFilter.ofId(Stream.DEFAULT_STREAM_ID),
                        StreamFilter.ofId("000000000000000000000002"))
                )
                .searchTypes(ImmutableList.of(
                        MessageList.builder()
                                .id(randomId())
                                .offset(0)
                                .limit(10)
                                .sort(ImmutableList.of(
                                        Sort.create(Message.FIELD_TIMESTAMP, SortOrder.DESC),
                                        Sort.create(Message.FIELD_SOURCE, SortOrder.ASC)))
                                .build()
                        )
                )
                .build();

        final Search search = Search.builder().id(randomId()).queries(ImmutableList.of(query)).build();

        LOG.warn("Query {}", query);

        final SearchJob job = new SearchJob(randomId(), search);

        final SearchJob searchJob = new QueryEngine(queryGenerators).execute(job);

        searchJob.getResultFuture().thenAccept(o -> LOG.warn("result {}", o)).get();
    }

    private static String randomId() {
        return UUID.randomUUID().toString();
    }

}