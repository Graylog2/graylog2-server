package org.graylog.plugins.enterprise.search.elasticsearch.searchtypes.pivot.series;

import io.searchbox.core.SearchResult;
import io.searchbox.core.search.aggregation.ExtendedStatsAggregation;
import org.elasticsearch.search.aggregations.AggregationBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.metrics.stats.extended.ExtendedStatsAggregationBuilder;
import org.graylog.plugins.enterprise.search.elasticsearch.ESGeneratedQueryContext;
import org.graylog.plugins.enterprise.search.elasticsearch.searchtypes.pivot.ESPivot;
import org.graylog.plugins.enterprise.search.elasticsearch.searchtypes.pivot.ESPivotSeriesSpecHandler;
import org.graylog.plugins.enterprise.search.searchtypes.pivot.Pivot;
import org.graylog.plugins.enterprise.search.searchtypes.pivot.series.StdDev;

import javax.annotation.Nonnull;
import java.util.Optional;
import java.util.stream.Stream;

public class ESStdDevHandler extends ESPivotSeriesSpecHandler<StdDev, ExtendedStatsAggregation> {
    @Nonnull
    @Override
    public Optional<AggregationBuilder> doCreateAggregation(String name, Pivot pivot, StdDev stddevSpec, ESPivot searchTypeHandler, ESGeneratedQueryContext queryContext) {
        final ExtendedStatsAggregationBuilder stddev = AggregationBuilders.extendedStats(name).field(stddevSpec.field());
        record(queryContext, pivot, stddevSpec, name, ExtendedStatsAggregation.class);
        return Optional.of(stddev);
    }

    @Override
    public Stream<Value> doHandleResult(Pivot pivot, StdDev pivotSpec,
                                        SearchResult searchResult,
                                        ExtendedStatsAggregation stddevAggregation,
                                        ESPivot searchTypeHandler,
                                        ESGeneratedQueryContext esGeneratedQueryContext) {
        return Stream.of(ESPivotSeriesSpecHandler.Value.create(pivotSpec.id(), StdDev.NAME, stddevAggregation.getStdDeviation()));
    }
}
