package org.graylog.plugins.enterprise.search.elasticsearch.searchtypes.aggregation;

import io.searchbox.core.SearchResult;
import io.searchbox.core.search.aggregation.Aggregation;
import io.searchbox.core.search.aggregation.AvgAggregation;
import io.searchbox.core.search.aggregation.MetricAggregation;
import org.elasticsearch.search.aggregations.AggregationBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.metrics.avg.AvgAggregationBuilder;
import org.graylog.plugins.enterprise.search.elasticsearch.ESGeneratedQueryContext;
import org.graylog.plugins.enterprise.search.searchtypes.aggregation.AggregationSpec;
import org.graylog.plugins.enterprise.search.searchtypes.aggregation.AverageMetric;
import org.jooq.lambda.tuple.Tuple2;

import javax.annotation.Nonnull;
import java.util.Optional;

public class ESAverageMetricHandler implements ESAggregationSpecHandler<AverageMetric, AvgAggregation> {

    @Nonnull
    @Override
    public Optional<AggregationBuilder> doCreateAggregation(String name, AverageMetric metricSpec, ESAggregation aggregationSearchTypeHandler, ESGeneratedQueryContext queryContext) {
        final AvgAggregationBuilder field = AggregationBuilders.avg(name).field(metricSpec.field());
        queryContext.recordAggregationType(metricSpec, name, AvgAggregation.class);
        return Optional.of(field);
    }

    @Override
    public AverageMetric.Result doHandleResult(AverageMetric aggregationSpec, SearchResult queryResult, AvgAggregation avgAggregation, ESAggregation searchTypeHandler, ESGeneratedQueryContext queryContext) {
        return AverageMetric.Result.create(avgAggregation.getAvg());
    }

    @Override
    public Aggregation extractAggregationFromResult(AggregationSpec spec, MetricAggregation aggregations, ESGeneratedQueryContext queryContext) {
        final Tuple2<String, Class<? extends Aggregation>> objects = queryContext.typeForAggregationSpec(spec);
        return aggregations.getAggregation(objects.v1, objects.v2);
    }
}
