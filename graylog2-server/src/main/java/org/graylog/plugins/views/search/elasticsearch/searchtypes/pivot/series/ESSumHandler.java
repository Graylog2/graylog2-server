package org.graylog.plugins.views.search.elasticsearch.searchtypes.pivot.series;

import io.searchbox.core.SearchResult;
import io.searchbox.core.search.aggregation.SumAggregation;
import org.elasticsearch.search.aggregations.AggregationBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.metrics.sum.SumAggregationBuilder;
import org.graylog.plugins.views.search.elasticsearch.ESGeneratedQueryContext;
import org.graylog.plugins.views.search.elasticsearch.searchtypes.pivot.ESPivot;
import org.graylog.plugins.views.search.elasticsearch.searchtypes.pivot.ESPivotSeriesSpecHandler;
import org.graylog.plugins.views.search.searchtypes.pivot.Pivot;
import org.graylog.plugins.views.search.searchtypes.pivot.series.Sum;
import org.graylog.plugins.views.search.elasticsearch.searchtypes.pivot.ESPivot;
import org.graylog.plugins.views.search.elasticsearch.searchtypes.pivot.ESPivotSeriesSpecHandler;

import javax.annotation.Nonnull;
import java.util.Optional;
import java.util.stream.Stream;

public class ESSumHandler extends ESPivotSeriesSpecHandler<Sum, SumAggregation> {
    @Nonnull
    @Override
    public Optional<AggregationBuilder> doCreateAggregation(String name, Pivot pivot, Sum sumSpec, ESPivot searchTypeHandler, ESGeneratedQueryContext queryContext) {
        final SumAggregationBuilder sum = AggregationBuilders.sum(name).field(sumSpec.field());
        record(queryContext, pivot, sumSpec, name, SumAggregation.class);
        return Optional.of(sum);
    }

    @Override
    public Stream<Value> doHandleResult(Pivot pivot, Sum pivotSpec,
                                        SearchResult searchResult,
                                        SumAggregation sumAggregation,
                                        ESPivot searchTypeHandler,
                                        ESGeneratedQueryContext esGeneratedQueryContext) {
        return Stream.of(Value.create(pivotSpec.id(), Sum.NAME, sumAggregation.getSum()));
    }
}
