package org.graylog.plugins.enterprise.search.elasticsearch.searchtypes.aggregation;

import io.searchbox.core.SearchResult;
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
import javax.inject.Inject;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;


public class ESTimeHistogramHandler implements ESAggregationSpecHandler<TimeHistogramGroup, DateHistogramAggregation> {
    private final MetricSubAggregationConverter subAggregationConverter;

    @Inject
    public ESTimeHistogramHandler(MetricSubAggregationConverter subAggregationConverter) {
        this.subAggregationConverter = subAggregationConverter;
    }

    @Nonnull
    @Override
    public Optional<AggregationBuilder> doCreateAggregation(String name,
                                                            TimeHistogramGroup aggregationSpec,
                                                            ESAggregation searchTypeHandler,
                                                            ESGeneratedQueryContext queryContext) {
        final DateHistogramAggregationBuilder dateHistogram = AggregationBuilders.dateHistogram(name)
                .dateHistogramInterval(new DateHistogramInterval(aggregationSpec.interval()))
                .format("date_time") // force complete ISO-8601 strings to come back
                .field(aggregationSpec.field());

        queryContext.recordAggregationType(aggregationSpec, name, DateHistogramAggregation.class);
        // for each sub-spec, simply add it to the elasticsearch representation, collect the generated aggregations and return the builder
        return Optional.of(StreamEx.of(aggregationSpec.subAggregations().iterator())
                .map(spec -> searchTypeHandler
                        .handlerForType(spec.type())
                        .createAggregation(searchTypeHandler.nextName(), spec, searchTypeHandler, queryContext))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .reduce(dateHistogram, AggregationBuilder::subAggregation)
        );
    }

    @Override
    public TimeHistogramGroup.Result doHandleResult(TimeHistogramGroup aggregationSpec, SearchResult queryResult, DateHistogramAggregation dateHistogram, ESAggregation searchTypeHandler, ESGeneratedQueryContext queryContext) {

        // ffs wrong
        final DateTime minMax[] = new DateTime[]{
                null, null
        };
        final List<TimeHistogramGroup.Bucket> buckets = dateHistogram.getBuckets().stream()
                .map(bucket -> {
                    final DateTime bucketTime = DateTime.parse(bucket.getTimeAsString());
                    findMinMax(minMax, bucketTime);

                    final List<Object> metrics = subAggregationConverter.convert(aggregationSpec.metrics(), searchTypeHandler, queryContext, queryResult, bucket);
                    return TimeHistogramGroup.Bucket.builder()
                            .key(bucketTime)
                            .metrics(metrics)
                            .groups(subAggregationConverter.convert(aggregationSpec.groups(), searchTypeHandler, queryContext, queryResult, bucket))
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
