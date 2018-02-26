package org.graylog.plugins.enterprise.search.elasticsearch.searchtypes.aggregation;

import com.google.common.collect.ImmutableMap;
import io.searchbox.core.search.aggregation.Aggregation;
import io.searchbox.core.search.aggregation.DateHistogramAggregation;
import io.searchbox.core.search.aggregation.MetricAggregation;
import one.util.streamex.StreamEx;
import org.elasticsearch.search.aggregations.AggregationBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.histogram.DateHistogramAggregationBuilder;
import org.elasticsearch.search.aggregations.bucket.histogram.DateHistogramInterval;
import org.graylog.plugins.enterprise.search.elasticsearch.ESGeneratedQueryContext;
import org.graylog.plugins.enterprise.search.searchtypes.aggregation.AggregationSpec;
import org.graylog.plugins.enterprise.search.searchtypes.aggregation.TimeHistogramGroup;
import org.graylog2.plugin.indexer.searches.timeranges.AbsoluteRange;
import org.joda.time.DateTime;
import org.jooq.lambda.tuple.Tuple2;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;


public class ESTimeHistogramHandler implements ESAggregationSpecHandler<TimeHistogramGroup, DateHistogramAggregation> {

    @Nonnull
    @Override
    public AggregationBuilder doCreateAggregation(String name,
                                                  TimeHistogramGroup aggregationSpec,
                                                  ESAggregation searchTypeHandler,
                                                  ESGeneratedQueryContext queryContext) {
        final DateHistogramAggregationBuilder dateHistogram = AggregationBuilders.dateHistogram(name)
                .dateHistogramInterval(new DateHistogramInterval(aggregationSpec.interval()))
                .format("date_time") // force complete ISO-8601 strings to come back
                .field(aggregationSpec.field());

        queryContext.recordAggregationType(aggregationSpec, name, DateHistogramAggregation.class);
        // for each sub-spec, simply add it to the elasticsearch representation, collect the generated aggregations and return the builder
        return StreamEx.of(aggregationSpec.subAggregations().iterator())
                .map(spec -> searchTypeHandler
                        .handlerForType(spec.type())
                        .createAggregation(searchTypeHandler.nextName(), spec, searchTypeHandler, queryContext))
                .reduce(dateHistogram, AggregationBuilder::subAggregation);
    }

    @Override
    public TimeHistogramGroup.Result doHandleResult(TimeHistogramGroup aggregationSpec, DateHistogramAggregation dateHistogram, ESAggregation searchTypeHandler, ESGeneratedQueryContext queryContext) {

        // ffs wrong
        final DateTime minMax[] = new DateTime[]{
                null, null
        };
        final List<TimeHistogramGroup.Bucket> buckets = dateHistogram.getBuckets().stream()
                .map(bucket -> {
                    final DateTime bucketTime = DateTime.parse(bucket.getTimeAsString());
                    findMinMax(minMax, bucketTime);

                    final List<Object> metrics = convertSubAggregations(aggregationSpec.metrics(), searchTypeHandler, queryContext, bucket);
                    metrics.add(ImmutableMap.of("count", bucket.getCount()));
                    return TimeHistogramGroup.Bucket.builder()
                            .key(bucketTime)
                            .metrics(metrics)
                            .groups(convertSubAggregations(aggregationSpec.groups(), searchTypeHandler, queryContext, bucket))
                            .build();
                }).collect(Collectors.toList());

        // TODO ffs this is wronger
        final AbsoluteRange timerange;
        if (minMax[0] == null) {
            final DateTime now = DateTime.now();
            timerange = AbsoluteRange.create(now, now);
        } else {
            timerange = AbsoluteRange.create(minMax[0], minMax[1]);
        }
        return TimeHistogramGroup.Result.create(buckets, timerange);
    }

    private List<Object> convertSubAggregations(List<? extends AggregationSpec> specs, ESAggregation searchTypeHandler, ESGeneratedQueryContext queryContext, DateHistogramAggregation.DateHistogram bucket) {
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

    // TODO ugh
    private void findMinMax(DateTime[] minMax, DateTime bucketTime) {
        if (minMax[0] == null) {
            minMax[0] = bucketTime;
        } else {
            minMax[0] = bucketTime.isBefore(minMax[0]) ? bucketTime : minMax[0];
        }
        if (minMax[1] == null) {
            minMax[1] = bucketTime;
        } else {
            minMax[1] = bucketTime.isAfter(minMax[1]) ? bucketTime : minMax[1];
        }
    }

    @Override
    public Aggregation extractAggregationFromResult(AggregationSpec spec, MetricAggregation aggregations, ESGeneratedQueryContext queryContext) {
        final Tuple2<String, Class<? extends Aggregation>> objects = queryContext.typeForAggregationSpec(spec);
        return aggregations.getAggregation(objects.v1, objects.v2);
    }
}
