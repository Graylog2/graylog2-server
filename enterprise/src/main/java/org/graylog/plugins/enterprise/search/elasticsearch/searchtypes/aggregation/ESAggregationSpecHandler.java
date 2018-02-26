package org.graylog.plugins.enterprise.search.elasticsearch.searchtypes.aggregation;

import io.searchbox.core.search.aggregation.Aggregation;
import io.searchbox.core.search.aggregation.MetricAggregation;
import org.elasticsearch.search.aggregations.AggregationBuilder;
import org.graylog.plugins.enterprise.search.elasticsearch.ESGeneratedQueryContext;
import org.graylog.plugins.enterprise.search.searchtypes.aggregation.AggregationSpec;
import org.graylog.plugins.enterprise.search.searchtypes.aggregation.AggregationSpecHandler;

import javax.annotation.Nonnull;

/**
 * Convenience interface to default to common types.
 *
 * @param <SPEC_TYPE> the type of the group aggregation spec handled by the implementations
 *                    * @param <RESULT> the actual type of the aggregation type in elasticsearch client, so that implementations don't have to cast manually
 */
public interface ESAggregationSpecHandler<SPEC_TYPE extends AggregationSpec, RESULT extends Aggregation>
        extends AggregationSpecHandler<SPEC_TYPE, AggregationBuilder, RESULT, ESAggregation, ESGeneratedQueryContext> {

    @Nonnull
    AggregationBuilder doCreateAggregation(String name, SPEC_TYPE aggregationSpec, ESAggregation searchTypeHandler, ESGeneratedQueryContext queryContext);

    // TODO this is a bit ugly and with a bit work could possibly be done generically
    Aggregation extractAggregationFromResult(AggregationSpec spec, MetricAggregation aggregations, ESGeneratedQueryContext queryContext);
}
