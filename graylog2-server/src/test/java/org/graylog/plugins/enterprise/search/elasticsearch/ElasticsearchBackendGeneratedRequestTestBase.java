package org.graylog.plugins.enterprise.search.elasticsearch;

import com.google.common.collect.ImmutableSet;
import io.searchbox.client.http.JestHttpClient;
import io.searchbox.core.MultiSearch;
import io.searchbox.core.search.aggregation.Aggregation;
import org.graylog.plugins.views.search.Query;
import org.graylog.plugins.views.search.QueryResult;
import org.graylog.plugins.views.search.Search;
import org.graylog.plugins.views.search.SearchJob;
import org.graylog.plugins.views.search.SearchType;
import org.graylog.plugins.views.search.elasticsearch.ESGeneratedQueryContext;
import org.graylog.plugins.views.search.elasticsearch.ESQueryDecorators;
import org.graylog.plugins.views.search.elasticsearch.QueryStringParser;
import org.graylog.plugins.views.search.elasticsearch.searchtypes.ESSearchTypeHandler;
import org.graylog.plugins.views.search.elasticsearch.searchtypes.pivot.ESPivot;
import org.graylog.plugins.views.search.elasticsearch.searchtypes.pivot.ESPivotBucketSpecHandler;
import org.graylog.plugins.views.search.elasticsearch.searchtypes.pivot.ESPivotSeriesSpecHandler;
import org.graylog.plugins.views.search.elasticsearch.searchtypes.pivot.series.ESAverageHandler;
import org.graylog.plugins.views.search.elasticsearch.searchtypes.pivot.series.ESMaxHandler;
import org.graylog.plugins.views.search.searchtypes.pivot.BucketSpec;
import org.graylog.plugins.views.search.searchtypes.pivot.Pivot;
import org.graylog.plugins.views.search.searchtypes.pivot.SeriesSpec;
import org.graylog.plugins.views.search.searchtypes.pivot.series.Average;
import org.graylog.plugins.views.search.searchtypes.pivot.series.Max;
import org.graylog2.indexer.ranges.IndexRangeService;
import org.graylog2.plugin.indexer.searches.timeranges.AbsoluteRange;
import org.graylog2.plugin.indexer.searches.timeranges.InvalidRangeParametersException;
import org.graylog2.plugin.indexer.searches.timeranges.TimeRange;
import org.graylog2.streams.StreamService;
import org.junit.Before;
import org.junit.Rule;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import javax.inject.Provider;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class ElasticsearchBackendGeneratedRequestTestBase extends ElasticsearchBackendTestBase {
    protected static final QueryStringParser queryStringParser = new QueryStringParser();

    @Rule
    public MockitoRule rule = MockitoJUnit.rule();

    ElasticsearchBackend elasticsearchBackend;

    @Mock
    protected JestHttpClient jestClient;
    @Mock
    protected IndexRangeService indexRangeService;
    @Mock
    protected StreamService streamService;

    @Captor
    protected ArgumentCaptor<MultiSearch> clientRequestCaptor;

    @Before
    public void setUpSUT() {
        Map<String, Provider<ESSearchTypeHandler<? extends SearchType>>> elasticSearchTypeHandlers = new HashMap<>();
        final Map<String, ESPivotBucketSpecHandler<? extends BucketSpec, ? extends Aggregation>> bucketHandlers = Collections.emptyMap();
        final Map<String, ESPivotSeriesSpecHandler<? extends SeriesSpec, ? extends Aggregation>> seriesHandlers = new HashMap<>();
        seriesHandlers.put(Average.NAME, new ESAverageHandler());
        seriesHandlers.put(Max.NAME, new ESMaxHandler());
        elasticSearchTypeHandlers.put(Pivot.NAME, () -> new ESPivot(bucketHandlers, seriesHandlers));

        this.elasticsearchBackend = new ElasticsearchBackend(elasticSearchTypeHandlers, queryStringParser, jestClient, indexRangeService, streamService, new ESQueryDecorators.Fake());
    }

    SearchJob searchJobForQuery(Query query) {
        final Search search = Search.builder()
                .id("search1")
                .queries(ImmutableSet.of(query))
                .build();
        return new SearchJob("job1", search, "admin");
    }

    TimeRange timeRangeForTest() {
        try {
            return AbsoluteRange.create("2018-08-23 10:02:00.247", "2018-08-23 10:07:00.252");
        } catch (InvalidRangeParametersException ignored) {
        }
        return null;
    }

    String run(SearchJob searchJob, Query query, ESGeneratedQueryContext queryContext, Set<QueryResult> predecessorResults) throws IOException {
        this.elasticsearchBackend.doRun(searchJob, query, queryContext, predecessorResults);

        verify(jestClient, times(1)).execute(clientRequestCaptor.capture(), any());

        final MultiSearch generatedSearch = clientRequestCaptor.getValue();
        return generatedSearch.getData(objectMapperProvider.get());
    }
}
