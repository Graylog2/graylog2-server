package org.graylog.plugins.views.search.elasticsearch.searchtypes.pivot.series;

import io.searchbox.core.SearchResult;
import io.searchbox.core.search.aggregation.PercentilesAggregation;
import org.elasticsearch.search.aggregations.AggregationBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.metrics.percentiles.PercentilesAggregationBuilder;
import org.graylog.plugins.views.search.elasticsearch.ESGeneratedQueryContext;
import org.graylog.plugins.views.search.elasticsearch.searchtypes.pivot.ESPivot;
import org.graylog.plugins.views.search.elasticsearch.searchtypes.pivot.ESPivotSeriesSpecHandler;
import org.graylog.plugins.views.search.searchtypes.pivot.Pivot;
import org.graylog.plugins.views.search.searchtypes.pivot.series.Percentile;
import org.graylog.plugins.views.search.elasticsearch.searchtypes.pivot.ESPivot;
import org.graylog.plugins.views.search.elasticsearch.searchtypes.pivot.ESPivotSeriesSpecHandler;

import javax.annotation.Nonnull;
import java.util.Optional;
import java.util.stream.Stream;

public class ESPercentilesHandler extends ESPivotSeriesSpecHandler<Percentile, PercentilesAggregation> {
    @Nonnull
    @Override
    public Optional<AggregationBuilder> doCreateAggregation(String name, Pivot pivot, Percentile percentileSpec, ESPivot searchTypeHandler, ESGeneratedQueryContext queryContext) {
        final PercentilesAggregationBuilder percentiles = AggregationBuilders.percentiles(name).field(percentileSpec.field()).percentiles(percentileSpec.percentile());
        record(queryContext, pivot, percentileSpec, name, PercentilesAggregation.class);
        return Optional.of(percentiles);
    }

    @Override
    public Stream<Value> doHandleResult(Pivot pivot, Percentile pivotSpec,
                                        SearchResult searchResult,
                                        PercentilesAggregation percentilesAggregation,
                                        ESPivot searchTypeHandler,
                                        ESGeneratedQueryContext queryContext) {
        Double percentile = percentilesAggregation.getPercentiles().getOrDefault(pivotSpec.percentile().toString(), null);
        return Stream.of(ESPivotSeriesSpecHandler.Value.create(pivotSpec.id(), Percentile.NAME, percentile));
    }
}
