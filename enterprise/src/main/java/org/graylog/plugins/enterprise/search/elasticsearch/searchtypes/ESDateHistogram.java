package org.graylog.plugins.enterprise.search.elasticsearch.searchtypes;

import io.searchbox.core.SearchResult;
import io.searchbox.core.search.aggregation.Bucket;
import io.searchbox.core.search.aggregation.DateHistogramAggregation;
import one.util.streamex.StreamEx;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.graylog.plugins.enterprise.search.Query;
import org.graylog.plugins.enterprise.search.SearchJob;
import org.graylog.plugins.enterprise.search.SearchType;
import org.graylog.plugins.enterprise.search.searchtypes.DateHistogram;
import org.graylog2.plugin.Message;
import org.graylog2.plugin.indexer.searches.timeranges.AbsoluteRange;
import org.graylog2.plugin.indexer.searches.timeranges.TimeRange;
import org.joda.time.DateTime;

import java.util.Map;

public class ESDateHistogram implements ESSearchTypeHandler<DateHistogram> {
    @Override
    public void doGenerateQueryPart(DateHistogram dateHistogram, SearchSourceBuilder searchSourceBuilder) {
        searchSourceBuilder.aggregation(
                AggregationBuilders.dateHistogram(dateHistogram.id())
                        .field(Message.FIELD_TIMESTAMP)
                        .interval(dateHistogram.interval().toESInterval()));
    }

    @SuppressWarnings("unchecked")
    @Override
    public SearchType.Result doExtractResult(SearchJob job, Query query, DateHistogram searchType, SearchResult result) {
        final DateHistogramAggregation dateHistogramAggregation = result.getAggregations().getDateHistogramAggregation(searchType.id());
        final Map<Long, Long> buckets = StreamEx.of(dateHistogramAggregation.getBuckets())
                .mapToEntry(bucket -> new DateTime(bucket.getKey()).getMillis() / 1000L,
                        Bucket::getCount)
                .toMap();

        final TimeRange timerange = query.timerange();
        return DateHistogram.Result.result(searchType.id())
                .results(buckets)
                .timerange(AbsoluteRange.create(timerange.getFrom(), timerange.getTo()))
                .build();

    }
}
