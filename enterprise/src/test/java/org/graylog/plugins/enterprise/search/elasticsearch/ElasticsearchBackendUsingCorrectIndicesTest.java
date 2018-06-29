package org.graylog.plugins.enterprise.search.elasticsearch;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import io.searchbox.client.http.JestHttpClient;
import io.searchbox.core.SearchResult;
import org.graylog.plugins.enterprise.search.Parameter;
import org.graylog.plugins.enterprise.search.Query;
import org.graylog.plugins.enterprise.search.Search;
import org.graylog.plugins.enterprise.search.SearchJob;
import org.graylog.plugins.enterprise.search.SearchType;
import org.graylog.plugins.enterprise.search.elasticsearch.searchtypes.ESDateHistogram;
import org.graylog.plugins.enterprise.search.elasticsearch.searchtypes.ESMessageList;
import org.graylog.plugins.enterprise.search.elasticsearch.searchtypes.ESSearchTypeHandler;
import org.graylog.plugins.enterprise.search.params.QueryReferenceBinding;
import org.graylog.plugins.enterprise.search.params.ValueBinding;
import org.graylog.plugins.enterprise.search.searchtypes.DateHistogram;
import org.graylog.plugins.enterprise.search.searchtypes.MessageList;
import org.graylog2.indexer.ranges.IndexRange;
import org.graylog2.indexer.ranges.IndexRangeService;
import org.graylog2.plugin.indexer.searches.timeranges.RelativeRange;
import org.joda.time.DateTime;
import org.joda.time.DateTimeUtils;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import javax.inject.Provider;
import java.util.Collections;
import java.util.Comparator;
import java.util.Map;
import java.util.TreeSet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ElasticsearchBackendUsingCorrectIndicesTest {
    private static Map<String, Provider<ESSearchTypeHandler<? extends SearchType>>> handlers;
    private static Map<String, Provider<Parameter.BindingHandler>> bindingHandlers;
    private static final QueryStringParser queryStringParser = new QueryStringParser();

    @Captor
    private ArgumentCaptor<io.searchbox.core.Search> clientRequestCaptor;

    @Mock
    private IndexRangeService indexRangeService;

    @Mock
    private JestHttpClient jestClient;

    @Mock
    private SearchResult jestResult;

    private ElasticsearchBackend backend;

    private SearchJob job;
    private Query query;

    @BeforeClass
    public static void setup() {
        handlers = Maps.newHashMap();
        handlers.put(MessageList.NAME, ESMessageList::new);
        handlers.put(DateHistogram.NAME, ESDateHistogram::new);

        bindingHandlers = Maps.newHashMap();
        bindingHandlers.put(ValueBinding.NAME, ValueBinding.Handler::new);
        bindingHandlers.put(QueryReferenceBinding.NAME, () -> new QueryReferenceBinding.Handler(new ObjectMapper()));
    }

    @Before
    public void before() throws Exception {
        MockitoAnnotations.initMocks(this);

        when(jestClient.execute(any(), any())).thenReturn(jestResult);
        when(jestResult.isSucceeded()).thenReturn(true);

        this.backend = new ElasticsearchBackend(handlers, bindingHandlers, queryStringParser, jestClient, indexRangeService);

        this.query = Query.builder()
                .id("query1")
                .timerange(RelativeRange.create(600))
                .query(ElasticsearchQueryString.builder().queryString("*").build())
                .searchTypes(ImmutableSet.of(MessageList.builder().id("1").build()))
                .build();
        final Search search = Search.builder()
                .id("search1")
                .queries(ImmutableSet.of(query))
                .build();
        this.job = new SearchJob("job1", search);
    }

    @Test
    public void queryFallsBackToUsingAllIndicesWhenNoIndexRangesAreReturned() throws Exception {
        when(indexRangeService.find(any(DateTime.class), any(DateTime.class))).thenReturn(new TreeSet<>());

        final ESGeneratedQueryContext context = backend.generate(job, query, Collections.emptySet());
        backend.doRun(job, query, context, Collections.emptySet());

        verify(jestClient, times(1)).execute(clientRequestCaptor.capture(), any());

        final io.searchbox.core.Search clientRequest = clientRequestCaptor.getValue();
        assertThat(clientRequest).isNotNull();
        assertThat(clientRequest.getIndex()).isEqualTo("_all");
    }

    @Test
    public void queryUsesCorrectTimerangeWhenDeterminingIndexRanges() throws Exception {
        when(indexRangeService.find(any(DateTime.class), any(DateTime.class))).thenReturn(new TreeSet<>());

        final long datetimeFixture = 1530194810;
        DateTimeUtils.setCurrentMillisFixed(datetimeFixture);

        final ESGeneratedQueryContext context = backend.generate(job, query, Collections.emptySet());
        backend.doRun(job, query, context, Collections.emptySet());

        final ArgumentCaptor<DateTime> fromCapture = ArgumentCaptor.forClass(DateTime.class);
        final ArgumentCaptor<DateTime> toCapture = ArgumentCaptor.forClass(DateTime.class);
        verify(indexRangeService, times(1)).find(fromCapture.capture(), toCapture.capture());

        assertThat(fromCapture.getValue().isEqual(new DateTime(datetimeFixture).minusSeconds(600))).isTrue();
        assertThat(toCapture.getValue().isEqual(new DateTime(datetimeFixture))).isTrue();
    }

    @Test
    public void queryUsesOnlyIndicesIncludingTimerange() throws Exception {
        final IndexRange indexRange1 = mock(IndexRange.class);
        when(indexRange1.indexName()).thenReturn("index1");
        final IndexRange indexRange2 = mock(IndexRange.class);
        when(indexRange2.indexName()).thenReturn("index2");

        final Comparator<IndexRange> indexRangeComparator = Comparator.comparing(IndexRange::indexName);

        final TreeSet<IndexRange> indexRangeSets = new TreeSet<>(indexRangeComparator);
        indexRangeSets.add(indexRange1);
        indexRangeSets.add(indexRange2);

        when(indexRangeService.find(any(DateTime.class), any(DateTime.class))).thenReturn(indexRangeSets);

        final Query query = Query.builder()
                .id("query1")
                .timerange(RelativeRange.create(600))
                .query(ElasticsearchQueryString.builder().queryString("*").build())
                .searchTypes(ImmutableSet.of(MessageList.builder().id("1").build()))
                .build();
        final Search search = Search.builder()
                .id("search1")
                .queries(ImmutableSet.of(query))
                .build();
        final SearchJob job = new SearchJob("job1", search);
        final ESGeneratedQueryContext context = backend.generate(job, query, Collections.emptySet());
        backend.doRun(job, query, context, Collections.emptySet());

        verify(jestClient, times(1)).execute(clientRequestCaptor.capture(), any());

        final io.searchbox.core.Search clientRequest = clientRequestCaptor.getValue();
        assertThat(clientRequest).isNotNull();
        assertThat(clientRequest.getIndex()).isEqualTo("index1,index2");
    }
}