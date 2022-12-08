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

import org.graylog.plugins.views.search.searchtypes.pivot.PivotSpec;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.search.aggregations.Aggregation;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.search.aggregations.HasAggregations;
import org.jooq.lambda.tuple.Tuple;
import org.jooq.lambda.tuple.Tuple2;

import java.util.HashMap;
import java.util.Map;

/**
 * This solely exists to hide the nasty type signature of the aggregation type map from the rest of the code.
 * It's just ugly and in the way.
 */
public class AggTypes {
    final Map<PivotSpec, Tuple2<String, Class<? extends Aggregation>>> aggTypeMap = new HashMap<>();

    public void record(PivotSpec pivotSpec, String name, Class<? extends Aggregation> aggClass) {
        aggTypeMap.put(pivotSpec, Tuple.tuple(name, aggClass));
    }

    public Aggregation getSubAggregation(PivotSpec pivotSpec, HasAggregations currentAggregationOrBucket) {
        final Tuple2<String, Class<? extends Aggregation>> tuple2 = getTypes(pivotSpec);
        return currentAggregationOrBucket.getAggregations().get(tuple2.v1);
    }

    public Tuple2<String, Class<? extends Aggregation>> getTypes(PivotSpec pivotSpec) {
        return aggTypeMap.get(pivotSpec);
    }
}
