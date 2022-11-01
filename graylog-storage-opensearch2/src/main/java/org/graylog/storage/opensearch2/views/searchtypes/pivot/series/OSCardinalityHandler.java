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
import org.graylog.plugins.views.search.searchtypes.pivot.series.Cardinality;
import org.graylog.storage.opensearch2.views.OSGeneratedQueryContext;
import org.graylog.storage.opensearch2.views.searchtypes.OSSearchTypeHandler;
import org.graylog.storage.opensearch2.views.searchtypes.pivot.OSPivotSeriesSpecHandler;
import org.graylog.shaded.opensearch2.org.opensearch.action.search.SearchResponse;
import org.graylog.shaded.opensearch2.org.opensearch.search.aggregations.AggregationBuilder;
import org.graylog.shaded.opensearch2.org.opensearch.search.aggregations.AggregationBuilders;
import org.graylog.shaded.opensearch2.org.opensearch.search.aggregations.metrics.CardinalityAggregationBuilder;

import javax.annotation.Nonnull;
import java.util.Optional;
import java.util.stream.Stream;

public class OSCardinalityHandler extends OSPivotSeriesSpecHandler<Cardinality, org.graylog.shaded.opensearch2.org.opensearch.search.aggregations.metrics.Cardinality> {
    @Nonnull
    @Override
    public Optional<AggregationBuilder> doCreateAggregation(String name, Pivot pivot, Cardinality cardinalitySpec, OSSearchTypeHandler<Pivot> searchTypeHandler, OSGeneratedQueryContext queryContext) {
        final CardinalityAggregationBuilder card = AggregationBuilders.cardinality(name).field(cardinalitySpec.field());
        record(queryContext, pivot, cardinalitySpec, name, org.graylog.shaded.opensearch2.org.opensearch.search.aggregations.metrics.Cardinality.class);
        return Optional.of(card);
    }

    @Override
    public Stream<Value> doHandleResult(Pivot pivot, Cardinality pivotSpec,
                                        SearchResponse searchResult,
                                        org.graylog.shaded.opensearch2.org.opensearch.search.aggregations.metrics.Cardinality cardinalityAggregation,
                                        OSSearchTypeHandler<Pivot> searchTypeHandler,
                                        OSGeneratedQueryContext OSGeneratedQueryContext) {
        return Stream.of(OSPivotSeriesSpecHandler.Value.create(pivotSpec.id(), Cardinality.NAME, cardinalityAggregation.getValue()));
    }
}
