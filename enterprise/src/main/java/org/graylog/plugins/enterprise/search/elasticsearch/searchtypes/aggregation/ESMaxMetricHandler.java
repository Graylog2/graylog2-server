package org.graylog.plugins.enterprise.search.elasticsearch.searchtypes.aggregation;

import io.searchbox.core.SearchResult;
import io.searchbox.core.search.aggregation.Aggregation;
import io.searchbox.core.search.aggregation.MaxAggregation;
import io.searchbox.core.search.aggregation.MetricAggregation;
import org.elasticsearch.search.aggregations.AggregationBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.metrics.max.MaxAggregationBuilder;
import org.graylog.plugins.enterprise.search.elasticsearch.ESGeneratedQueryContext;
import org.graylog.plugins.enterprise.search.searchtypes.aggregation.AggregationSpec;
import org.graylog.plugins.enterprise.search.searchtypes.aggregation.MaxMetric;
import org.jooq.lambda.tuple.Tuple2;

import javax.annotation.Nonnull;
import java.util.Optional;

public class ESMaxMetricHandler implements ESAggregationSpecHandler<MaxMetric, MaxAggregation> {

    @Nonnull
    @Override
    public Optional<AggregationBuilder> doCreateAggregation(String name, MaxMetric metricSpec, ESAggregation aggregationSearchTypeHandler, ESGeneratedQueryContext queryContext) {
        final MaxAggregationBuilder field = AggregationBuilders.max(name).field(metricSpec.field());
        queryContext.recordAggregationType(metricSpec, name, MaxAggregation.class);
        return Optional.of(field);
    }

    @Override
    public MaxMetric.Result doHandleResult(MaxMetric aggregationSpec, SearchResult queryResult, MaxAggregation maxAggregation, ESAggregation searchTypeHandler, ESGeneratedQueryContext queryContext) {
        return MaxMetric.Result.create(maxAggregation.getMax());
    }

    @Override
    public Aggregation extractAggregationFromResult(AggregationSpec spec, MetricAggregation aggregations, ESGeneratedQueryContext queryContext) {
        final Tuple2<String, Class<? extends Aggregation>> objects = queryContext.typeForAggregationSpec(spec);
        return aggregations.getAggregation(objects.v1, objects.v2);
    }
}
