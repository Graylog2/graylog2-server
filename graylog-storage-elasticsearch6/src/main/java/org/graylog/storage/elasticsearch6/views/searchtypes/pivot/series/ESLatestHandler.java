package org.graylog.storage.elasticsearch6.views.searchtypes.pivot.series;

import io.searchbox.core.SearchResult;
import io.searchbox.core.search.aggregation.MaxAggregation;
import io.searchbox.core.search.aggregation.TopHitsAggregation;
import org.graylog.plugins.views.search.searchtypes.pivot.Pivot;
import org.graylog.plugins.views.search.searchtypes.pivot.series.Latest;
import org.graylog.plugins.views.search.searchtypes.pivot.series.Max;
import org.graylog.shaded.elasticsearch5.org.elasticsearch.search.aggregations.AggregationBuilder;
import org.graylog.shaded.elasticsearch5.org.elasticsearch.search.aggregations.AggregationBuilders;
import org.graylog.shaded.elasticsearch5.org.elasticsearch.search.aggregations.metrics.tophits.TopHitsAggregationBuilder;
import org.graylog.shaded.elasticsearch5.org.elasticsearch.search.sort.SortBuilders;
import org.graylog.shaded.elasticsearch5.org.elasticsearch.search.sort.SortOrder;
import org.graylog.storage.elasticsearch6.views.ESGeneratedQueryContext;
import org.graylog.storage.elasticsearch6.views.searchtypes.pivot.ESPivot;
import org.graylog.storage.elasticsearch6.views.searchtypes.pivot.ESPivotSeriesSpecHandler;

import javax.annotation.Nonnull;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

public class ESLatestHandler extends ESPivotSeriesSpecHandler<Latest, TopHitsAggregation> {
    @Nonnull
    @Override
    public Optional<AggregationBuilder> doCreateAggregation(String name, Pivot pivot, Latest latestSpec, ESPivot searchTypeHandler, ESGeneratedQueryContext queryContext) {
        final TopHitsAggregationBuilder latest = AggregationBuilders.topHits(name).size(1).sort(SortBuilders.fieldSort("timestamp").order(SortOrder.DESC));
        record(queryContext, pivot, latestSpec, name, MaxAggregation.class);
        return Optional.of(latest);
    }

    @Override
    public Stream<Value> doHandleResult(Pivot pivot,
                                        Latest pivotSpec,
                                        SearchResult searchResult,
                                        TopHitsAggregation latestAggregation,
                                        ESPivot searchTypeHandler,
                                        ESGeneratedQueryContext esGeneratedQueryContext) {
        final Optional<Value> latestValue = Optional.ofNullable(latestAggregation.getFirstHit(Map.class))
                .flatMap(hit -> Optional.ofNullable(hit.source)
                        .map(source -> source.get(pivotSpec.field())))
                .map(value -> Value.create(pivotSpec.id(), Latest.NAME, value));
        return latestValue.map(Stream::of).orElse(Stream.empty());
    }
}
