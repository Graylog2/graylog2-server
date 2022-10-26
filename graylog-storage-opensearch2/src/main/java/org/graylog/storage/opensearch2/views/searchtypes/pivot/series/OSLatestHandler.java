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
import org.graylog.storage.opensearch2.views.OSGeneratedQueryContext;
import org.graylog.storage.opensearch2.views.searchtypes.pivot.OSPivot;
import org.graylog.storage.opensearch2.views.searchtypes.pivot.OSPivotSeriesSpecHandler;
import org.graylog.shaded.opensearch2.org.opensearch.action.search.SearchResponse;
import org.graylog.shaded.opensearch2.org.opensearch.search.SearchHit;
import org.graylog.shaded.opensearch2.org.opensearch.search.SearchHits;
import org.graylog.shaded.opensearch2.org.opensearch.search.aggregations.AggregationBuilder;
import org.graylog.shaded.opensearch2.org.opensearch.search.aggregations.AggregationBuilders;
import org.graylog.shaded.opensearch2.org.opensearch.search.aggregations.metrics.TopHits;
import org.graylog.shaded.opensearch2.org.opensearch.search.aggregations.metrics.TopHitsAggregationBuilder;
import org.graylog.shaded.opensearch2.org.opensearch.search.sort.SortBuilders;
import org.graylog.shaded.opensearch2.org.opensearch.search.sort.SortOrder;

import javax.annotation.Nonnull;
import java.util.Optional;
import java.util.stream.Stream;

public class OSLatestHandler extends OSPivotSeriesSpecHandler<Latest, TopHits> {
    @Nonnull
    @Override
    public Optional<AggregationBuilder> doCreateAggregation(String name, Pivot pivot, Latest latestSpec, OSPivot searchTypeHandler, OSGeneratedQueryContext queryContext) {
        final TopHitsAggregationBuilder latest = AggregationBuilders.topHits(name).size(1).sort(SortBuilders.fieldSort("timestamp").order(SortOrder.DESC));
        record(queryContext, pivot, latestSpec, name, TopHits.class);
        return Optional.of(latest);
    }

    @Override
    public Stream<Value> doHandleResult(Pivot pivot,
                                        Latest pivotSpec,
                                        SearchResponse searchResult,
                                        TopHits latestAggregation,
                                        OSPivot searchTypeHandler,
                                        OSGeneratedQueryContext OSGeneratedQueryContext) {
        final Optional<Value> latestValue = Optional.ofNullable(latestAggregation.getHits())
                .map(SearchHits::getHits)
                .filter(hits -> hits.length > 0)
                .map(hits -> hits[0])
                .map(SearchHit::getSourceAsMap)
                .map(source -> source.get(pivotSpec.field()))
                .map(value -> Value.create(pivotSpec.id(), Latest.NAME, value));
        return latestValue.stream();
    }
}
