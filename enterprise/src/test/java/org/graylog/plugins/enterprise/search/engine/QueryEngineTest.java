package org.graylog.plugins.enterprise.search.engine;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import org.elasticsearch.search.sort.SortOrder;
import org.graylog.plugins.enterprise.search.Query;
import org.graylog.plugins.enterprise.search.QueryJob;
import org.graylog.plugins.enterprise.search.QueryResult;
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
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
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

    @Test
    public void test() throws ExecutionException, InterruptedException {

        final Query query = Query.builder()
                .query(ElasticsearchQueryString.builder().queryString("_exists_:message").build())
                .timerange(RelativeRange.builder().type(RelativeRange.RELATIVE).range(600).build())
                .filter(OrFilter.or(
                        StreamFilter.ofId(Stream.DEFAULT_STREAM_ID),
                        StreamFilter.ofId("000000000000000000000002"))
                )
                .searchTypes(ImmutableList.of(
                        MessageList.builder()
                                .offset(0)
                                .limit(10)
                                .sort(ImmutableList.of(
                                        Sort.create(Message.FIELD_TIMESTAMP, SortOrder.DESC),
                                        Sort.create(Message.FIELD_SOURCE, SortOrder.ASC)))
                                .build()
                        )
                )
                .build();

        LOG.warn("Query {}", query);

        final QueryJob job = new QueryJob(UUID.randomUUID().toString(), query);

        final CompletableFuture<QueryResult> queryJob = new QueryEngine(queryGenerators).execute(job);

        queryJob.thenAccept(o -> LOG.warn("result {}", o)).get();
    }

}