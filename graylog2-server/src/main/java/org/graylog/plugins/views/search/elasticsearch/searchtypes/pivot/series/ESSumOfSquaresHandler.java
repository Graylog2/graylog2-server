package org.graylog.plugins.views.search.elasticsearch.searchtypes.pivot.series;

import io.searchbox.core.SearchResult;
import io.searchbox.core.search.aggregation.ExtendedStatsAggregation;
import org.elasticsearch.search.aggregations.AggregationBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.metrics.stats.extended.ExtendedStatsAggregationBuilder;
import org.graylog.plugins.views.search.elasticsearch.ESGeneratedQueryContext;
import org.graylog.plugins.views.search.elasticsearch.searchtypes.pivot.ESPivot;
import org.graylog.plugins.views.search.elasticsearch.searchtypes.pivot.ESPivotSeriesSpecHandler;
import org.graylog.plugins.views.search.searchtypes.pivot.Pivot;
import org.graylog.plugins.views.search.searchtypes.pivot.series.SumOfSquares;
import org.graylog.plugins.views.search.elasticsearch.searchtypes.pivot.ESPivot;
import org.graylog.plugins.views.search.elasticsearch.searchtypes.pivot.ESPivotSeriesSpecHandler;

import javax.annotation.Nonnull;
import java.util.Optional;
import java.util.stream.Stream;

public class ESSumOfSquaresHandler extends ESPivotSeriesSpecHandler<SumOfSquares, ExtendedStatsAggregation> {
    @Nonnull
    @Override
    public Optional<AggregationBuilder> doCreateAggregation(String name, Pivot pivot, SumOfSquares sumOfSquaresSpec, ESPivot searchTypeHandler, ESGeneratedQueryContext queryContext) {
        final ExtendedStatsAggregationBuilder sumOfSquares = AggregationBuilders.extendedStats(name).field(sumOfSquaresSpec.field());
        record(queryContext, pivot, sumOfSquaresSpec, name, ExtendedStatsAggregation.class);
        return Optional.of(sumOfSquares);
    }

    @Override
    public Stream<Value> doHandleResult(Pivot pivot, SumOfSquares pivotSpec,
                                        SearchResult searchResult,
                                        ExtendedStatsAggregation sumOfSquaresAggregation,
                                        ESPivot searchTypeHandler,
                                        ESGeneratedQueryContext esGeneratedQueryContext) {
        return Stream.of(ESPivotSeriesSpecHandler.Value.create(pivotSpec.id(), SumOfSquares.NAME, sumOfSquaresAggregation.getSumOfSquares()));
    }
}
