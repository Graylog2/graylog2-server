package org.graylog.plugins.enterprise.search.elasticsearch.searchtypes.aggregation;

import io.searchbox.core.SearchResult;
import io.searchbox.core.search.aggregation.Aggregation;
import io.searchbox.core.search.aggregation.CardinalityAggregation;
import io.searchbox.core.search.aggregation.MetricAggregation;
import org.elasticsearch.search.aggregations.AggregationBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.metrics.cardinality.CardinalityAggregationBuilder;
import org.graylog.plugins.enterprise.search.elasticsearch.ESGeneratedQueryContext;
import org.graylog.plugins.enterprise.search.searchtypes.aggregation.AggregationSpec;
import org.graylog.plugins.enterprise.search.searchtypes.aggregation.CardinalityMetric;
import org.jooq.lambda.tuple.Tuple2;

import javax.annotation.Nonnull;
import java.util.Optional;

public class ESCardinalityMetricHandler implements ESAggregationSpecHandler<CardinalityMetric, CardinalityAggregation> {

    @Nonnull
    @Override
    public Optional<AggregationBuilder> doCreateAggregation(String name, CardinalityMetric metricSpec, ESAggregation aggregationSearchTypeHandler, ESGeneratedQueryContext queryContext) {
        final CardinalityAggregationBuilder field = AggregationBuilders.cardinality(name).field(metricSpec.field());
        queryContext.recordAggregationType(metricSpec, name, CardinalityAggregation.class);
        return Optional.of(field);
    }

    @Override
    public CardinalityMetric.Result doHandleResult(CardinalityMetric aggregationSpec, SearchResult queryResult, CardinalityAggregation cardinalityAggregation, ESAggregation searchTypeHandler, ESGeneratedQueryContext queryContext) {
        return CardinalityMetric.Result.create(cardinalityAggregation.getCardinality());
    }

    @Override
    public Aggregation extractAggregationFromResult(AggregationSpec spec, MetricAggregation aggregations, ESGeneratedQueryContext queryContext) {
        final Tuple2<String, Class<? extends Aggregation>> objects = queryContext.typeForAggregationSpec(spec);
        return aggregations.getAggregation(objects.v1, objects.v2);
    }}
