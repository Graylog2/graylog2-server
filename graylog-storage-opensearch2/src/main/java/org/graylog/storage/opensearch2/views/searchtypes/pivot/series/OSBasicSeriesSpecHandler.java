package org.graylog.storage.opensearch2.views.searchtypes.pivot.series;

import org.graylog.plugins.views.search.searchtypes.pivot.Pivot;
import org.graylog.plugins.views.search.searchtypes.pivot.SeriesSpec;
import org.graylog.plugins.views.search.searchtypes.pivot.SeriesSpecHandler;
import org.graylog.shaded.opensearch2.org.opensearch.action.search.SearchResponse;
import org.graylog.shaded.opensearch2.org.opensearch.search.aggregations.Aggregation;
import org.graylog.storage.opensearch2.views.OSGeneratedQueryContext;
import org.graylog.storage.opensearch2.views.searchtypes.pivot.OSPivotSeriesSpecHandler;
import org.graylog.storage.opensearch2.views.searchtypes.pivot.SeriesAggregationBuilder;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.stream.Stream;

public abstract class OSBasicSeriesSpecHandler<SPEC_TYPE extends SeriesSpec, AGGREGATION_RESULT extends Aggregation>
        extends OSPivotSeriesSpecHandler<SPEC_TYPE, AGGREGATION_RESULT> {

    @Override
    public @NotNull List<SeriesAggregationBuilder> doCreateAggregation(String name,
                                                                       Pivot pivot,
                                                                       SPEC_TYPE seriesSpec,
                                                                       OSGeneratedQueryContext queryContext) {
        queryContext.recordNameForPivotSpec(pivot, seriesSpec, name);
        return List.of(createAggregationBuilder(name, seriesSpec));
    }

    protected abstract SeriesAggregationBuilder createAggregationBuilder(final String name, final SPEC_TYPE seriesSpec);

    @Override
    public Stream<SeriesSpecHandler.Value> doHandleResult(Pivot pivot,
                                                          SPEC_TYPE seriesSpec,
                                                          SearchResponse searchResult,
                                                          AGGREGATION_RESULT aggregationResult,
                                                          OSGeneratedQueryContext queryContext) {

        return Stream.of(SeriesSpecHandler.Value.create(
                seriesSpec.id(),
                seriesSpec.type(),
                getValueFromAggregationResult(aggregationResult, seriesSpec))
        );
    }

    protected abstract Object getValueFromAggregationResult(final AGGREGATION_RESULT aggregationResult, final SPEC_TYPE seriesSpec);
}
