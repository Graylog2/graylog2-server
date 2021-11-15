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
package org.graylog.storage.elasticsearch6.views.searchtypes.pivot.series;

import io.searchbox.core.SearchResult;
import io.searchbox.core.search.aggregation.Aggregation;
import io.searchbox.core.search.aggregation.MaxAggregation;
import io.searchbox.core.search.aggregation.TopHitsAggregation;
import org.graylog.plugins.views.search.searchtypes.pivot.Pivot;
import org.graylog.plugins.views.search.searchtypes.pivot.series.Latest;
import org.graylog.shaded.elasticsearch6.org.elasticsearch.search.aggregations.AggregationBuilder;
import org.graylog.shaded.elasticsearch6.org.elasticsearch.search.aggregations.AggregationBuilders;
import org.graylog.shaded.elasticsearch6.org.elasticsearch.search.aggregations.metrics.tophits.TopHitsAggregationBuilder;
import org.graylog.shaded.elasticsearch6.org.elasticsearch.search.sort.SortBuilders;
import org.graylog.shaded.elasticsearch6.org.elasticsearch.search.sort.SortOrder;
import org.graylog.storage.elasticsearch6.views.ESGeneratedQueryContext;
import org.graylog.storage.elasticsearch6.views.searchtypes.pivot.ESPivot;
import org.graylog.storage.elasticsearch6.views.searchtypes.pivot.ESPivotSeriesSpecHandler;

import javax.annotation.Nonnull;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

public class ESLatestHandler extends ESPivotSeriesSpecHandler<Latest, LatestValueAggregation> {
    @Nonnull
    @Override
    public Optional<AggregationBuilder> doCreateAggregation(String name, Pivot pivot, Latest latestSpec, ESPivot searchTypeHandler, ESGeneratedQueryContext queryContext) {
        final TopHitsAggregationBuilder latest = AggregationBuilders.topHits(name).size(1).sort(SortBuilders.fieldSort("timestamp").order(SortOrder.DESC));
        record(queryContext, pivot, latestSpec, name, LatestValueAggregation.class);
        return Optional.of(latest);
    }

    @Override
    public Stream<Value> doHandleResult(Pivot pivot,
                                        Latest pivotSpec,
                                        SearchResult searchResult,
                                        LatestValueAggregation latestAggregation,
                                        ESPivot searchTypeHandler,
                                        ESGeneratedQueryContext esGeneratedQueryContext) {
        final Optional<Value> latestValue = latestAggregation.getField(pivotSpec.field())
                .map(value -> Value.create(pivotSpec.id(), Latest.NAME, value));
        return latestValue.map(Stream::of).orElse(Stream.empty());
    }
}
