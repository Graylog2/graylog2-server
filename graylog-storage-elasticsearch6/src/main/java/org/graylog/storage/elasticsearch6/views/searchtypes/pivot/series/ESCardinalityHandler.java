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
import io.searchbox.core.search.aggregation.CardinalityAggregation;
import org.graylog.shaded.elasticsearch5.org.elasticsearch.search.aggregations.AggregationBuilder;
import org.graylog.shaded.elasticsearch5.org.elasticsearch.search.aggregations.AggregationBuilders;
import org.graylog.shaded.elasticsearch5.org.elasticsearch.search.aggregations.metrics.cardinality.CardinalityAggregationBuilder;
import org.graylog.storage.elasticsearch6.views.ESGeneratedQueryContext;
import org.graylog.storage.elasticsearch6.views.searchtypes.pivot.ESPivot;
import org.graylog.storage.elasticsearch6.views.searchtypes.pivot.ESPivotSeriesSpecHandler;
import org.graylog.plugins.views.search.searchtypes.pivot.Pivot;
import org.graylog.plugins.views.search.searchtypes.pivot.series.Cardinality;

import javax.annotation.Nonnull;
import java.util.Optional;
import java.util.stream.Stream;

public class ESCardinalityHandler extends ESPivotSeriesSpecHandler<Cardinality, CardinalityAggregation> {
    @Nonnull
    @Override
    public Optional<AggregationBuilder> doCreateAggregation(String name, Pivot pivot, Cardinality cardinalitySpec, ESPivot searchTypeHandler, ESGeneratedQueryContext queryContext) {
        final CardinalityAggregationBuilder card = AggregationBuilders.cardinality(name).field(cardinalitySpec.field());
        record(queryContext, pivot, cardinalitySpec, name, CardinalityAggregation.class);
        return Optional.of(card);
    }

    @Override
    public Stream<Value> doHandleResult(Pivot pivot, Cardinality pivotSpec,
                                        SearchResult searchResult,
                                        CardinalityAggregation cardinalityAggregation,
                                        ESPivot searchTypeHandler,
                                        ESGeneratedQueryContext esGeneratedQueryContext) {
        return Stream.of(ESPivotSeriesSpecHandler.Value.create(pivotSpec.id(), Cardinality.NAME, cardinalityAggregation.getCardinality()));
    }
}
