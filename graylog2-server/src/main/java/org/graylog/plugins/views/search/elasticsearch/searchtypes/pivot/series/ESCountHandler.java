package org.graylog.plugins.views.search.elasticsearch.searchtypes.pivot.series;

import io.searchbox.core.SearchResult;
import io.searchbox.core.search.aggregation.Aggregation;
import io.searchbox.core.search.aggregation.Bucket;
import io.searchbox.core.search.aggregation.MetricAggregation;
import io.searchbox.core.search.aggregation.RootAggregation;
import io.searchbox.core.search.aggregation.ValueCountAggregation;
import org.elasticsearch.search.aggregations.AggregationBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.metrics.valuecount.ValueCountAggregationBuilder;
import org.graylog.plugins.views.search.elasticsearch.ESGeneratedQueryContext;
import org.graylog.plugins.views.search.elasticsearch.searchtypes.pivot.ESPivot;
import org.graylog.plugins.views.search.elasticsearch.searchtypes.pivot.ESPivotSeriesSpecHandler;
import org.graylog.plugins.views.search.searchtypes.pivot.Pivot;
import org.graylog.plugins.views.search.searchtypes.pivot.PivotSpec;
import org.graylog.plugins.views.search.searchtypes.pivot.series.Count;
import org.graylog.plugins.views.search.elasticsearch.searchtypes.pivot.ESPivot;
import org.graylog.plugins.views.search.elasticsearch.searchtypes.pivot.ESPivotSeriesSpecHandler;
import org.jooq.lambda.tuple.Tuple2;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.util.Optional;
import java.util.stream.Stream;

public class ESCountHandler extends ESPivotSeriesSpecHandler<Count, MetricAggregation> {
    private static final Logger LOG = LoggerFactory.getLogger(ESCountHandler.class);

    @Nonnull
    @Override
    public Optional<AggregationBuilder> doCreateAggregation(String name, Pivot pivot, Count count, ESPivot searchTypeHandler, ESGeneratedQueryContext queryContext) {
        final String field = count.field();
        if (field == null) {
            // doc_count is always present in elasticsearch's bucket aggregations, no need to add it
            return Optional.empty();
        } else {
            // the request was for a field count, we have to add a value_count sub aggregation
            final ValueCountAggregationBuilder value = AggregationBuilders.count(name).field(field);
            record(queryContext, pivot, count, name, ValueCountAggregation.class);
            return Optional.of(value);
        }
    }

    @Override
    public Stream<Value> doHandleResult(Pivot pivot,
                                        Count count,
                                        SearchResult searchResult,
                                        MetricAggregation metricAggregation,
                                        ESPivot searchTypeHandler,
                                        ESGeneratedQueryContext esGeneratedQueryContext) {
        Object value = null;
        if (metricAggregation instanceof Bucket) {
            value = ((Bucket) metricAggregation).getCount();
        } else if (metricAggregation instanceof RootAggregation) {
            value = searchResult.getTotal();
        } else if (metricAggregation instanceof ValueCountAggregation) {
            value = ((ValueCountAggregation) metricAggregation).getValueCount();
        }
        if (value == null) {
            LOG.error("Unexpected aggregation type {}, returning 0 for the count. This is a bug.", metricAggregation);
            value = 0;
        }
        return Stream.of(Value.create(count.id(), Count.NAME, value));
    }

    @Override
    protected Aggregation extractAggregationFromResult(Pivot pivot, PivotSpec spec, MetricAggregation aggregations, ESGeneratedQueryContext queryContext) {
        final Tuple2<String, Class<? extends Aggregation>> objects = aggTypes(queryContext, pivot).getTypes(spec);
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
