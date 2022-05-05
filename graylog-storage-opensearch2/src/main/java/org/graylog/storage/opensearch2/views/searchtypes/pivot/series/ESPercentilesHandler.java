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
import org.graylog.plugins.views.search.searchtypes.pivot.series.Percentile;
import org.graylog.storage.opensearch2.views.ESGeneratedQueryContext;
import org.graylog.storage.opensearch2.views.searchtypes.pivot.ESPivot;
import org.graylog.storage.opensearch2.views.searchtypes.pivot.ESPivotSeriesSpecHandler;
import org.opensearch.action.search.SearchResponse;
import org.opensearch.search.aggregations.AggregationBuilder;
import org.opensearch.search.aggregations.AggregationBuilders;
import org.opensearch.search.aggregations.metrics.Percentiles;
import org.opensearch.search.aggregations.metrics.PercentilesAggregationBuilder;

import javax.annotation.Nonnull;
import java.util.Optional;
import java.util.stream.Stream;

public class ESPercentilesHandler extends ESPivotSeriesSpecHandler<Percentile, Percentiles> {
    @Nonnull
    @Override
    public Optional<AggregationBuilder> doCreateAggregation(String name, Pivot pivot, Percentile percentileSpec, ESPivot searchTypeHandler, ESGeneratedQueryContext queryContext) {
        final PercentilesAggregationBuilder percentiles = AggregationBuilders.percentiles(name).field(percentileSpec.field()).percentiles(percentileSpec.percentile());
        record(queryContext, pivot, percentileSpec, name, Percentiles.class);
        return Optional.of(percentiles);
    }

    @Override
    public Stream<Value> doHandleResult(Pivot pivot,
                                        Percentile pivotSpec,
                                        SearchResponse searchResult,
                                        Percentiles percentilesAggregation,
                                        ESPivot searchTypeHandler,
                                        ESGeneratedQueryContext queryContext) {
        Double percentile = percentilesAggregation.percentile(pivotSpec.percentile());
        return Stream.of(ESPivotSeriesSpecHandler.Value.create(pivotSpec.id(), Percentile.NAME, percentile));
    }
}
