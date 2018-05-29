package org.graylog.plugins.enterprise.search.elasticsearch.searchtypes.aggregation;

import io.searchbox.core.SearchResult;
import io.searchbox.core.search.aggregation.Aggregation;
import io.searchbox.core.search.aggregation.ExtendedStatsAggregation;
import io.searchbox.core.search.aggregation.MetricAggregation;
import org.elasticsearch.search.aggregations.AggregationBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.metrics.stats.extended.ExtendedStatsAggregationBuilder;
import org.graylog.plugins.enterprise.search.elasticsearch.ESGeneratedQueryContext;
import org.graylog.plugins.enterprise.search.searchtypes.aggregation.AggregationSpec;
import org.graylog.plugins.enterprise.search.searchtypes.aggregation.StdDevMetric;
import org.jooq.lambda.tuple.Tuple2;

import javax.annotation.Nonnull;
import java.util.Optional;

public class ESStdDevMetricHandler implements ESAggregationSpecHandler<StdDevMetric, ExtendedStatsAggregation> {

    @Nonnull
    @Override
    public Optional<AggregationBuilder> doCreateAggregation(String name, StdDevMetric metricSpec, ESAggregation aggregationSearchTypeHandler, ESGeneratedQueryContext queryContext) {
        final ExtendedStatsAggregationBuilder field = AggregationBuilders.extendedStats(name).field(metricSpec.field());
        queryContext.recordAggregationType(metricSpec, name, ExtendedStatsAggregation.class);
        return Optional.of(field);
    }

    @Override
    public StdDevMetric.Result doHandleResult(StdDevMetric aggregationSpec, SearchResult queryResult, ExtendedStatsAggregation extendedStatsAggregation, ESAggregation searchTypeHandler, ESGeneratedQueryContext queryContext) {
        return StdDevMetric.Result.create(extendedStatsAggregation.getStdDeviation());
    }

    @Override
    public Aggregation extractAggregationFromResult(AggregationSpec spec, MetricAggregation aggregations, ESGeneratedQueryContext queryContext) {
        final Tuple2<String, Class<? extends Aggregation>> objects = queryContext.typeForAggregationSpec(spec);
        return aggregations.getAggregation(objects.v1, objects.v2);
    }
}
