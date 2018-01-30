package org.graylog.plugins.enterprise.search.elasticsearch;

import com.google.common.collect.Maps;
import org.graylog.plugins.enterprise.search.Query;
import org.graylog.plugins.enterprise.search.QueryInfo;
import org.graylog.plugins.enterprise.search.QueryParameter;
import org.graylog.plugins.enterprise.search.SearchType;
import org.graylog.plugins.enterprise.search.elasticsearch.searchtypes.ESDateHistogram;
import org.graylog.plugins.enterprise.search.elasticsearch.searchtypes.ESMessageList;
import org.graylog.plugins.enterprise.search.elasticsearch.searchtypes.ESSearchTypeHandler;
import org.graylog.plugins.enterprise.search.searchtypes.DateHistogram;
import org.graylog.plugins.enterprise.search.searchtypes.MessageList;
import org.graylog2.plugin.indexer.searches.timeranges.RelativeRange;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class ElasticsearchBackendTest {

    private static ElasticsearchBackend backend;

    @BeforeClass
    public static void setup() {
        Map<String, ESSearchTypeHandler<? extends SearchType>> handlers = Maps.newHashMap();
        handlers.put(MessageList.NAME, new ESMessageList());
        handlers.put(DateHistogram.NAME, new ESDateHistogram());

        backend = new ElasticsearchBackend(handlers, new ElasticsearchQueryGenerator(handlers), null);
    }

    @Test
    public void parse() throws Exception {
        final QueryInfo queryInfo = backend.parse(Query.builder()
                .id("abc123")
                .query(ElasticsearchQueryString.builder().queryString("user_name:$username$ http_method:$foo$").build())
                .timerange(RelativeRange.create(600))
                .build());

        assertThat(queryInfo.parameters())
                .containsOnly(
                        Maps.immutableEntry("username", QueryParameter.any("username")),
                        Maps.immutableEntry("foo", QueryParameter.any("foo")));
    }

}