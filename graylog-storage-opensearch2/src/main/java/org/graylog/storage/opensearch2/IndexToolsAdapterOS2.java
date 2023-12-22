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
import org.graylog2.indexer.IndexToolsAdapter;
import org.graylog2.plugin.Message;
import org.graylog2.plugin.streams.Stream;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.opensearch.client.opensearch._types.ExpandWildcard;
import org.opensearch.client.opensearch._types.FieldValue;
import org.opensearch.client.opensearch._types.query_dsl.BoolQuery;
import org.opensearch.client.opensearch._types.query_dsl.Query;
import org.opensearch.client.opensearch.core.MsearchRequest;
import org.opensearch.client.opensearch.core.msearch.MultisearchBody;

import jakarta.inject.Inject;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class IndexToolsAdapterOS2 implements IndexToolsAdapter {
    private static final String AGG_DATE_HISTOGRAM = "source_date_histogram";
    private static final String AGG_MESSAGE_FIELD = "message_field";
    private static final String AGG_FILTER = "message_filter";
    private final OpenSearchClient client;

    @Inject
    public IndexToolsAdapterOS2(OpenSearchClient client) {
        this.client = client;
    }

    @Override
    public Map<DateTime, Map<String, Long>> fieldHistogram(String fieldName, Set<String> indices, Optional<Set<String>> includedStreams, long interval) {
        final var queryBuilder = buildStreamIdFilter(includedStreams);

        final var body = MultisearchBody.of(builder -> builder.query(query -> query.matchAll(matchAll -> matchAll))
                .aggregations(AGG_FILTER, aggBuilder -> aggBuilder
                        .filter(filterBuilder -> filterBuilder.bool(queryBuilder))
                        .aggregations(AGG_DATE_HISTOGRAM, subAggBuilder -> subAggBuilder.dateHistogram(histogram -> histogram
                                        // We use "min_doc_count" here to avoid empty buckets in the histogram result.
                                        // This is needed to avoid out-of-memory errors when creating a histogram for a really large
                                        // date range. See: https://github.com/Graylog2/graylog-plugin-archive/issues/59
                                        .minDocCount(1)
                                        .field("timestamp")
                                        .fixedInterval(fixedInterval -> fixedInterval.time(interval + "ms")))
                                .aggregations(AGG_MESSAGE_FIELD, subSubAggBuilder -> subSubAggBuilder.terms(terms -> terms
                                        .field(fieldName))))));

        final var searchResult = client.search(MsearchRequest.of(builder -> builder.index(indices.stream().toList())
                .searches(searchBuilder -> searchBuilder.body(body).header(header -> header.index(indices.stream().toList())))), "Unable to retrieve field histogram.");

        final var filterAggregation = searchResult.result().aggregations().get(AGG_FILTER).filter();
        final var dateHistogram = filterAggregation.aggregations().get(AGG_DATE_HISTOGRAM).dateHistogram();


        final var histogramBuckets =  dateHistogram.buckets().array();
        final Map<DateTime, Map<String, Long>> result = Maps.newHashMapWithExpectedSize(histogramBuckets.size());

        for (var bucket : histogramBuckets) {
            final var instant = Instant.ofEpochMilli(Long.parseLong(bucket.key()));
            final ZonedDateTime zonedDateTime = instant.atZone(ZoneId.of("UTC"));
            final DateTime date = new DateTime(zonedDateTime.toInstant().toEpochMilli(), DateTimeZone.UTC);

            final var sourceFieldAgg = bucket.aggregations().get(AGG_MESSAGE_FIELD).sterms();
            final var termBuckets = sourceFieldAgg.buckets().array();

            final HashMap<String, Long> termCounts = Maps.newHashMapWithExpectedSize(termBuckets.size());

            for (var termBucket : termBuckets) {
                termCounts.put(termBucket.key(), termBucket.docCount());
            }

            result.put(date, termCounts);
        }

        return ImmutableMap.copyOf(result);
    }

    @Override
    public long count(Set<String> indices, Optional<Set<String>> includedStreams) {
        final var query = Query.of(builder -> builder.bool(buildStreamIdFilter(includedStreams)));

        final var result = client.execute((c) -> c.count(countRequest -> countRequest
                .index(indices.stream().toList())
                .query(query)
                .ignoreUnavailable(true)
                .allowNoIndices(false)
                .expandWildcards(ExpandWildcard.Open)), "Unable to count documents of index.");

        return result.count();
    }

    private BoolQuery buildStreamIdFilter(Optional<Set<String>> includedStreams) {
        return BoolQuery.of(queryBuilder -> {
            queryBuilder.must(must -> must.matchAll(matchAll -> matchAll));

            // If the included streams are not present, we do not filter on streams
            if (includedStreams.isPresent()) {
                final Set<String> streams = includedStreams.get();
                final var filterBuilder = BoolQuery.of(filter -> {

                    // If the included streams set contains the default stream, we also want all documents which do not
                    // have any stream assigned. Those documents have basically been in the "default stream" which didn't
                    // exist in Graylog <2.2.0.
                    if (streams.contains(Stream.DEFAULT_STREAM_ID)) {
                        filter.should(shouldBuilder -> shouldBuilder.bool(bool -> bool.mustNot(mustNot -> mustNot
                                .exists(exists -> exists.field(Message.FIELD_STREAMS)))));
                    }

                    // Only select messages which are assigned to the given streams
                    filter.should(should -> should.terms(terms -> terms.field(Message.FIELD_STREAMS)
                            .terms(termsField -> termsField.value(streams.stream().map(FieldValue::of).toList()))));
                    return filter;
                });

                queryBuilder.filter(Query.of(builder -> builder.bool(filterBuilder)));
            }
            return queryBuilder;
        });
    }
}
