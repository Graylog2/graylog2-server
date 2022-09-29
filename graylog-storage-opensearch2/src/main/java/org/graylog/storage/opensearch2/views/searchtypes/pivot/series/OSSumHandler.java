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
import org.graylog.plugins.views.search.searchtypes.pivot.series.Sum;
import org.graylog.storage.opensearch2.views.OSGeneratedQueryContext;
import org.graylog.storage.opensearch2.views.searchtypes.OSSearchTypeHandler;
import org.graylog.storage.opensearch2.views.searchtypes.pivot.OSPivot;
import org.graylog.storage.opensearch2.views.searchtypes.pivot.OSPivotSeriesSpecHandler;
import org.graylog.shaded.opensearch2.org.opensearch.action.search.SearchResponse;
import org.graylog.shaded.opensearch2.org.opensearch.search.aggregations.AggregationBuilder;
import org.graylog.shaded.opensearch2.org.opensearch.search.aggregations.AggregationBuilders;
import org.graylog.shaded.opensearch2.org.opensearch.search.aggregations.metrics.SumAggregationBuilder;

import javax.annotation.Nonnull;
import java.util.Optional;
import java.util.stream.Stream;

public class OSSumHandler extends OSPivotSeriesSpecHandler<Sum, org.graylog.shaded.opensearch2.org.opensearch.search.aggregations.metrics.Sum> {
    @Nonnull
    @Override
    public Optional<AggregationBuilder> doCreateAggregation(String name, Pivot pivot, Sum sumSpec, OSSearchTypeHandler<Pivot> searchTypeHandler, OSGeneratedQueryContext queryContext) {
        final SumAggregationBuilder sum = AggregationBuilders.sum(name).field(sumSpec.field());
        record(queryContext, pivot, sumSpec, name, org.graylog.shaded.opensearch2.org.opensearch.search.aggregations.metrics.Sum.class);
        return Optional.of(sum);
    }

    @Override
    public Stream<Value> doHandleResult(Pivot pivot, Sum pivotSpec,
                                        SearchResponse searchResult,
                                        org.graylog.shaded.opensearch2.org.opensearch.search.aggregations.metrics.Sum sumAggregation,
                                        OSSearchTypeHandler<Pivot> searchTypeHandler,
                                        OSGeneratedQueryContext OSGeneratedQueryContext) {
        return Stream.of(Value.create(pivotSpec.id(), Sum.NAME, sumAggregation.getValue()));
    }
}
