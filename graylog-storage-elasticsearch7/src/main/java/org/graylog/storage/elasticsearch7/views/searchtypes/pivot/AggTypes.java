package org.graylog.storage.elasticsearch7.views.searchtypes.pivot;

import org.graylog.plugins.views.search.searchtypes.pivot.PivotSpec;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.search.aggregations.Aggregation;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.search.aggregations.HasAggregations;
import org.jooq.lambda.tuple.Tuple;
import org.jooq.lambda.tuple.Tuple2;

import java.util.IdentityHashMap;

/**
 * This solely exists to hide the nasty type signature of the aggregation type map from the rest of the code.
 * It's just ugly and in the way.
 */
public class AggTypes {
    final IdentityHashMap<PivotSpec, Tuple2<String, Class<? extends Aggregation>>> aggTypeMap = new IdentityHashMap<>();

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
