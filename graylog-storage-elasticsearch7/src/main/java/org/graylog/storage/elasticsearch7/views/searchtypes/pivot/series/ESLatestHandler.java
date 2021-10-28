package org.graylog.storage.elasticsearch7.views.searchtypes.pivot.series;

import org.graylog.plugins.views.search.searchtypes.pivot.Pivot;
import org.graylog.plugins.views.search.searchtypes.pivot.series.Latest;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.action.search.SearchResponse;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.common.document.DocumentField;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.search.SearchHit;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.search.SearchHits;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.search.aggregations.AggregationBuilder;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.search.aggregations.AggregationBuilders;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.search.aggregations.metrics.TopHits;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.search.aggregations.metrics.TopHitsAggregationBuilder;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.search.sort.SortBuilders;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.search.sort.SortOrder;
import org.graylog.storage.elasticsearch7.views.ESGeneratedQueryContext;
import org.graylog.storage.elasticsearch7.views.searchtypes.pivot.ESPivot;
import org.graylog.storage.elasticsearch7.views.searchtypes.pivot.ESPivotSeriesSpecHandler;

import javax.annotation.Nonnull;
import java.util.Optional;
import java.util.stream.Stream;

public class ESLatestHandler extends ESPivotSeriesSpecHandler<Latest, TopHits> {
    @Nonnull
    @Override
    public Optional<AggregationBuilder> doCreateAggregation(String name, Pivot pivot, Latest latestSpec, ESPivot searchTypeHandler, ESGeneratedQueryContext queryContext) {
        final TopHitsAggregationBuilder latest = AggregationBuilders.topHits(name).size(1).sort(SortBuilders.fieldSort("timestamp").order(SortOrder.DESC));
        record(queryContext, pivot, latestSpec, name, TopHits.class);
        return Optional.of(latest);
    }

    @Override
    public Stream<Value> doHandleResult(Pivot pivot,
                                        Latest pivotSpec,
                                        SearchResponse searchResult,
                                        TopHits latestAggregation,
                                        ESPivot searchTypeHandler,
                                        ESGeneratedQueryContext esGeneratedQueryContext) {
        final Optional<Value> latestValue = Optional.ofNullable(latestAggregation.getHits())
                .map(SearchHits::getHits)
                .map(hits -> hits[0])
                .map(SearchHit::getSourceAsMap)
                .map(source -> source.get(pivotSpec.field()))
                .map(value -> Value.create(pivotSpec.id(), Latest.NAME, value));
        return latestValue.map(Stream::of).orElse(Stream.empty());
    }
}
