package org.graylog.plugins.views.search.elasticsearch.searchtypes.pivot.series;

import io.searchbox.core.SearchResult;
import io.searchbox.core.search.aggregation.AvgAggregation;
import org.elasticsearch.search.aggregations.AggregationBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.metrics.avg.AvgAggregationBuilder;
import org.graylog.plugins.views.search.elasticsearch.ESGeneratedQueryContext;
import org.graylog.plugins.views.search.elasticsearch.searchtypes.pivot.ESPivot;
import org.graylog.plugins.views.search.elasticsearch.searchtypes.pivot.ESPivotSeriesSpecHandler;
import org.graylog.plugins.views.search.searchtypes.pivot.Pivot;
import org.graylog.plugins.views.search.searchtypes.pivot.series.Average;
import org.graylog.plugins.views.search.elasticsearch.searchtypes.pivot.ESPivot;
import org.graylog.plugins.views.search.elasticsearch.searchtypes.pivot.ESPivotSeriesSpecHandler;

import javax.annotation.Nonnull;
import java.util.Optional;
import java.util.stream.Stream;

public class ESAverageHandler extends ESPivotSeriesSpecHandler<Average, AvgAggregation> {
    @Nonnull
    @Override
    public Optional<AggregationBuilder> doCreateAggregation(String name, Pivot pivot, Average avgSpec, ESPivot searchTypeHandler, ESGeneratedQueryContext queryContext) {
        final AvgAggregationBuilder avg = AggregationBuilders.avg(name).field(avgSpec.field());
        record(queryContext, pivot, avgSpec, name, AvgAggregation.class);
        return Optional.of(avg);
    }

    @Override
    public Stream<ESPivotSeriesSpecHandler.Value> doHandleResult(Pivot pivot, Average pivotSpec,
                                                                 SearchResult searchResult,
                                                                 AvgAggregation avgAggregation,
                                                                 ESPivot searchTypeHandler,
                                                                 ESGeneratedQueryContext esGeneratedQueryContext) {
        return Stream.of(ESPivotSeriesSpecHandler.Value.create(pivotSpec.id(), Average.NAME, avgAggregation.getAvg()));
    }
}
