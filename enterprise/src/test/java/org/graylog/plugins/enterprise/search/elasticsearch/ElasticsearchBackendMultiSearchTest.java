package org.graylog.plugins.enterprise.search.elasticsearch;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import io.searchbox.client.http.JestHttpClient;
import io.searchbox.core.MultiSearch;
import io.searchbox.core.search.aggregation.Aggregation;
import org.graylog.plugins.enterprise.search.Parameter;
import org.graylog.plugins.enterprise.search.Query;
import org.graylog.plugins.enterprise.search.QueryResult;
import org.graylog.plugins.enterprise.search.Search;
import org.graylog.plugins.enterprise.search.SearchJob;
import org.graylog.plugins.enterprise.search.SearchType;
import org.graylog.plugins.enterprise.search.elasticsearch.searchtypes.ESSearchTypeHandler;
import org.graylog.plugins.enterprise.search.elasticsearch.searchtypes.pivot.ESPivot;
import org.graylog.plugins.enterprise.search.elasticsearch.searchtypes.pivot.ESPivotBucketSpecHandler;
import org.graylog.plugins.enterprise.search.elasticsearch.searchtypes.pivot.ESPivotSeriesSpecHandler;
import org.graylog.plugins.enterprise.search.elasticsearch.searchtypes.pivot.series.ESAverageHandler;
import org.graylog.plugins.enterprise.search.elasticsearch.searchtypes.pivot.series.ESMaxHandler;
import org.graylog.plugins.enterprise.search.errors.SearchTypeError;
import org.graylog.plugins.enterprise.search.searchtypes.pivot.BucketSpec;
import org.graylog.plugins.enterprise.search.searchtypes.pivot.Pivot;
import org.graylog.plugins.enterprise.search.searchtypes.pivot.PivotResult;
import org.graylog.plugins.enterprise.search.searchtypes.pivot.SeriesSpec;
import org.graylog.plugins.enterprise.search.searchtypes.pivot.series.Average;
import org.graylog.plugins.enterprise.search.searchtypes.pivot.series.Max;
import org.graylog2.indexer.ranges.IndexRangeService;
import org.graylog2.plugin.indexer.searches.timeranges.AbsoluteRange;
import org.graylog2.plugin.indexer.searches.timeranges.InvalidRangeParametersException;
import org.graylog2.plugin.indexer.searches.timeranges.TimeRange;
import org.graylog2.streams.StreamService;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import javax.inject.Provider;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ElasticsearchBackendMultiSearchTest extends ElasticsearchBackendTestBase {
    private static final QueryStringParser queryStringParser = new QueryStringParser();

    @Rule
    public MockitoRule rule = MockitoJUnit.rule();

    private ElasticsearchBackend elasticsearchBackend;
    private Map<String, Provider<Parameter.BindingHandler>> bindingHandlers;
    private Map<String, Provider<ESSearchTypeHandler<? extends SearchType>>> elasticSearchTypeHandlers;

    @Mock
    private JestHttpClient jestClient;
    @Mock
    private IndexRangeService indexRangeService;
    @Mock
    private StreamService streamService;

    @Captor
    private ArgumentCaptor<io.searchbox.core.MultiSearch> clientRequestCaptor;

    private SearchJob searchJob;
    private Set<SearchType> searchTypes;
    private Query query;

    @Before
    public void setUpSUT() throws Exception {
        this.bindingHandlers = new HashMap<>();
        this.elasticSearchTypeHandlers = new HashMap<>();
        final Map<String, ESPivotBucketSpecHandler<? extends BucketSpec, ? extends Aggregation>> bucketHandlers = Collections.emptyMap();
        final Map<String, ESPivotSeriesSpecHandler<? extends SeriesSpec, ? extends Aggregation>> seriesHandlers = new HashMap<>();
        seriesHandlers.put(Average.NAME, new ESAverageHandler());
        seriesHandlers.put(Max.NAME, new ESMaxHandler());
        this.elasticSearchTypeHandlers.put(Pivot.NAME, () -> new ESPivot(bucketHandlers, seriesHandlers));

        this.elasticsearchBackend = new ElasticsearchBackend(elasticSearchTypeHandlers, bindingHandlers, queryStringParser, jestClient, indexRangeService, streamService);
    }

    @Before
    public void setUpFixtures() throws Exception {
        this.searchTypes = new HashSet<SearchType>() {{
            add(
                    Pivot.builder()
                            .id("pivot1")
                            .series(Collections.singletonList(Average.builder().field("field1").build()))
                            .rollup(true)
                            .build()
            );
            add(
                    Pivot.builder()
                            .id("pivot2")
                            .series(Collections.singletonList(Max.builder().field("field2").build()))
                            .rollup(true)
                            .build()
            );
        }};
        this.query = Query.builder()
                .id("query1")
                .searchTypes(this.searchTypes)
                .query(ElasticsearchQueryString.builder().queryString("*").build())
                .timerange(timeRangeForTest())
                .build();

        this.searchJob = searchJobForQuery(this.query);
    }

    private SearchJob searchJobForQuery(Query query) {
        final Search search = Search.builder()
                .id("search1")
                .queries(ImmutableSet.of(query))
                .build();
        return new SearchJob("job1", search);
    }

    private TimeRange timeRangeForTest() {
        try {
            return AbsoluteRange.create("2018-08-23 10:02:00.247", "2018-08-23 10:07:00.252");
        } catch (InvalidRangeParametersException ignored) {
        }
        return null;
    }

    @Test
    public void everySearchTypeGeneratesASearchSourceBuilder() throws Exception {
        final ESGeneratedQueryContext queryContext = this.elasticsearchBackend.generate(searchJob, query, Collections.emptySet());

        assertThat(queryContext.searchTypeQueries())
                .hasSize(2)
                .containsOnlyKeys("pivot1", "pivot2");
    }

    @Test
    public void everySearchTypeGeneratesOneESQuery() throws Exception {
        final ESGeneratedQueryContext queryContext = this.elasticsearchBackend.generate(searchJob, query, Collections.emptySet());
        when(jestClient.execute(any(), any())).thenReturn(resultFor(resourceFile("successfulMultiSearchResponse.json")));

        this.elasticsearchBackend.doRun(searchJob, query, queryContext, Collections.emptySet());

        verify(jestClient, times(1)).execute(clientRequestCaptor.capture(), any());

        final MultiSearch generatedSearch = clientRequestCaptor.getValue();
        final String generatedRequest = generatedSearch.getData(objectMapperProvider.get());

        assertThat(generatedRequest).isEqualTo(resourceFile("everySearchTypeGeneratesOneESQuery.request.ndjson"));
    }

    @Test
    public void multiSearchResultsAreAssignedToSearchTypes() throws Exception {
        final ESGeneratedQueryContext queryContext = this.elasticsearchBackend.generate(searchJob, query, Collections.emptySet());

        when(jestClient.execute(any(), any())).thenReturn(resultFor(resourceFile("successfulMultiSearchResponse.json")));

        final QueryResult queryResult = this.elasticsearchBackend.doRun(searchJob, query, queryContext, Collections.emptySet());

        assertThat(queryResult.searchTypes()).containsOnlyKeys("pivot1", "pivot2");

        final PivotResult pivot1Result = (PivotResult) queryResult.searchTypes().get("pivot1");
        assertThat(pivot1Result.rows().get(0)).isEqualTo(
                PivotResult.Row.builder().key(ImmutableList.of()).source("leaf").addValue(
                        PivotResult.Value.create(Collections.singletonList("avg(field1)"), 27220.273504273504, true, "row-leaf")
                ).build()
        );

        final PivotResult pivot2Result = (PivotResult) queryResult.searchTypes().get("pivot2");
        assertThat(pivot2Result.rows().get(0)).isEqualTo(
                PivotResult.Row.builder().key(ImmutableList.of()).source("leaf").addValue(
                        PivotResult.Value.create(Collections.singletonList("max(field2)"), 42.0, true, "row-leaf")
                ).build()
        );
    }

    @Test
    public void oneFailingSearchTypeReturnsPartialResults() throws Exception {
        final ESGeneratedQueryContext queryContext = this.elasticsearchBackend.generate(searchJob, query, Collections.emptySet());

        when(jestClient.execute(any(), any())).thenReturn(resultFor(resourceFile("partiallySuccessfulMultiSearchResponse.json")));

        final QueryResult queryResult = this.elasticsearchBackend.doRun(searchJob, query, queryContext, Collections.emptySet());

        assertThat(queryResult.errors()).hasSize(1);
        final SearchTypeError searchTypeError = (SearchTypeError)new ArrayList<>(queryResult.errors()).get(0);
        assertThat(searchTypeError.description()).isEqualTo(
                "Unable to perform search query\n" +
                        "\n" +
                        "Expected numeric type on field [field1], but got [keyword]."
        );
        assertThat(searchTypeError.searchTypeId()).isEqualTo("pivot1");

        assertThat(queryResult.searchTypes()).containsOnlyKeys("pivot2");

        final PivotResult pivot2Result = (PivotResult) queryResult.searchTypes().get("pivot2");
        assertThat(pivot2Result.rows().get(0)).isEqualTo(
                PivotResult.Row.builder().key(ImmutableList.of()).source("leaf").addValue(
                        PivotResult.Value.create(Collections.singletonList("max(field2)"), 42.0, true, "row-leaf")
                ).build()
        );
    }
}