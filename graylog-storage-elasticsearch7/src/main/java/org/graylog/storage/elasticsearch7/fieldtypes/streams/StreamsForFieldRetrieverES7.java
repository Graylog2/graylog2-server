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
package org.graylog.storage.elasticsearch7.fieldtypes.streams;

import org.graylog.shaded.elasticsearch7.org.elasticsearch.action.search.MultiSearchResponse;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.action.search.SearchRequest;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.action.search.SearchResponse;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.index.query.ExistsQueryBuilder;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.search.aggregations.Aggregation;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.search.aggregations.AggregationBuilders;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.search.aggregations.Aggregations;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.search.aggregations.bucket.MultiBucketsAggregation;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.search.builder.SearchSourceBuilder;
import org.graylog.storage.elasticsearch7.ElasticsearchClient;
import org.graylog2.indexer.fieldtypes.streamfiltered.esadapters.StreamsForFieldRetriever;
import org.graylog2.plugin.Message;

import javax.inject.Inject;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class StreamsForFieldRetrieverES7 implements StreamsForFieldRetriever {

    private static final int SEARCH_MAX_BUCKETS_ES = 10_000;

    private final ElasticsearchClient client;

    @Inject
    public StreamsForFieldRetrieverES7(final ElasticsearchClient client) {
        this.client = client;
    }

    @Override
    public Map<String, Set<String>> getStreams(final List<String> fieldNames, final String indexName) {
        final List<MultiSearchResponse.Item> multiSearchResponse = client.msearch(fieldNames.stream()
                        .map(fieldName -> createSearchRequest(fieldName, indexName))
                        .collect(Collectors.toList()),
                "Unable to retrieve fields types aggregations");


        final List<Set<String>> streamsPerField = multiSearchResponse.stream()
                .map(item -> retrieveStreamsFromAggregationInResponse(item.getResponse()))
                .toList();

        Map<String, Set<String>> result = new HashMap<>(fieldNames.size());
        for (int i = 0; i < fieldNames.size(); i++) {
            result.put(fieldNames.get(i), streamsPerField.get(i));
        }

        return result;

    }

    @Override
    public Set<String> getStreams(final String fieldName, final String indexName) {
        final SearchRequest searchRequest = createSearchRequest(fieldName, indexName);

        final SearchResponse searchResult = client.search(searchRequest, "Unable to retrieve fields types aggregations");

        return retrieveStreamsFromAggregationInResponse(searchResult);
    }

    private Set<String> retrieveStreamsFromAggregationInResponse(final SearchResponse searchResult) {
        final Aggregations aggregations = searchResult.getAggregations();
        if (aggregations != null) {
            final Aggregation streamsAggregation = aggregations.get(Message.FIELD_STREAMS);

            if (streamsAggregation instanceof MultiBucketsAggregation) {
                final List<? extends MultiBucketsAggregation.Bucket> buckets = ((MultiBucketsAggregation) streamsAggregation).getBuckets();
                if (buckets != null) {
                    return buckets.stream()
                            .map(MultiBucketsAggregation.Bucket::getKeyAsString)
                            .collect(Collectors.toSet());
                }
            }
        }
        return Set.of();
    }

    private SearchRequest createSearchRequest(final String fieldName, final String indexName) {
        final SearchSourceBuilder searchSourceBuilder = createSearchSourceBuilder(fieldName);
        return new SearchRequest(indexName)
                .source(searchSourceBuilder);
    }

    private SearchSourceBuilder createSearchSourceBuilder(final String fieldName) {
        final SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder()
                .query(new ExistsQueryBuilder(fieldName))
                .trackTotalHits(false)
                .size(0);

        searchSourceBuilder.aggregation(AggregationBuilders
                .terms(Message.FIELD_STREAMS)
                .field(Message.FIELD_STREAMS)
                .size(SEARCH_MAX_BUCKETS_ES));
        return searchSourceBuilder;
    }

}
