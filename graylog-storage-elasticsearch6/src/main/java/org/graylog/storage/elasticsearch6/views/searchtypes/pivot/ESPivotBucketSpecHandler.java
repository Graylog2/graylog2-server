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
package org.graylog.storage.elasticsearch6.views.searchtypes.pivot;

import io.searchbox.core.SearchResult;
import io.searchbox.core.search.aggregation.Aggregation;
import io.searchbox.core.search.aggregation.MetricAggregation;
import org.graylog.plugins.views.search.searchtypes.pivot.BucketSpec;
import org.graylog.plugins.views.search.searchtypes.pivot.BucketSpecHandler;
import org.graylog.plugins.views.search.searchtypes.pivot.Pivot;
import org.graylog.plugins.views.search.searchtypes.pivot.PivotSpec;
import org.graylog.shaded.elasticsearch6.org.elasticsearch.search.aggregations.AggregationBuilder;
import org.graylog.storage.elasticsearch6.views.ESGeneratedQueryContext;

import java.util.stream.Stream;

public abstract class ESPivotBucketSpecHandler<SPEC_TYPE extends BucketSpec, AGGREGATION_RESULT extends Aggregation>
        implements BucketSpecHandler<SPEC_TYPE, AggregationBuilder, SearchResult, AGGREGATION_RESULT, ESGeneratedQueryContext> {

    protected ESPivot.AggTypes aggTypes(ESGeneratedQueryContext queryContext, Pivot pivot) {
        return (ESPivot.AggTypes) queryContext.contextMap().get(pivot.id());
    }

    protected void record(ESGeneratedQueryContext queryContext, Pivot pivot, PivotSpec spec, String name, Class<? extends Aggregation> aggregationClass) {
        aggTypes(queryContext, pivot).record(spec, name, aggregationClass);
    }

    protected Aggregation extractAggregationFromResult(Pivot pivot, PivotSpec spec, MetricAggregation aggregations, ESGeneratedQueryContext queryContext) {
        return aggTypes(queryContext, pivot).getSubAggregation(spec, aggregations);
    }

    @SuppressWarnings("unchecked")
    @Override
    public Stream<Bucket> handleResult(BucketSpec bucketSpec, Object aggregationResult) {
        return doHandleResult((SPEC_TYPE) bucketSpec, (AGGREGATION_RESULT) aggregationResult);
    }

    @Override
    public abstract Stream<Bucket> doHandleResult(SPEC_TYPE bucketSpec, AGGREGATION_RESULT aggregationResult);

    public static class Bucket {

        private final String key;
        private final MetricAggregation bucket;

        public Bucket(String key, MetricAggregation bucket) {
            this.key = key;
            this.bucket = bucket;
        }

        public static Bucket create(String key, MetricAggregation aggregation) {
            return new Bucket(key, aggregation);
        }

        public String key() {
            return key;
        }

        public MetricAggregation aggregation() {
            return bucket;
        }
    }
}
