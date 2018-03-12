package org.graylog.plugins.enterprise.search.elasticsearch.searchtypes.aggregation;

import io.searchbox.core.search.aggregation.Aggregation;
import io.searchbox.core.search.aggregation.MetricAggregation;
import org.graylog.plugins.enterprise.search.elasticsearch.ESGeneratedQueryContext;
import org.graylog.plugins.enterprise.search.searchtypes.aggregation.AggregationSpec;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

class MetricSubAggregationConverter {
    List<Object> convert(List<? extends AggregationSpec> specs, ESAggregation searchTypeHandler, ESGeneratedQueryContext queryContext, MetricAggregation bucket) {
        return specs.stream()
                .map((Function<AggregationSpec,Object>) aggSpec -> {
                    final ESAggregationSpecHandler<? extends AggregationSpec, ? extends Aggregation> handler = searchTypeHandler.handlerForType(aggSpec.type());
                    final Aggregation subAggregationResult = handler.extractAggregationFromResult(aggSpec, bucket, queryContext);
                    return handler.handleResult(aggSpec,
                            subAggregationResult, searchTypeHandler,
                            queryContext);
                })
                .collect(Collectors.toList());
    }
}
