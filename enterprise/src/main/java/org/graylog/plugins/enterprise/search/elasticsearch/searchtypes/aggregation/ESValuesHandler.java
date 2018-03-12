package org.graylog.plugins.enterprise.search.elasticsearch.searchtypes.aggregation;

import io.searchbox.core.search.aggregation.Aggregation;
import io.searchbox.core.search.aggregation.MetricAggregation;
import io.searchbox.core.search.aggregation.TermsAggregation;
import one.util.streamex.StreamEx;
import org.elasticsearch.search.aggregations.AggregationBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.terms.TermsAggregationBuilder;
import org.graylog.plugins.enterprise.search.elasticsearch.ESGeneratedQueryContext;
import org.graylog.plugins.enterprise.search.searchtypes.aggregation.AggregationSpec;
import org.graylog.plugins.enterprise.search.searchtypes.aggregation.ValuesGroup;
import org.jooq.lambda.tuple.Tuple2;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import java.util.List;
import java.util.stream.Collectors;

public class ESValuesHandler implements ESAggregationSpecHandler<ValuesGroup, TermsAggregation> {
    private final MetricSubAggregationConverter subAggregationConverter;

    @Inject
    public ESValuesHandler(MetricSubAggregationConverter subAggregationConverter) {
        this.subAggregationConverter = subAggregationConverter;
    }

    @Nonnull
    @Override
    public AggregationBuilder doCreateAggregation(String name,
                                                  ValuesGroup aggregationSpec,
                                                  ESAggregation searchTypeHandler,
                                                  ESGeneratedQueryContext queryContext) {
        final TermsAggregationBuilder terms = AggregationBuilders.terms(name)
                .field(aggregationSpec.field());

        queryContext.recordAggregationType(aggregationSpec, name, TermsAggregation.class);

        return StreamEx.of(aggregationSpec.subAggregations().iterator())
                .map(spec -> searchTypeHandler
                        .handlerForType(spec.type())
                        .createAggregation(searchTypeHandler.nextName(), spec, searchTypeHandler, queryContext))
                .reduce(terms, AggregationBuilder::subAggregation);
    }

    @Override
    public Aggregation extractAggregationFromResult(AggregationSpec spec, MetricAggregation aggregations, ESGeneratedQueryContext queryContext) {
        final Tuple2<String, Class<? extends Aggregation>> objects = queryContext.typeForAggregationSpec(spec);
        return aggregations.getAggregation(objects.v1, objects.v2);
    }

    @Override
    public ValuesGroup.Result doHandleResult(ValuesGroup aggregationSpec, TermsAggregation termsAggregation, ESAggregation searchTypeHandler, ESGeneratedQueryContext esGeneratedQueryContext) {
        final List<ValuesGroup.Bucket> buckets = termsAggregation.getBuckets().stream()
                .map(bucket -> {
                    return ValuesGroup.Bucket.builder()
                            .key(bucket.getKey())
                            .count(bucket.getCount())
                            .metrics(subAggregationConverter.convert(aggregationSpec.metrics(), searchTypeHandler, esGeneratedQueryContext, bucket))
                            .groups(subAggregationConverter.convert(aggregationSpec.groups(), searchTypeHandler, esGeneratedQueryContext, bucket))
                            .build();
                })
                .collect(Collectors.toList());
        return ValuesGroup.Result.create(buckets);
    }
}
