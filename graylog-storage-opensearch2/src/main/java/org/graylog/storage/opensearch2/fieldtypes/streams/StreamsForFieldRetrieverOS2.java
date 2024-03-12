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
package org.graylog.storage.opensearch2.fieldtypes.streams;

import org.graylog.storage.opensearch2.IndexedMessage;
import org.graylog.storage.opensearch2.OpenSearchClient;
import org.graylog2.indexer.fieldtypes.streamfiltered.esadapters.StreamsForFieldRetriever;
import org.graylog2.plugin.Message;
import org.opensearch.client.opensearch._types.aggregations.StringTermsBucket;
import org.opensearch.client.opensearch.core.MsearchRequest;
import org.opensearch.client.opensearch.core.msearch.MultiSearchResponseItem;
import org.opensearch.client.opensearch.core.msearch.RequestItem;

import jakarta.inject.Inject;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class StreamsForFieldRetrieverOS2 implements StreamsForFieldRetriever {

    private static final int SEARCH_MAX_BUCKETS_OS = 65_535;

    private final OpenSearchClient client;

    @Inject
    public StreamsForFieldRetrieverOS2(final OpenSearchClient client) {
        this.client = client;
    }

    @Override
    public Map<String, Set<String>> getStreams(final List<String> fieldNames, final String indexName) {
        final var multiSearchResponse = client.msearch2(fieldNames.stream()
                        .map(fieldName -> createSearchRequest(fieldName, indexName))
                        .collect(Collectors.toList()),
                "Unable to retrieve fields types aggregations");


        final List<Set<String>> streamsPerField = multiSearchResponse.stream()
                .map(this::retrieveStreamsFromAggregationInResponse)
                .toList();

        Map<String, Set<String>> result = new HashMap<>(fieldNames.size());
        for (int i = 0; i < fieldNames.size(); i++) {
            result.put(fieldNames.get(i), streamsPerField.get(i));
        }

        return result;

    }

    @Override
    public Set<String> getStreams(final String fieldName, final String indexName) {
        final var searchRequest = MsearchRequest.of(builder -> builder.searches(createSearchRequest(fieldName, indexName)));

        final var searchResult = client.search(searchRequest, "Unable to retrieve fields types aggregations");

        return retrieveStreamsFromAggregationInResponse(searchResult);
    }

    private Set<String> retrieveStreamsFromAggregationInResponse(final MultiSearchResponseItem<IndexedMessage> searchResult) {
        final var aggregations = searchResult.result().aggregations();
        if (aggregations != null) {
            final var streamsAggregation = aggregations.get(Message.FIELD_STREAMS).sterms();
            final var buckets = streamsAggregation.buckets();
            if (buckets != null) {
                return buckets.array().stream()
                        .map(StringTermsBucket::key)
                        .collect(Collectors.toSet());
            }
        }
        return Set.of();
    }

    private RequestItem createSearchRequest(final String fieldName, final String indexName) {
        return RequestItem.of(searchBuilder -> searchBuilder
                .header(headerBuilder -> headerBuilder.index(indexName))
                .body(bodyBuilder -> bodyBuilder
                        .query(queryBuilder -> queryBuilder.exists(existsBuilder -> existsBuilder.field(fieldName)))
                        .size(0)
                        .trackTotalHits(trackHitsBuilder -> trackHitsBuilder.enabled(false))
                        .aggregations(Message.FIELD_STREAMS, aggBuilder -> aggBuilder.terms(termsBuilder -> termsBuilder.field(Message.FIELD_STREAMS).size(SEARCH_MAX_BUCKETS_OS)))));
    }
}

