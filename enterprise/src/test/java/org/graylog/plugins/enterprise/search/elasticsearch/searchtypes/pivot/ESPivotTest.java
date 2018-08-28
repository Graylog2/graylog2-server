package org.graylog.plugins.enterprise.search.elasticsearch.searchtypes.pivot;

import io.searchbox.core.SearchResult;
import io.searchbox.core.search.aggregation.Aggregation;
import io.searchbox.core.search.aggregation.FilterAggregation;
import io.searchbox.core.search.aggregation.MetricAggregation;
import org.graylog.plugins.enterprise.search.Filter;
import org.graylog.plugins.enterprise.search.Query;
import org.graylog.plugins.enterprise.search.SearchJob;
import org.graylog.plugins.enterprise.search.SearchType;
import org.graylog.plugins.enterprise.search.elasticsearch.ESGeneratedQueryContext;
import org.graylog.plugins.enterprise.search.searchtypes.pivot.BucketSpec;
import org.graylog.plugins.enterprise.search.searchtypes.pivot.Pivot;
import org.graylog.plugins.enterprise.search.searchtypes.pivot.PivotResult;
import org.graylog.plugins.enterprise.search.searchtypes.pivot.SeriesSpec;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ESPivotTest {
    @Rule
    public final MockitoRule mockitoRule = MockitoJUnit.rule();
    @Mock
    private SearchJob job;
    @Mock
    private Query query;
    @Mock
    private Pivot pivot;
    @Mock
    private SearchResult queryResult;
    @Mock
    private MetricAggregation aggregations;
    @Mock
    private ESGeneratedQueryContext queryContext;

    private ESPivot esPivot;
    private Map<String, ESPivotBucketSpecHandler<? extends BucketSpec, ? extends Aggregation>> bucketHandlers;
    private Map<String, ESPivotSeriesSpecHandler<? extends SeriesSpec, ? extends Aggregation>> seriesHandlers;

    @Before
    public void setUp() throws Exception {
        this.esPivot = new ESPivot(bucketHandlers, seriesHandlers);
        when(pivot.id()).thenReturn("dummypivot");
    }

    @Test
    public void searchResultIncludesDocumentCount() {
        final long documentCount = 424242;
        when(queryResult.getTotal()).thenReturn(documentCount);

        final SearchType.Result result = this.esPivot.doExtractResult(job, query, pivot, queryResult, aggregations, queryContext);

        final PivotResult pivotResult = (PivotResult)result;

        assertThat(pivotResult.total()).isEqualTo(documentCount);
    }

    @Test
    public void searchResultWithFilterIncludesDocumentCountFromFilteredAggregation() {
        final long documentCount = 2323;

        when(queryContext.filterName(pivot)).thenReturn("filtered-dummypivot");
        when(pivot.filter()).thenReturn(mock(Filter.class));
        final MetricAggregation metricAggregation = mock(MetricAggregation.class);
        when(queryResult.getAggregations()).thenReturn(metricAggregation);
        final FilterAggregation filterAggregation = mock(FilterAggregation.class);
        when(metricAggregation.getFilterAggregation("filtered-dummypivot")).thenReturn(filterAggregation);
        when(filterAggregation.getCount()).thenReturn(documentCount);

        final SearchType.Result result = this.esPivot.doExtractResult(job, query, pivot, queryResult, aggregations, queryContext);

        final PivotResult pivotResult = (PivotResult)result;

        assertThat(pivotResult.total()).isEqualTo(documentCount);
        verify(queryContext, times(1)).filterName(pivot);
    }
}