package org.graylog.plugins.enterprise.search.elasticsearch.searchtypes.pivot.buckets;

import io.searchbox.core.SearchResult;
import io.searchbox.core.search.aggregation.DateHistogramAggregation;
import org.elasticsearch.search.aggregations.AggregationBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.histogram.DateHistogramAggregationBuilder;
import org.elasticsearch.search.aggregations.bucket.histogram.DateHistogramInterval;
import org.graylog.plugins.enterprise.search.Query;
import org.graylog.plugins.enterprise.search.elasticsearch.ESGeneratedQueryContext;
import org.graylog.plugins.enterprise.search.elasticsearch.searchtypes.pivot.ESPivot;
import org.graylog.plugins.enterprise.search.elasticsearch.searchtypes.pivot.ESPivotBucketSpecHandler;
import org.graylog.plugins.enterprise.search.searchtypes.pivot.Pivot;
import org.graylog.plugins.enterprise.search.searchtypes.pivot.buckets.Time;

import javax.annotation.Nonnull;
import java.util.Optional;
import java.util.stream.Stream;

public class ESTimeHandler extends ESPivotBucketSpecHandler<Time, DateHistogramAggregation> {
    @Nonnull
    @Override
    public Optional<AggregationBuilder> doCreateAggregation(String name, Pivot pivot, Time timeSpec, ESPivot searchTypeHandler, ESGeneratedQueryContext esGeneratedQueryContext, Query query) {
        final DateHistogramInterval dateHistogramInterval = timeSpec.interval().toDateHistogramInterval(query.timerange());
        final DateHistogramAggregationBuilder builder = AggregationBuilders.dateHistogram(name)
                .dateHistogramInterval(dateHistogramInterval)
                .field(timeSpec.field())
                .format("date_time");
        record(esGeneratedQueryContext, pivot, timeSpec, name, DateHistogramAggregation.class);

        return Optional.of(builder);
    }

    @Override
    public Stream<Bucket> doHandleResult(Pivot pivot,
                                         Time bucketSpec,
                                         SearchResult searchResult,
                                         DateHistogramAggregation dateHistogramAggregation,
                                         ESPivot searchTypeHandler,
                                         ESGeneratedQueryContext esGeneratedQueryContext) {
        return dateHistogramAggregation.getBuckets().stream()
                .map(dateHistogram -> Bucket.create(dateHistogram.getTimeAsString(), dateHistogram));
    }
}
