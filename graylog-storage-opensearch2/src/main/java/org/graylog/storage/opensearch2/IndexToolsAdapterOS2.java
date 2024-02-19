/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
 */
package org.graylog.storage.opensearch2;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import jakarta.inject.Inject;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.graylog.shaded.opensearch2.org.opensearch.action.search.SearchRequest;
import org.graylog.shaded.opensearch2.org.opensearch.action.search.SearchResponse;
import org.graylog.shaded.opensearch2.org.opensearch.action.support.IndicesOptions;
import org.graylog.shaded.opensearch2.org.opensearch.client.core.CountRequest;
import org.graylog.shaded.opensearch2.org.opensearch.client.core.CountResponse;
import org.graylog.shaded.opensearch2.org.opensearch.index.query.BoolQueryBuilder;
import org.graylog.shaded.opensearch2.org.opensearch.index.query.QueryBuilders;
import org.graylog.shaded.opensearch2.org.opensearch.index.query.RangeQueryBuilder;
import org.graylog.shaded.opensearch2.org.opensearch.search.aggregations.AggregationBuilders;
import org.graylog.shaded.opensearch2.org.opensearch.search.aggregations.bucket.filter.Filter;
import org.graylog.shaded.opensearch2.org.opensearch.search.aggregations.bucket.filter.FilterAggregationBuilder;
import org.graylog.shaded.opensearch2.org.opensearch.search.aggregations.bucket.filter.ParsedFilter;
import org.graylog.shaded.opensearch2.org.opensearch.search.aggregations.bucket.histogram.DateHistogramInterval;
import org.graylog.shaded.opensearch2.org.opensearch.search.aggregations.bucket.histogram.ParsedDateHistogram;
import org.graylog.shaded.opensearch2.org.opensearch.search.aggregations.bucket.terms.Terms;
import org.graylog.shaded.opensearch2.org.opensearch.search.aggregations.metrics.Max;
import org.graylog.shaded.opensearch2.org.opensearch.search.aggregations.metrics.MaxAggregationBuilder;
import org.graylog.shaded.opensearch2.org.opensearch.search.aggregations.metrics.Min;
import org.graylog.shaded.opensearch2.org.opensearch.search.aggregations.metrics.MinAggregationBuilder;
import org.graylog.shaded.opensearch2.org.opensearch.search.builder.SearchSourceBuilder;
import org.graylog2.indexer.IndexToolsAdapter;
import org.graylog2.plugin.Message;
import org.graylog2.plugin.indexer.searches.timeranges.TimeRange;
import org.graylog2.plugin.streams.Stream;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import javax.annotation.Nonnull;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.graylog.shaded.opensearch2.org.opensearch.index.query.QueryBuilders.boolQuery;
import static org.graylog.shaded.opensearch2.org.opensearch.index.query.QueryBuilders.existsQuery;
import static org.graylog.shaded.opensearch2.org.opensearch.index.query.QueryBuilders.matchAllQuery;
import static org.graylog.shaded.opensearch2.org.opensearch.index.query.QueryBuilders.termsQuery;

public class IndexToolsAdapterOS2 implements IndexToolsAdapter {
    private static final String AGG_DATE_HISTOGRAM = "source_date_histogram";
    private static final String AGG_MESSAGE_FIELD = "message_field";
    private static final String AGG_FILTER = "message_filter";
    private static final String AGG_MAX = "agg_max";
    private static final String AGG_MIN = "agg_min";

    private final OpenSearchClient client;

    @Inject
    public IndexToolsAdapterOS2(OpenSearchClient client) {
        this.client = client;
    }

    @Override
    public Map<DateTime, Map<String, Long>> fieldHistogram(String fieldName, Set<String> indices, Optional<Set<String>> includedStreams, long interval) {
        final BoolQueryBuilder queryBuilder = buildStreamIdFilter(includedStreams);

        final FilterAggregationBuilder theFilter = AggregationBuilders.filter(AGG_FILTER, queryBuilder)
                .subAggregation(AggregationBuilders.dateHistogram(AGG_DATE_HISTOGRAM)
                        .field("timestamp")
                        .subAggregation(AggregationBuilders.terms(AGG_MESSAGE_FIELD).field(fieldName))
                        .fixedInterval(new DateHistogramInterval(interval + "ms"))
                        // We use "min_doc_count" here to avoid empty buckets in the histogram result.
                        // This is needed to avoid out-of-memory errors when creating a histogram for a really large
                        // date range. See: https://github.com/Graylog2/graylog-plugin-archive/issues/59
                        .minDocCount(1L));

        final SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder()
                .query(QueryBuilders.matchAllQuery())
                .aggregation(theFilter);

        final SearchRequest searchRequest = new SearchRequest()
                .source(searchSourceBuilder)
                .indices(indices.toArray(new String[0]));
        final SearchResponse searchResult = client.search(searchRequest, "Unable to retrieve field histogram.");

        final Filter filterAggregation = searchResult.getAggregations().get(AGG_FILTER);
        final ParsedDateHistogram dateHistogram = filterAggregation.getAggregations().get(AGG_DATE_HISTOGRAM);


        final List<ParsedDateHistogram.ParsedBucket> histogramBuckets = (List<ParsedDateHistogram.ParsedBucket>) dateHistogram.getBuckets();
        final Map<DateTime, Map<String, Long>> result = Maps.newHashMapWithExpectedSize(histogramBuckets.size());

        for (ParsedDateHistogram.ParsedBucket bucket : histogramBuckets) {
            final ZonedDateTime zonedDateTime = (ZonedDateTime) bucket.getKey();
            final DateTime date = new DateTime(zonedDateTime.toInstant().toEpochMilli(), DateTimeZone.UTC);

            final Terms sourceFieldAgg = bucket.getAggregations().get(AGG_MESSAGE_FIELD);
            final List<? extends Terms.Bucket> termBuckets = sourceFieldAgg.getBuckets();

            final HashMap<String, Long> termCounts = Maps.newHashMapWithExpectedSize(termBuckets.size());

            for (Terms.Bucket termBucket : termBuckets) {
                termCounts.put(termBucket.getKeyAsString(), termBucket.getDocCount());
            }

            result.put(date, termCounts);
        }

        return ImmutableMap.copyOf(result);
    }

    @Override
    public long count(Set<String> indices, Optional<Set<String>> includedStreams) {
        final CountRequest request = new CountRequest(indices.toArray(new String[0]), buildStreamIdFilter(includedStreams))
                .indicesOptions(IndicesOptions.LENIENT_EXPAND_OPEN);

        final CountResponse result = client.execute((c, requestOptions) -> c.count(request, requestOptions), "Unable to count documents of index.");

        return result.getCount();
    }

    @Override
    @Nonnull
    public ImmutablePair<Double, Double> minMax(TimeRange timeRange, String fieldName, Set<String> indices, Optional<Set<String>> includedStreams) {
        final RangeQueryBuilder rangeQuery = TimeRangeQueryFactory.create(timeRange);
        final BoolQueryBuilder streamRangeQuery = buildStreamIdFilter(includedStreams).filter(rangeQuery);
        final FilterAggregationBuilder filterAgg = AggregationBuilders.filter(AGG_FILTER, streamRangeQuery);
        final MaxAggregationBuilder maxAgg = AggregationBuilders.max(AGG_MAX).field("scores.raw_risk");
        final MinAggregationBuilder minAgg = AggregationBuilders.min(AGG_MIN).field("scores.raw_risk");
        final FilterAggregationBuilder complexAgg = filterAgg.subAggregation(maxAgg).subAggregation(minAgg);

        final SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder()
                .query(QueryBuilders.matchAllQuery())
                .aggregation(complexAgg);
        final SearchRequest searchRequest = new SearchRequest()
                .source(searchSourceBuilder)
                .indices(indices.toArray(new String[0]));
        final SearchResponse searchResponse = client.search(searchRequest, "Unable to retrieve min/max aggregation");

        Max aggMax = ((ParsedFilter) searchResponse.getAggregations().getAsMap().get(AGG_FILTER)).getAggregations().get(AGG_MAX);
        Min aggMin = ((ParsedFilter) searchResponse.getAggregations().getAsMap().get(AGG_FILTER)).getAggregations().get(AGG_MIN);

        return new ImmutablePair<>(aggMin == null ? 0 : aggMin.getValue(), aggMax == null ? 0 : aggMax.getValue());
    }

    private BoolQueryBuilder buildStreamIdFilter(Optional<Set<String>> includedStreams) {
        final BoolQueryBuilder queryBuilder = boolQuery().must(matchAllQuery());

        // If the included streams are not present, we do not filter on streams
        if (includedStreams.isPresent()) {
            final Set<String> streams = includedStreams.get();
            final BoolQueryBuilder filterBuilder = boolQuery();

            // If the included streams set contains the default stream, we also want all documents which do not
            // have any stream assigned. Those documents have basically been in the "default stream" which didn't
            // exist in Graylog <2.2.0.
            if (streams.contains(Stream.DEFAULT_STREAM_ID)) {
                filterBuilder.should(boolQuery().mustNot(existsQuery(Message.FIELD_STREAMS)));
            }

            // Only select messages which are assigned to the given streams
            filterBuilder.should(termsQuery(Message.FIELD_STREAMS, streams));

            queryBuilder.filter(filterBuilder);
        }

        return queryBuilder;
    }
}
