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
package org.graylog.storage.opensearch3;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import jakarta.inject.Inject;
import org.graylog2.indexer.IndexToolsAdapter;
import org.graylog2.plugin.Message;
import org.graylog2.plugin.streams.Stream;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.opensearch.client.opensearch._types.ExpandWildcard;
import org.opensearch.client.opensearch._types.FieldValue;
import org.opensearch.client.opensearch._types.aggregations.Aggregation;
import org.opensearch.client.opensearch._types.aggregations.DateHistogramBucket;
import org.opensearch.client.opensearch._types.aggregations.FilterAggregate;
import org.opensearch.client.opensearch._types.aggregations.StringTermsBucket;
import org.opensearch.client.opensearch._types.query_dsl.BoolQuery;
import org.opensearch.client.opensearch._types.query_dsl.MatchAllQuery;
import org.opensearch.client.opensearch._types.query_dsl.Query;
import org.opensearch.client.opensearch.core.SearchRequest;
import org.opensearch.client.opensearch.core.SearchResponse;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class IndexToolsAdapterOS implements IndexToolsAdapter {
    private static final String AGG_DATE_HISTOGRAM = "source_date_histogram";
    private static final String AGG_MESSAGE_FIELD = "message_field";
    private static final String AGG_FILTER = "message_filter";
    private final OfficialOpensearchClient client;

    @Inject
    public IndexToolsAdapterOS(OfficialOpensearchClient client) {
        this.client = client;
    }

    @Override
    public Map<DateTime, Map<String, Long>> fieldHistogram(String fieldName, Set<String> indices,
                                                           Optional<Set<String>> includedStreams, long interval) {
        final Query streamFilter = buildStreamIdFilter(includedStreams);

        final Aggregation dateHistogramAgg = Aggregation.of(a -> a
                .dateHistogram(dh -> dh
                        .field("timestamp")
                        .fixedInterval(fi -> fi.time(interval + "ms"))
                        // We use "min_doc_count" here to avoid empty buckets in the histogram result.
                        // This is needed to avoid out-of-memory errors when creating a histogram for a really large
                        // date range. See: https://github.com/Graylog2/graylog-plugin-archive/issues/59
                        .minDocCount(1)
                )
                .aggregations(AGG_MESSAGE_FIELD, Aggregation.of(ta -> ta
                        .terms(t -> t.field(fieldName))
                ))
        );

        final Aggregation filterAgg = Aggregation.of(a -> a
                .filter(streamFilter)
                .aggregations(AGG_DATE_HISTOGRAM, dateHistogramAgg)
        );

        final SearchRequest searchRequest = SearchRequest.of(sr -> sr
                .index(indices.stream().toList())
                .query(Query.of(q -> q.matchAll(m -> m)))
                .aggregations(AGG_FILTER, filterAgg)
                .size(0)
        );

        final SearchResponse<Void> searchResult = client.sync(
                c -> c.search(searchRequest, Void.class),
                "Unable to retrieve field histogram."
        );

        final FilterAggregate filterAggregate = searchResult.aggregations().get(AGG_FILTER).filter();
        final List<DateHistogramBucket> histogramBuckets = filterAggregate.aggregations()
                .get(AGG_DATE_HISTOGRAM).dateHistogram().buckets().array();

        final Map<DateTime, Map<String, Long>> result = Maps.newHashMapWithExpectedSize(histogramBuckets.size());

        for (final DateHistogramBucket bucket : histogramBuckets) {
            final DateTime date = new DateTime(bucket.key(), DateTimeZone.UTC);

            final List<StringTermsBucket> termBuckets = bucket.aggregations()
                    .get(AGG_MESSAGE_FIELD).sterms().buckets().array();

            final HashMap<String, Long> termCounts = Maps.newHashMapWithExpectedSize(termBuckets.size());
            for (final StringTermsBucket termBucket : termBuckets) {
                termCounts.put(termBucket.key(), termBucket.docCount());
            }

            result.put(date, termCounts);
        }

        return ImmutableMap.copyOf(result);
    }

    @Override
    public long count(Set<String> indices, Optional<Set<String>> includedStreams) {
        final Query query = buildStreamIdFilter(includedStreams);

        final SearchRequest searchRequest = SearchRequest.of(sr -> sr
                .index(indices.stream().toList())
                .query(query)
                .ignoreUnavailable(true)
                .allowNoIndices(true)
                .expandWildcards(ExpandWildcard.Open)
                .trackTotalHits(t -> t.enabled(true))
                .size(0)
        );

        final SearchResponse<Void> response = client.sync(
                c -> c.search(searchRequest, Void.class),
                "Unable to count documents of index."
        );

        return response.hits().total().value();
    }

    private Query buildStreamIdFilter(Optional<Set<String>> includedStreams) {
        BoolQuery.Builder queryBuilder = BoolQuery.builder().must(MatchAllQuery.builder().build().toQuery());

        // If the included streams are not present, we do not filter on streams
        if (includedStreams.isPresent()) {
            final Set<String> streams = includedStreams.get();
            final BoolQuery.Builder filterBuilder = new BoolQuery.Builder();

            // If the included streams set contains the default stream, we also want all documents which do not
            // have any stream assigned. Those documents have basically been in the "default stream" which didn't
            // exist in Graylog <2.2.0.
            if (streams.contains(Stream.DEFAULT_STREAM_ID)) {
                final Query noStreamsField = Query.of(q -> q
                        .bool(b -> b.mustNot(mn -> mn.exists(e -> e.field(Message.FIELD_STREAMS))))
                );
                filterBuilder.should(noStreamsField);
            }

            // Only select messages which are assigned to the given streams
            final Query termsQuery = Query.of(q -> q
                    .terms(t -> t
                            .field(Message.FIELD_STREAMS)
                            .terms(tv -> tv.value(streams.stream().map(FieldValue::of).toList()))
                    )
            );
            filterBuilder.should(termsQuery);

            queryBuilder.filter(filterBuilder.build().toQuery());
        }

        return queryBuilder.build().toQuery();
    }
}
