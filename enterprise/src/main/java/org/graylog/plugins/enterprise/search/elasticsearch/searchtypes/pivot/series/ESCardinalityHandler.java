package org.graylog.plugins.enterprise.search.elasticsearch.searchtypes.pivot.series;

import io.searchbox.core.SearchResult;
import io.searchbox.core.search.aggregation.CardinalityAggregation;
import org.elasticsearch.search.aggregations.AggregationBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.metrics.cardinality.CardinalityAggregationBuilder;
import org.graylog.plugins.enterprise.search.elasticsearch.ESGeneratedQueryContext;
import org.graylog.plugins.enterprise.search.elasticsearch.searchtypes.pivot.ESPivot;
import org.graylog.plugins.enterprise.search.elasticsearch.searchtypes.pivot.ESPivotSeriesSpecHandler;
import org.graylog.plugins.enterprise.search.searchtypes.pivot.Pivot;
import org.graylog.plugins.enterprise.search.searchtypes.pivot.series.Cardinality;

import javax.annotation.Nonnull;
import java.util.Optional;
import java.util.stream.Stream;

public class ESCardinalityHandler extends ESPivotSeriesSpecHandler<Cardinality, CardinalityAggregation> {
    @Nonnull
    @Override
    public Optional<AggregationBuilder> doCreateAggregation(String name, Pivot pivot, Cardinality cardinalitySpec, ESPivot searchTypeHandler, ESGeneratedQueryContext queryContext) {
        final CardinalityAggregationBuilder card = AggregationBuilders.cardinality(name).field(cardinalitySpec.field());
        record(queryContext, pivot, cardinalitySpec, name, CardinalityAggregation.class);
        return Optional.of(card);
    }

    @Override
    public Stream<Value> doHandleResult(Pivot pivot, Cardinality pivotSpec,
                                        SearchResult searchResult,
                                        CardinalityAggregation cardinalityAggregation,
                                        ESPivot searchTypeHandler,
                                        ESGeneratedQueryContext esGeneratedQueryContext) {
        return Stream.of(ESPivotSeriesSpecHandler.Value.create(pivotSpec.id(), Cardinality.NAME, cardinalityAggregation.getCardinality()));
    }
}
