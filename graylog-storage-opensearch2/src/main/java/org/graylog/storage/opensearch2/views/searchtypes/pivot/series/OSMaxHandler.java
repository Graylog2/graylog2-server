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
import org.graylog.storage.opensearch2.views.OSGeneratedQueryContext;
import org.graylog.storage.opensearch2.views.searchtypes.pivot.OSPivot;
import org.graylog.storage.opensearch2.views.searchtypes.pivot.OSPivotSeriesSpecHandler;
import org.opensearch.action.search.SearchResponse;
import org.opensearch.search.aggregations.AggregationBuilder;
import org.opensearch.search.aggregations.AggregationBuilders;
import org.opensearch.search.aggregations.metrics.MaxAggregationBuilder;

import javax.annotation.Nonnull;
import java.util.Optional;
import java.util.stream.Stream;

public class OSMaxHandler extends OSPivotSeriesSpecHandler<Max, org.opensearch.search.aggregations.metrics.Max> {
    @Nonnull
    @Override
    public Optional<AggregationBuilder> doCreateAggregation(String name, Pivot pivot, Max maxSpec, OSPivot searchTypeHandler, OSGeneratedQueryContext queryContext) {
        final MaxAggregationBuilder max = AggregationBuilders.max(name).field(maxSpec.field());
        record(queryContext, pivot, maxSpec, name, org.opensearch.search.aggregations.metrics.Max.class);
        return Optional.of(max);
    }

    @Override
    public Stream<Value> doHandleResult(Pivot pivot,
                                        Max pivotSpec,
                                        SearchResponse searchResult,
                                        org.opensearch.search.aggregations.metrics.Max maxAggregation,
                                        OSPivot searchTypeHandler,
                                        OSGeneratedQueryContext OSGeneratedQueryContext) {
        return Stream.of(OSPivotSeriesSpecHandler.Value.create(pivotSpec.id(), Max.NAME, maxAggregation.getValue()));
    }
}
