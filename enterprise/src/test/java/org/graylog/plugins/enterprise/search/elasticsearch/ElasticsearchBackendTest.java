package org.graylog.plugins.enterprise.search.elasticsearch;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import org.graylog.plugins.enterprise.search.Parameter;
import org.graylog.plugins.enterprise.search.Query;
import org.graylog.plugins.enterprise.search.Search;
import org.graylog.plugins.enterprise.search.SearchJob;
import org.graylog.plugins.enterprise.search.QueryMetadata;
import org.graylog.plugins.enterprise.search.SearchType;
import org.graylog.plugins.enterprise.search.elasticsearch.searchtypes.ESDateHistogram;
import org.graylog.plugins.enterprise.search.elasticsearch.searchtypes.ESMessageList;
import org.graylog.plugins.enterprise.search.elasticsearch.searchtypes.ESSearchTypeHandler;
import org.graylog.plugins.enterprise.search.params.QueryReferenceBinding;
import org.graylog.plugins.enterprise.search.params.ValueBinding;
import org.graylog.plugins.enterprise.search.searchtypes.DateHistogram;
import org.graylog.plugins.enterprise.search.searchtypes.MessageList;
import org.graylog2.plugin.indexer.searches.timeranges.RelativeRange;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.inject.Provider;
import java.util.Collections;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

public class ElasticsearchBackendTest {

    private static ElasticsearchBackend backend;

    @BeforeClass
    public static void setup() {
        Map<String, Provider<ESSearchTypeHandler<? extends SearchType>>> handlers = Maps.newHashMap();
        handlers.put(MessageList.NAME, ESMessageList::new);
        handlers.put(DateHistogram.NAME, ESDateHistogram::new);
        Map<String, Provider<Parameter.BindingHandler>> bindingHandlers = Maps.newHashMap();
        bindingHandlers.put(ValueBinding.NAME, ValueBinding.Handler::new);
        bindingHandlers.put(QueryReferenceBinding.NAME, () -> new QueryReferenceBinding.Handler(new ObjectMapper()));

        final QueryStringParser queryStringParser = new QueryStringParser();
        backend = new ElasticsearchBackend(handlers, bindingHandlers, queryStringParser, null);
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
    public void unboundParameter() throws Exception {
        try {
            final Query query = Query.builder()
                    .id("query1")
                    .timerange(RelativeRange.create(600))
                    .query(ElasticsearchQueryString.builder().queryString("_exists_:$TESTPARAM$").build())
                    .searchTypes(ImmutableSet.of(MessageList.builder().id("1").build()))
                    .build();
            final Search search = Search.builder()
                    .id("search1")
                    .queries(ImmutableSet.of(query))
                    .build();
            final SearchJob job = new SearchJob("job1", search);

            backend.generate(job, query, Collections.emptySet());
            fail("Must throw exception");
        } catch (IllegalStateException e) {
            assertThat(e).hasMessageContaining("TESTPARAM");
        }
    }
}