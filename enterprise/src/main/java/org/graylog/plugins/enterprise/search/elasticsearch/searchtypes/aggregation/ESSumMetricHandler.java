package org.graylog.plugins.enterprise.search.elasticsearch.searchtypes.aggregation;

import io.searchbox.core.SearchResult;
import io.searchbox.core.search.aggregation.Aggregation;
import io.searchbox.core.search.aggregation.MetricAggregation;
import io.searchbox.core.search.aggregation.SumAggregation;
import org.elasticsearch.search.aggregations.AggregationBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.metrics.sum.SumAggregationBuilder;
import org.graylog.plugins.enterprise.search.elasticsearch.ESGeneratedQueryContext;
import org.graylog.plugins.enterprise.search.searchtypes.aggregation.AggregationSpec;
import org.graylog.plugins.enterprise.search.searchtypes.aggregation.SumMetric;
import org.jooq.lambda.tuple.Tuple2;

import javax.annotation.Nonnull;
import java.util.Optional;

public class ESSumMetricHandler implements ESAggregationSpecHandler<SumMetric, SumAggregation> {

    @Nonnull
    @Override
    public Optional<AggregationBuilder> doCreateAggregation(String name, SumMetric metricSpec, ESAggregation aggregationSearchTypeHandler, ESGeneratedQueryContext queryContext) {
        final SumAggregationBuilder field = AggregationBuilders.sum(name).field(metricSpec.field());
        queryContext.recordAggregationType(metricSpec, name, SumAggregation.class);
        return Optional.of(field);
    }

    @Override
    public SumMetric.Result doHandleResult(SumMetric aggregationSpec, SearchResult queryResult, SumAggregation sumAggregation, ESAggregation searchTypeHandler, ESGeneratedQueryContext queryContext) {
        return SumMetric.Result.create(sumAggregation.getSum());
    }

    @Override
    public Aggregation extractAggregationFromResult(AggregationSpec spec, MetricAggregation aggregations, ESGeneratedQueryContext queryContext) {
        final Tuple2<String, Class<? extends Aggregation>> objects = queryContext.typeForAggregationSpec(spec);
        return aggregations.getAggregation(objects.v1, objects.v2);
    }
}
