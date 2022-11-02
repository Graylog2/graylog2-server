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
package org.graylog.storage.elasticsearch7.views.searchtypes.pivot.series;

import org.graylog.plugins.views.search.searchtypes.pivot.Pivot;
import org.graylog.plugins.views.search.searchtypes.pivot.series.Average;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.action.search.SearchResponse;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.search.aggregations.AggregationBuilder;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.search.aggregations.AggregationBuilders;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.search.aggregations.metrics.Avg;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.search.aggregations.metrics.AvgAggregationBuilder;
import org.graylog.storage.elasticsearch7.views.ESGeneratedQueryContext;
import org.graylog.storage.elasticsearch7.views.searchtypes.ESSearchTypeHandler;
import org.graylog.storage.elasticsearch7.views.searchtypes.pivot.ESPivotSeriesSpecHandler;

import javax.annotation.Nonnull;
import java.util.Optional;
import java.util.stream.Stream;

public class ESAverageHandler extends ESPivotSeriesSpecHandler<Average, Avg> {
    @Nonnull
    @Override
    public Optional<AggregationBuilder> doCreateAggregation(String name, Pivot pivot, Average avgSpec, ESSearchTypeHandler<Pivot> searchTypeHandler, ESGeneratedQueryContext queryContext) {
        final AvgAggregationBuilder avg = AggregationBuilders.avg(name).field(avgSpec.field());
        record(queryContext, pivot, avgSpec, name, Avg.class);
        return Optional.of(avg);
    }

    @Override
    public Stream<ESPivotSeriesSpecHandler.Value> doHandleResult(Pivot pivot, Average pivotSpec,
                                                                 SearchResponse searchResult,
                                                                 Avg avgAggregation,
                                                                 ESSearchTypeHandler<Pivot> searchTypeHandler,
                                                                 ESGeneratedQueryContext esGeneratedQueryContext) {
        return Stream.of(ESPivotSeriesSpecHandler.Value.create(pivotSpec.id(), Average.NAME, avgAggregation.getValue()));
    }
}
