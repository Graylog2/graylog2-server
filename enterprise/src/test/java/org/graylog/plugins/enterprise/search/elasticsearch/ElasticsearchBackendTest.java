package org.graylog.plugins.enterprise.search.elasticsearch;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import org.graylog.plugins.enterprise.search.Parameter;
import org.graylog.plugins.enterprise.search.Query;
import org.graylog.plugins.enterprise.search.QueryMetadata;
import org.graylog.plugins.enterprise.search.Search;
import org.graylog.plugins.enterprise.search.SearchJob;
import org.graylog.plugins.enterprise.search.SearchType;
import org.graylog.plugins.enterprise.search.elasticsearch.searchtypes.ESDateHistogram;
import org.graylog.plugins.enterprise.search.elasticsearch.searchtypes.ESMessageList;
import org.graylog.plugins.enterprise.search.elasticsearch.searchtypes.ESSearchTypeHandler;
import org.graylog.plugins.enterprise.search.errors.SearchException;
import org.graylog.plugins.enterprise.search.filter.AndFilter;
import org.graylog.plugins.enterprise.search.filter.QueryStringFilter;
import org.graylog.plugins.enterprise.search.params.QueryReferenceBinding;
import org.graylog.plugins.enterprise.search.params.ValueBinding;
import org.graylog.plugins.enterprise.search.searchtypes.DateHistogram;
import org.graylog.plugins.enterprise.search.searchtypes.MessageList;
import org.graylog.plugins.enterprise.search.searchtypes.pivot.Pivot;
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
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;

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
        backend = new ElasticsearchBackend(handlers, bindingHandlers, queryStringParser, null, mock(IndexRangeService.class), mock(StreamService.class));
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
        } catch (SearchException e) {
            assertThat(e).hasMessageContaining("TESTPARAM");
        }
    }

    @Test
    public void unboundParameterInWidgetFilter() throws Exception {
        try {
            final Query query = Query.builder()
                    .id("query1")
                    .timerange(RelativeRange.create(600))
                    .query(ElasticsearchQueryString.builder().queryString("*").build())
                    .searchTypes(ImmutableSet.of(
                            MessageList.builder()
                                    .id("1")
                                    .filter(QueryStringFilter.builder().query("source:$bar$").build())
                                    .build()
                    ))
                    .build();
            final Search search = Search.builder()
                    .id("search1")
                    .queries(ImmutableSet.of(query))
                    .build();
            final SearchJob job = new SearchJob("job1", search);

            backend.generate(job, query, Collections.emptySet());
            fail("Must throw exception");
        } catch (SearchException e) {
            assertThat(e).hasMessageContaining("bar");
        }
    }
}