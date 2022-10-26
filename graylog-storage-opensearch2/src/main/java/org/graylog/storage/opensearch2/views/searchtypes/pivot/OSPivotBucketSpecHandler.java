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

import org.graylog.plugins.views.search.searchtypes.pivot.BucketSpec;
import org.graylog.plugins.views.search.searchtypes.pivot.BucketSpecHandler;
import org.graylog.plugins.views.search.searchtypes.pivot.Pivot;
import org.graylog.plugins.views.search.searchtypes.pivot.PivotSpec;
import org.graylog.storage.opensearch2.views.OSGeneratedQueryContext;
import org.graylog.shaded.opensearch2.org.opensearch.action.search.SearchResponse;
import org.graylog.shaded.opensearch2.org.opensearch.search.aggregations.Aggregation;
import org.graylog.shaded.opensearch2.org.opensearch.search.aggregations.AggregationBuilder;
import org.graylog.shaded.opensearch2.org.opensearch.search.aggregations.HasAggregations;
import org.graylog.shaded.opensearch2.org.opensearch.search.aggregations.bucket.MultiBucketsAggregation;

import java.util.stream.Stream;

public abstract class OSPivotBucketSpecHandler<SPEC_TYPE extends BucketSpec, AGGREGATION_RESULT extends Aggregation>
        implements BucketSpecHandler<SPEC_TYPE, AggregationBuilder, SearchResponse, AGGREGATION_RESULT, OSGeneratedQueryContext> {

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
    public Stream<Bucket> handleResult(BucketSpec bucketSpec, Object aggregationResult) {
        return doHandleResult((SPEC_TYPE) bucketSpec, (AGGREGATION_RESULT) aggregationResult);
    }

    @Override
    public abstract Stream<Bucket> doHandleResult(SPEC_TYPE bucketSpec, AGGREGATION_RESULT aggregationResult);

    public static class Bucket {

        private final String key;
        private final MultiBucketsAggregation.Bucket bucket;

        public Bucket(String key, MultiBucketsAggregation.Bucket bucket) {
            this.key = key;
            this.bucket = bucket;
        }

        public static Bucket create(String key, MultiBucketsAggregation.Bucket aggregation) {
            return new Bucket(key, aggregation);
        }

        public String key() {
            return key;
        }

        public MultiBucketsAggregation.Bucket aggregation() {
            return bucket;
        }
    }
}
