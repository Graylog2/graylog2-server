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
import org.graylog.plugins.views.search.searchtypes.pivot.series.Max;
import org.graylog.storage.opensearch2.views.ESGeneratedQueryContext;
import org.graylog.storage.opensearch2.views.searchtypes.pivot.ESPivot;
import org.graylog.storage.opensearch2.views.searchtypes.pivot.ESPivotSeriesSpecHandler;
import org.opensearch.action.search.SearchResponse;
import org.opensearch.search.aggregations.AggregationBuilder;
import org.opensearch.search.aggregations.AggregationBuilders;
import org.opensearch.search.aggregations.metrics.MaxAggregationBuilder;

import javax.annotation.Nonnull;
import java.util.Optional;
import java.util.stream.Stream;

public class ESMaxHandler extends ESPivotSeriesSpecHandler<Max, org.opensearch.search.aggregations.metrics.Max> {
    @Nonnull
    @Override
    public Optional<AggregationBuilder> doCreateAggregation(String name, Pivot pivot, Max maxSpec, ESPivot searchTypeHandler, ESGeneratedQueryContext queryContext) {
        final MaxAggregationBuilder max = AggregationBuilders.max(name).field(maxSpec.field());
        record(queryContext, pivot, maxSpec, name, org.opensearch.search.aggregations.metrics.Max.class);
        return Optional.of(max);
    }

    @Override
    public Stream<Value> doHandleResult(Pivot pivot,
                                        Max pivotSpec,
                                        SearchResponse searchResult,
                                        org.opensearch.search.aggregations.metrics.Max maxAggregation,
                                        ESPivot searchTypeHandler,
                                        ESGeneratedQueryContext esGeneratedQueryContext) {
        return Stream.of(ESPivotSeriesSpecHandler.Value.create(pivotSpec.id(), Max.NAME, maxAggregation.getValue()));
    }
}
