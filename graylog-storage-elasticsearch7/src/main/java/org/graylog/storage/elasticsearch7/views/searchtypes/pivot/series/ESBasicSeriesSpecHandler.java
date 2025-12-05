package org.graylog.storage.elasticsearch7.views.searchtypes.pivot.series;

import org.graylog.plugins.views.search.searchtypes.pivot.Pivot;
import org.graylog.plugins.views.search.searchtypes.pivot.SeriesSpec;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.action.search.SearchResponse;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.search.aggregations.Aggregation;
import org.graylog.storage.elasticsearch7.views.ESGeneratedQueryContext;
import org.graylog.storage.elasticsearch7.views.searchtypes.pivot.ESPivotSeriesSpecHandler;
import org.graylog.storage.elasticsearch7.views.searchtypes.pivot.SeriesAggregationBuilder;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.stream.Stream;

public abstract class ESBasicSeriesSpecHandler<SPEC_TYPE extends SeriesSpec, AGGREGATION_RESULT extends Aggregation>
        extends ESPivotSeriesSpecHandler<SPEC_TYPE, AGGREGATION_RESULT> {

    @Override
    public @NotNull List<SeriesAggregationBuilder> doCreateAggregation(String name,
                                                                       Pivot pivot,
                                                                       SPEC_TYPE seriesSpec,
                                                                       ESGeneratedQueryContext queryContext) {
        queryContext.recordNameForPivotSpec(pivot, seriesSpec, name);
        return List.of(createAggregationBuilder(name, seriesSpec));
    }

    protected abstract SeriesAggregationBuilder createAggregationBuilder(final String name, final SPEC_TYPE seriesSpec);

    @Override
    public Stream<Value> doHandleResult(Pivot pivot,
                                        SPEC_TYPE seriesSpec,
                                        SearchResponse searchResult,
                                        AGGREGATION_RESULT aggregationResult,
                                        ESGeneratedQueryContext queryContext) {

        return Stream.of(Value.create(
                seriesSpec.id(),
                seriesSpec.type(),
                getValueFromAggregationResult(aggregationResult, seriesSpec))
        );
    }

    protected abstract Object getValueFromAggregationResult(final AGGREGATION_RESULT aggregationResult, final SPEC_TYPE seriesSpec);
}
