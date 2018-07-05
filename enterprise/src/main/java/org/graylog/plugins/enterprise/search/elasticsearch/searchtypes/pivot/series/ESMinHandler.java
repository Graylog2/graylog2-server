package org.graylog.plugins.enterprise.search.elasticsearch.searchtypes.pivot.series;

import io.searchbox.core.SearchResult;
import io.searchbox.core.search.aggregation.MinAggregation;
import org.elasticsearch.search.aggregations.AggregationBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.metrics.min.MinAggregationBuilder;
import org.graylog.plugins.enterprise.search.elasticsearch.ESGeneratedQueryContext;
import org.graylog.plugins.enterprise.search.elasticsearch.searchtypes.pivot.ESPivot;
import org.graylog.plugins.enterprise.search.elasticsearch.searchtypes.pivot.ESPivotSeriesSpecHandler;
import org.graylog.plugins.enterprise.search.searchtypes.pivot.Pivot;
import org.graylog.plugins.enterprise.search.searchtypes.pivot.series.Min;

import javax.annotation.Nonnull;
import java.util.Optional;
import java.util.stream.Stream;

public class ESMinHandler extends ESPivotSeriesSpecHandler<Min, MinAggregation> {
    @Nonnull
    @Override
    public Optional<AggregationBuilder> doCreateAggregation(String name, Pivot pivot, Min minSpec, ESPivot searchTypeHandler, ESGeneratedQueryContext queryContext) {
        final MinAggregationBuilder min = AggregationBuilders.min(name).field(minSpec.field());
        record(queryContext, pivot, minSpec, name, MinAggregation.class);
        return Optional.of(min);
    }

    @Override
    public Stream<Value> doHandleResult(Pivot pivot, Min pivotSpec,
                                        SearchResult searchResult,
                                        MinAggregation minAggregation,
                                        ESPivot searchTypeHandler,
                                        ESGeneratedQueryContext esGeneratedQueryContext) {
        return Stream.of(ESPivotSeriesSpecHandler.Value.create(pivotSpec.id(), Min.NAME, minAggregation.getMin()));
    }
}
