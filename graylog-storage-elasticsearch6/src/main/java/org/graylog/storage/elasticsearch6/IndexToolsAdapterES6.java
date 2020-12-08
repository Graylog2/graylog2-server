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
package org.graylog.storage.elasticsearch6;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import io.searchbox.client.JestClient;
import io.searchbox.core.Count;
import io.searchbox.core.CountResult;
import io.searchbox.core.Search;
import io.searchbox.core.SearchResult;
import io.searchbox.core.search.aggregation.DateHistogramAggregation;
import io.searchbox.core.search.aggregation.FilterAggregation;
import io.searchbox.core.search.aggregation.HistogramAggregation;
import io.searchbox.core.search.aggregation.TermsAggregation;
import io.searchbox.params.Parameters;
import org.graylog.shaded.elasticsearch5.org.elasticsearch.index.query.BoolQueryBuilder;
import org.graylog.shaded.elasticsearch5.org.elasticsearch.index.query.QueryBuilders;
import org.graylog.shaded.elasticsearch5.org.elasticsearch.search.aggregations.AggregationBuilders;
import org.graylog.shaded.elasticsearch5.org.elasticsearch.search.aggregations.bucket.filter.FilterAggregationBuilder;
import org.graylog.shaded.elasticsearch5.org.elasticsearch.search.builder.SearchSourceBuilder;
import org.graylog2.indexer.IndexMapping;
import org.graylog2.indexer.IndexToolsAdapter;
import org.graylog.storage.elasticsearch6.jest.JestUtils;
import org.graylog2.plugin.Message;
import org.graylog2.plugin.streams.Stream;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import javax.inject.Inject;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.graylog.shaded.elasticsearch5.org.elasticsearch.index.query.QueryBuilders.boolQuery;
import static org.graylog.shaded.elasticsearch5.org.elasticsearch.index.query.QueryBuilders.existsQuery;
import static org.graylog.shaded.elasticsearch5.org.elasticsearch.index.query.QueryBuilders.matchAllQuery;
import static org.graylog.shaded.elasticsearch5.org.elasticsearch.index.query.QueryBuilders.termsQuery;

public class IndexToolsAdapterES6 implements IndexToolsAdapter {
    private static final String AGG_DATE_HISTOGRAM = "source_date_histogram";
    private static final String AGG_MESSAGE_FIELD = "message_field";
    private static final String AGG_FILTER = "message_filter";

    private final JestClient jestClient;

    @Inject
    public IndexToolsAdapterES6(JestClient jestClient) {
        this.jestClient = jestClient;
    }

    @Override
    public Map<DateTime, Map<String, Long>> fieldHistogram(String fieldName, Set<String> indices, Optional<Set<String>> includedStreams, long interval) {
        final BoolQueryBuilder queryBuilder = buildStreamIdFilter(includedStreams);

        final FilterAggregationBuilder the_filter = AggregationBuilders.filter(AGG_FILTER, queryBuilder)
                .subAggregation(AggregationBuilders.dateHistogram(AGG_DATE_HISTOGRAM)
                        .field("timestamp")
                        .subAggregation(AggregationBuilders.terms(AGG_MESSAGE_FIELD).field(fieldName))
                        .interval(interval)
                        // We use "min_doc_count" here to avoid empty buckets in the histogram result.
                        // This is needed to avoid out-of-memory errors when creating a histogram for a really large
                        // date range. See: https://github.com/Graylog2/graylog-plugin-archive/issues/59
                        .minDocCount(1L));

        final SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder()
                .query(QueryBuilders.matchAllQuery())
                .aggregation(the_filter);

        final Search.Builder searchBuilder = new Search.Builder(searchSourceBuilder.toString())
                .addIndex(indices)
                .addType(IndexMapping.TYPE_MESSAGE);
        final SearchResult searchResult = JestUtils.execute(this.jestClient, searchBuilder.build(), () -> "Unable to retrieve field histogram.");

        final FilterAggregation filterAggregation = searchResult.getAggregations().getFilterAggregation(AGG_FILTER);
        final DateHistogramAggregation dateHistogram = filterAggregation.getDateHistogramAggregation(AGG_DATE_HISTOGRAM);


        final List<DateHistogramAggregation.DateHistogram> histogramBuckets = dateHistogram.getBuckets();
        final Map<DateTime, Map<String, Long>> result = Maps.newHashMapWithExpectedSize(histogramBuckets.size());

        for (HistogramAggregation.Histogram bucket : histogramBuckets) {
            final DateTime date = new DateTime(bucket.getKey()).toDateTime(DateTimeZone.UTC);

            final TermsAggregation sourceFieldAgg = bucket.getTermsAggregation(AGG_MESSAGE_FIELD);
            final List<TermsAggregation.Entry> termBuckets = sourceFieldAgg.getBuckets();

            final HashMap<String, Long> termCounts = Maps.newHashMapWithExpectedSize(termBuckets.size());

            for (TermsAggregation.Entry termBucket : termBuckets) {
                termCounts.put(termBucket.getKeyAsString(), termBucket.getCount());
            }

            result.put(date, termCounts);
        }

        return ImmutableMap.copyOf(result);
    }

    @Override
    public long count(Set<String> indices, Optional<Set<String>> includedStreams) {
        final SearchSourceBuilder queryBuilder = new SearchSourceBuilder().query(buildStreamIdFilter(includedStreams));

        final Count.Builder builder = new Count.Builder()
                .query(queryBuilder.toString())
                .addIndex(indices)
                .addType(IndexMapping.TYPE_MESSAGE)
                .setParameter(Parameters.IGNORE_UNAVAILABLE, true);

        final CountResult result = JestUtils.execute(jestClient, builder.build(), () -> "Unable to count documents of index.");

        return result.getCount().longValue();
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
