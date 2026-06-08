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
package org.graylog.storage.opensearch3.views.searchtypes.pivot;

import org.graylog.plugins.views.search.engine.IndexerGeneratedQueryContext;
import org.graylog.plugins.views.search.searchtypes.pivot.Pivot;
import org.graylog.plugins.views.search.searchtypes.pivot.PivotSpec;
import org.graylog.plugins.views.search.searchtypes.pivot.SeriesSpec;
import org.graylog.plugins.views.search.searchtypes.pivot.SeriesSpecHandler;
import org.graylog.storage.opensearch3.views.OSGeneratedQueryContext;
import org.opensearch.client.json.JsonData;
import org.opensearch.client.opensearch._types.aggregations.Aggregate;
import org.opensearch.client.opensearch._types.aggregations.MultiBucketBase;
import org.opensearch.client.opensearch.core.msearch.MultiSearchItem;

import java.util.stream.Stream;

public abstract class OSPivotSeriesSpecHandler<SPEC_TYPE extends SeriesSpec>
        implements SeriesSpecHandler<SPEC_TYPE, SeriesAggregationBuilder, MultiSearchItem<JsonData>, Aggregate, OSGeneratedQueryContext> {

    public Aggregate extractAggregationFromResult(Pivot pivot, PivotSpec spec, MultiBucketBase currentAggregationOrBucket, IndexerGeneratedQueryContext<?> queryContext) {
        final String aggName = queryContext.getAggNameForPivotSpecFromContext(pivot, spec);
        return currentAggregationOrBucket.aggregations().get(aggName);
    }

    @SuppressWarnings("unchecked")
    public Stream<Value> handleResult(Pivot pivot, SeriesSpec seriesSpec, MultiSearchItem<JsonData> queryResult, Aggregate aggregationResult, OSGeneratedQueryContext queryContext) {
        return doHandleResult(pivot, (SPEC_TYPE) seriesSpec, queryResult, aggregationResult, queryContext);
    }

    @Override
    public abstract Stream<Value> doHandleResult(Pivot pivot, SPEC_TYPE seriesSpec, MultiSearchItem<JsonData> searchResult, Aggregate aggregation_result, OSGeneratedQueryContext queryContext);

}
