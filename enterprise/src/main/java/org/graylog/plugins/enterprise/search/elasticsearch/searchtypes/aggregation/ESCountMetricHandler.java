package org.graylog.plugins.enterprise.search.elasticsearch.searchtypes.aggregation;

import io.searchbox.core.SearchResult;
import io.searchbox.core.search.aggregation.Aggregation;
import io.searchbox.core.search.aggregation.Bucket;
import io.searchbox.core.search.aggregation.MetricAggregation;
import io.searchbox.core.search.aggregation.RootAggregation;
import io.searchbox.core.search.aggregation.ValueCountAggregation;
import org.elasticsearch.search.aggregations.AggregationBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.metrics.valuecount.ValueCountAggregationBuilder;
import org.graylog.plugins.enterprise.search.elasticsearch.ESGeneratedQueryContext;
import org.graylog.plugins.enterprise.search.searchtypes.aggregation.AggregationSpec;
import org.graylog.plugins.enterprise.search.searchtypes.aggregation.CountMetric;
import org.jooq.lambda.tuple.Tuple2;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.util.Optional;

public class ESCountMetricHandler implements ESAggregationSpecHandler<CountMetric, MetricAggregation> {
    private static final Logger LOG = LoggerFactory.getLogger(ESCountMetricHandler.class);

    @Nonnull
    @Override
    public Optional<AggregationBuilder> doCreateAggregation(String name, CountMetric metricSpec, ESAggregation searchTypeHandler, ESGeneratedQueryContext queryContext) {
        final String field = metricSpec.field();
        if (field == null) {
            // the request is for the doc_count of a bucket or the total number of documents in a search (aka total)
            // in either case we do not need to add an aggregation
            // TODO figure out how to detect that we are in a top-level metric aggregation, without a surrounding bucket aggregation
            return Optional.empty();
        } else {
            // if the request was for a field count, we have to add a value_count sub aggregation
            final ValueCountAggregationBuilder value = AggregationBuilders.count(name).field(field);
            queryContext.recordAggregationType(metricSpec, name, ValueCountAggregation.class);
            return Optional.of(value);
        }
    }

    @Override
    public CountMetric.Result doHandleResult(CountMetric aggregationSpec, SearchResult queryResult, MetricAggregation aggregation, ESAggregation searchTypeHandler, ESGeneratedQueryContext queryContext) {
        if (aggregation instanceof Bucket) {
            return CountMetric.Result.create(((Bucket) aggregation).getCount());
        }
        if (aggregation instanceof RootAggregation) {
                // the count request was for the total of the entire search result (top-level metric aggregation)
                return CountMetric.Result.create(queryResult.getTotal());
        }
        // in this case we generated a ValueCount aggregation
        if (aggregation instanceof ValueCountAggregation) {
            return CountMetric.Result.create(((ValueCountAggregation) aggregation).getValueCount());
        }
        LOG.error("Unexpected aggregation type {}, returning 0 for the count. This is a bug.", aggregation);
        // safe fallback
        return CountMetric.Result.create(0);
    }

    @Override
    public Aggregation extractAggregationFromResult(AggregationSpec spec, MetricAggregation aggregations, ESGeneratedQueryContext queryContext) {
        final Tuple2<String, Class<? extends Aggregation>> objects = queryContext.typeForAggregationSpec(spec);
        if (objects == null) {
            return aggregations;
        } else {
            // try to saved sub aggregation type. this might fail if we refer to the total result of the entire result instead of a specific
            // value_count aggregation. we'll handle that special case in doHandleResult above
            final Aggregation subAggregation = aggregations.getAggregation(objects.v1, objects.v2);
            if (subAggregation == null) {
                // only to avoid returning null here
                return aggregations;
            } else {
                return subAggregation;
            }
        }
    }
}
