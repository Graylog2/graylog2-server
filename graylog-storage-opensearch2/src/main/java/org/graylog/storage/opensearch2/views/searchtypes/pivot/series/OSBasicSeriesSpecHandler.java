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
import org.graylog.plugins.views.search.searchtypes.pivot.SeriesSpec;
import org.graylog.plugins.views.search.searchtypes.pivot.SeriesSpecHandler;
import org.graylog.shaded.opensearch2.org.opensearch.action.search.SearchResponse;
import org.graylog.shaded.opensearch2.org.opensearch.search.aggregations.Aggregation;
import org.graylog.storage.opensearch2.views.OSGeneratedQueryContext;
import org.graylog.storage.opensearch2.views.searchtypes.pivot.OSPivotSeriesSpecHandler;
import org.graylog.storage.opensearch2.views.searchtypes.pivot.SeriesAggregationBuilder;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

public abstract class OSBasicSeriesSpecHandler<SPEC_TYPE extends SeriesSpec, AGGREGATION_RESULT extends Aggregation>
        extends OSPivotSeriesSpecHandler<SPEC_TYPE, AGGREGATION_RESULT> {

    @Override
    public @Nonnull List<SeriesAggregationBuilder> doCreateAggregation(String name,
                                                                       Pivot pivot,
                                                                       SPEC_TYPE seriesSpec,
                                                                       OSGeneratedQueryContext queryContext) {
        queryContext.recordNameForPivotSpec(pivot, seriesSpec, name);
        return List.of(createAggregationBuilder(name, seriesSpec));
    }

    protected abstract SeriesAggregationBuilder createAggregationBuilder(final String name, final SPEC_TYPE seriesSpec);

    @Override
    public Stream<SeriesSpecHandler.Value> doHandleResult(Pivot pivot,
                                                          SPEC_TYPE seriesSpec,
                                                          SearchResponse searchResult,
                                                          AGGREGATION_RESULT aggregationResult,
                                                          OSGeneratedQueryContext queryContext) {

        return Optional.ofNullable(getValueFromAggregationResult(aggregationResult, seriesSpec))
                .map(res -> Value.create(
                        seriesSpec.id(),
                        seriesSpec.type(),
                        res))
                .stream();
    }

    protected abstract Object getValueFromAggregationResult(final AGGREGATION_RESULT aggregationResult, final SPEC_TYPE seriesSpec);
}
