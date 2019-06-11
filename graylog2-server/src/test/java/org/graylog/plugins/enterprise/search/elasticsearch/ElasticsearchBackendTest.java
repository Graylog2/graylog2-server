package org.graylog.plugins.enterprise.search.elasticsearch;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import org.graylog.plugins.views.search.Query;
import org.graylog.plugins.views.search.QueryMetadata;
import org.graylog.plugins.views.search.SearchType;
import org.graylog.plugins.views.search.elasticsearch.ESQueryDecorators;
import org.graylog.plugins.views.search.elasticsearch.ElasticsearchQueryString;
import org.graylog.plugins.views.search.elasticsearch.QueryStringParser;
import org.graylog.plugins.views.search.elasticsearch.searchtypes.ESDateHistogram;
import org.graylog.plugins.views.search.elasticsearch.searchtypes.ESMessageList;
import org.graylog.plugins.views.search.elasticsearch.searchtypes.ESSearchTypeHandler;
import org.graylog.plugins.views.search.filter.AndFilter;
import org.graylog.plugins.views.search.filter.QueryStringFilter;
import org.graylog.plugins.views.search.searchtypes.DateHistogram;
import org.graylog.plugins.views.search.searchtypes.MessageList;
import org.graylog.plugins.views.search.searchtypes.pivot.Pivot;
import org.graylog2.indexer.ranges.IndexRangeService;
import org.graylog2.plugin.indexer.searches.timeranges.RelativeRange;
import org.graylog2.streams.StreamService;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.inject.Provider;
import java.util.ArrayList;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

public class ElasticsearchBackendTest {

    private static ElasticsearchBackend backend;

    @BeforeClass
    public static void setup() {
        Map<String, Provider<ESSearchTypeHandler<? extends SearchType>>> handlers = Maps.newHashMap();
        handlers.put(MessageList.NAME, () -> new ESMessageList(new ESQueryDecorators.Fake()));
        handlers.put(DateHistogram.NAME, ESDateHistogram::new);

        final QueryStringParser queryStringParser = new QueryStringParser();
        backend = new ElasticsearchBackend(handlers, queryStringParser, null, mock(IndexRangeService.class), mock(StreamService.class), new ESQueryDecorators.Fake());
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
}
