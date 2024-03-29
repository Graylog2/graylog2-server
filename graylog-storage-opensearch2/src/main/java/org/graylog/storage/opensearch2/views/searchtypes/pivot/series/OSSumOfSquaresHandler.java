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
import org.graylog.plugins.views.search.searchtypes.pivot.series.SumOfSquares;
import org.graylog.shaded.opensearch2.org.opensearch.action.search.SearchResponse;
import org.graylog.shaded.opensearch2.org.opensearch.search.aggregations.AggregationBuilders;
import org.graylog.shaded.opensearch2.org.opensearch.search.aggregations.metrics.ExtendedStats;
import org.graylog.shaded.opensearch2.org.opensearch.search.aggregations.metrics.ExtendedStatsAggregationBuilder;
import org.graylog.storage.opensearch2.views.OSGeneratedQueryContext;
import org.graylog.storage.opensearch2.views.searchtypes.OSSearchTypeHandler;
import org.graylog.storage.opensearch2.views.searchtypes.pivot.OSPivotSeriesSpecHandler;
import org.graylog.storage.opensearch2.views.searchtypes.pivot.SeriesAggregationBuilder;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.stream.Stream;

public class OSSumOfSquaresHandler extends OSPivotSeriesSpecHandler<SumOfSquares, ExtendedStats> {
    @Nonnull
    @Override
    public List<SeriesAggregationBuilder> doCreateAggregation(String name, Pivot pivot, SumOfSquares sumOfSquaresSpec, OSSearchTypeHandler<Pivot> searchTypeHandler, OSGeneratedQueryContext queryContext) {
        final ExtendedStatsAggregationBuilder sumOfSquares = AggregationBuilders.extendedStats(name).field(sumOfSquaresSpec.field());
        record(queryContext, pivot, sumOfSquaresSpec, name, ExtendedStats.class);
        return List.of(SeriesAggregationBuilder.metric(sumOfSquares));
    }

    @Override
    public Stream<Value> doHandleResult(Pivot pivot, SumOfSquares pivotSpec,
                                        SearchResponse searchResult,
                                        ExtendedStats sumOfSquaresAggregation,
                                        OSSearchTypeHandler<Pivot> searchTypeHandler,
                                        OSGeneratedQueryContext OSGeneratedQueryContext) {
        return Stream.of(OSPivotSeriesSpecHandler.Value.create(pivotSpec.id(), SumOfSquares.NAME, sumOfSquaresAggregation.getSumOfSquares()));
    }
}
