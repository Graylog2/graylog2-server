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
package org.graylog.storage.elasticsearch7.views.searchtypes.pivot;

import org.graylog.plugins.views.search.engine.IndexerGeneratedQueryContext;
import org.graylog.plugins.views.search.searchtypes.pivot.Pivot;
import org.graylog.plugins.views.search.searchtypes.pivot.PivotSpec;
import org.graylog.plugins.views.search.searchtypes.pivot.SeriesSpec;
import org.graylog.plugins.views.search.searchtypes.pivot.SeriesSpecHandler;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.action.search.SearchResponse;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.search.aggregations.Aggregation;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.search.aggregations.HasAggregations;
import org.graylog.storage.elasticsearch7.views.ESGeneratedQueryContext;

import java.util.stream.Stream;

public abstract class ESPivotSeriesSpecHandler<SPEC_TYPE extends SeriesSpec, AGGREGATION_RESULT extends Aggregation>
        implements SeriesSpecHandler<SPEC_TYPE, SeriesAggregationBuilder, SearchResponse, AGGREGATION_RESULT, ESGeneratedQueryContext> {

    public Aggregation extractAggregationFromResult(Pivot pivot, PivotSpec spec, HasAggregations currentAggregationOrBucket, IndexerGeneratedQueryContext<?> queryContext) {
        final String aggName = queryContext.getAggNameForPivotSpecFromContext(pivot, spec);
        return currentAggregationOrBucket.getAggregations().get(aggName);
    }

    @SuppressWarnings("unchecked")
    @Override
    public Stream<Value> handleResult(Pivot pivot, SeriesSpec seriesSpec, SearchResponse queryResult, Aggregation aggregationResult, ESGeneratedQueryContext queryContext) {
        return doHandleResult(pivot, (SPEC_TYPE) seriesSpec, queryResult, (AGGREGATION_RESULT) aggregationResult, queryContext);
    }

    @Override
    public abstract Stream<Value> doHandleResult(Pivot pivot, SPEC_TYPE seriesSpec, SearchResponse searchResult, AGGREGATION_RESULT aggregationResult, ESGeneratedQueryContext queryContext);

}
