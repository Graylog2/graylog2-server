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
package org.graylog.storage.opensearch2.views.searchtypes.pivot;

import org.graylog.plugins.views.search.engine.GeneratedQueryContext;
import org.graylog.plugins.views.search.engine.SearchTypeHandler;
import org.graylog.plugins.views.search.searchtypes.pivot.Pivot;
import org.graylog.plugins.views.search.searchtypes.pivot.PivotSpec;
import org.graylog.plugins.views.search.searchtypes.pivot.SeriesSpec;
import org.graylog.plugins.views.search.searchtypes.pivot.SeriesSpecHandler;
import org.graylog.storage.opensearch2.views.OSGeneratedQueryContext;
import org.graylog.shaded.opensearch2.org.opensearch.action.search.SearchResponse;
import org.graylog.shaded.opensearch2.org.opensearch.search.aggregations.Aggregation;
import org.graylog.shaded.opensearch2.org.opensearch.search.aggregations.AggregationBuilder;
import org.graylog.shaded.opensearch2.org.opensearch.search.aggregations.HasAggregations;

import java.util.stream.Stream;

public abstract class OSPivotSeriesSpecHandler<SPEC_TYPE extends SeriesSpec, AGGREGATION_RESULT extends Aggregation>
        implements SeriesSpecHandler<SPEC_TYPE, AggregationBuilder, SearchResponse, AGGREGATION_RESULT, OSPivot, OSGeneratedQueryContext> {

    protected OSPivot.AggTypes aggTypes(OSGeneratedQueryContext queryContext, Pivot pivot) {
        return (OSPivot.AggTypes) queryContext.contextMap().get(pivot.id());
    }

    protected void record(OSGeneratedQueryContext queryContext, Pivot pivot, PivotSpec spec, String name, Class<? extends Aggregation> aggregationClass) {
        aggTypes(queryContext, pivot).record(spec, name, aggregationClass);
    }

    protected Aggregation extractAggregationFromResult(Pivot pivot, PivotSpec spec, HasAggregations aggregations, OSGeneratedQueryContext queryContext) {
        return aggTypes(queryContext, pivot).getSubAggregation(spec, aggregations);
    }

    @SuppressWarnings("unchecked")
    @Override
    public Stream<Value> handleResult(Pivot pivot, SeriesSpec seriesSpec, Object queryResult, Object aggregationResult, SearchTypeHandler searchTypeHandler, GeneratedQueryContext queryContext) {
        return doHandleResult(pivot, (SPEC_TYPE) seriesSpec, (SearchResponse) queryResult, (AGGREGATION_RESULT) aggregationResult, (OSPivot) searchTypeHandler, (OSGeneratedQueryContext) queryContext);
    }

    @Override
    public abstract Stream<Value> doHandleResult(Pivot pivot, SPEC_TYPE seriesSpec, SearchResponse searchResult, AGGREGATION_RESULT aggregation_result, OSPivot searchTypeHandler, OSGeneratedQueryContext queryContext);

    public static class Value {

        private final String id;
        private final String key;
        private final Object value;

        public Value(String id, String key, Object value) {
            this.id = id;
            this.key = key;
            this.value = value;
        }

        public static Value create(String id, String key, Object value) {
            return new Value(id, key, value);
        }

        public String id() {
            return id;
        }

        public String key() {
            return key;
        }

        public Object value() {
            return value;
        }
    }
}
