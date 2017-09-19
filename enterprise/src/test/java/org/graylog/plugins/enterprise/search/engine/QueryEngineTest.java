package org.graylog.plugins.enterprise.search.engine;

import com.google.common.collect.ImmutableList;
import org.elasticsearch.search.sort.SortOrder;
import org.graylog.plugins.enterprise.search.Query;
import org.graylog.plugins.enterprise.search.elasticsearch.ElasticsearchQuery;
import org.graylog.plugins.enterprise.search.filter.StreamFilter;
import org.graylog.plugins.enterprise.search.searchtypes.MessageList;
import org.graylog.plugins.enterprise.search.searchtypes.Sort;
import org.graylog2.plugin.Message;
import org.graylog2.plugin.indexer.searches.timeranges.RelativeRange;
import org.graylog2.plugin.streams.Stream;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;

public class QueryEngineTest {
    private static final Logger LOG = LoggerFactory.getLogger(QueryEngineTest.class);

    @Test
    public void test() {

        final Query query = Query.builder()
                .query(ElasticsearchQuery.builder().queryString("_exists_:message").build())
                .timerange(RelativeRange.builder().type(RelativeRange.RELATIVE).range(600).build())
                .filters(Collections.singletonList(StreamFilter.builder().streamId(Stream.DEFAULT_STREAM_ID).build()))
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
    }

}