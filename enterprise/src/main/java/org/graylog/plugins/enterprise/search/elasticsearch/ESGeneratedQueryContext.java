package org.graylog.plugins.enterprise.search.elasticsearch;

import com.google.common.base.MoreObjects;
import com.google.common.collect.Maps;
import io.searchbox.core.search.aggregation.Aggregation;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.graylog.plugins.enterprise.search.engine.GeneratedQueryContext;
import org.graylog.plugins.enterprise.search.searchtypes.aggregation.AggregationSpec;
import org.jooq.lambda.tuple.Tuple;
import org.jooq.lambda.tuple.Tuple2;

import java.util.IdentityHashMap;

public class ESGeneratedQueryContext implements GeneratedQueryContext {

    private SearchSourceBuilder ssb;
    // do _NOT_ turn this into a regular hashmap!
    private IdentityHashMap<AggregationSpec, Tuple2<String, Class<? extends Aggregation>>> aggResultTypes = Maps.newIdentityHashMap();

    public ESGeneratedQueryContext(SearchSourceBuilder ssb) {
        this.ssb = ssb;
    }

    public SearchSourceBuilder searchSourceBuilder() {
        return ssb;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("elasticsearch query", ssb)
                .toString();
    }

    public void recordAggregationType(AggregationSpec aggregationSpec, String name, Class<? extends Aggregation> aggregationClass) {
        final Tuple2<String, Class<? extends Aggregation>> tuple = Tuple.tuple(name, aggregationClass);
        aggResultTypes.put(aggregationSpec, tuple);
    }

    public Tuple2<String, Class<? extends Aggregation>> typeForAggregationSpec(AggregationSpec aggregationSpec) {
        return aggResultTypes.get(aggregationSpec);
    }
}
