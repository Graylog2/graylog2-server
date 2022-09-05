/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
 */
package org.graylog.storage.opensearch2.views.searchtypes.pivot.series;

import org.graylog.plugins.views.search.searchtypes.pivot.Pivot;
import org.graylog.plugins.views.search.searchtypes.pivot.series.Latest;
import org.graylog.storage.opensearch2.views.ESGeneratedQueryContext;
import org.graylog.storage.opensearch2.views.searchtypes.pivot.ESPivot;
import org.graylog.storage.opensearch2.views.searchtypes.pivot.ESPivotSeriesSpecHandler;
import org.opensearch.action.search.SearchResponse;
import org.opensearch.search.SearchHit;
import org.opensearch.search.SearchHits;
import org.opensearch.search.aggregations.AggregationBuilder;
import org.opensearch.search.aggregations.AggregationBuilders;
import org.opensearch.search.aggregations.metrics.TopHits;
import org.opensearch.search.aggregations.metrics.TopHitsAggregationBuilder;
import org.opensearch.search.sort.SortBuilders;
import org.opensearch.search.sort.SortOrder;

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
