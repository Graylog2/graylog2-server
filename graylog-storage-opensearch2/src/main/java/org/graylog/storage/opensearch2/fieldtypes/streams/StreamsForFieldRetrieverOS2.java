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

import jakarta.inject.Inject;
import org.graylog.shaded.opensearch2.org.opensearch.action.search.MultiSearchResponse;
import org.graylog.shaded.opensearch2.org.opensearch.action.search.SearchRequest;
import org.graylog.shaded.opensearch2.org.opensearch.action.search.SearchResponse;
import org.graylog.shaded.opensearch2.org.opensearch.index.query.QueryBuilders;
import org.graylog.shaded.opensearch2.org.opensearch.search.aggregations.Aggregation;
import org.graylog.shaded.opensearch2.org.opensearch.search.aggregations.AggregationBuilders;
import org.graylog.shaded.opensearch2.org.opensearch.search.aggregations.Aggregations;
import org.graylog.shaded.opensearch2.org.opensearch.search.aggregations.bucket.MultiBucketsAggregation;
import org.graylog.shaded.opensearch2.org.opensearch.search.aggregations.bucket.filter.FiltersAggregator;
import org.graylog.shaded.opensearch2.org.opensearch.search.aggregations.bucket.filter.ParsedFilters;
import org.graylog.shaded.opensearch2.org.opensearch.search.builder.SearchSourceBuilder;
import org.graylog.storage.opensearch2.OpenSearchClient;
import org.graylog2.indexer.fieldtypes.streamfiltered.esadapters.StreamsForFieldRetriever;
import org.graylog2.plugin.Message;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.graylog2.utilities.Entry.createEntry;

public class StreamsForFieldRetrieverOS2 implements StreamsForFieldRetriever {
    private static final String AGG_NAME = "fields";
    private static final int SEARCH_MAX_BUCKETS_OS = 65_535;

    private final OpenSearchClient client;

    @Inject
    public StreamsForFieldRetrieverOS2(final OpenSearchClient client) {
        this.client = client;
    }

    @Override
    public Map<String, Set<String>> getStreams(final List<String> fieldNames, final String indexName) {
        final List<MultiSearchResponse.Item> multiSearchResponse = client.msearch(List.of(createSearchRequest(fieldNames, indexName)),
                "Unable to retrieve fields types aggregations");

        final var response = multiSearchResponse.get(0);
        final ParsedFilters aggregation = response.getResponse().getAggregations().get(AGG_NAME);

        return fieldNames.stream()
                .map(fieldName -> createEntry(fieldName, aggregation.getBucketByKey(fieldName)))
                .map(entry -> entry.withValue(retrieveStreamsFromAggregationInResponse(entry.getValue())))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    @Override
    public Set<String> getStreams(final String fieldName, final String indexName) {
        final SearchRequest searchRequest = createSearchRequest(List.of(fieldName), indexName);

        final SearchResponse searchResult = client.search(searchRequest, "Unable to retrieve fields types aggregations");

        final ParsedFilters aggregation = searchResult.getAggregations().get(AGG_NAME);
        final var bucket = aggregation.getBucketByKey(fieldName);

        return retrieveStreamsFromAggregationInResponse(bucket);
    }

    private Set<String> retrieveStreamsFromAggregationInResponse(ParsedFilters.ParsedBucket searchResult) {
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

    private SearchRequest createSearchRequest(List<String> fieldNames, final String indexName) {
        final SearchSourceBuilder searchSourceBuilder = createSearchSourceBuilder(fieldNames);
        return new SearchRequest(indexName)
                .source(searchSourceBuilder);
    }

    private SearchSourceBuilder createSearchSourceBuilder(final List<String> fieldNames) {
        final SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder()
                .trackTotalHits(false)
                .size(0);

        final var filters = fieldNames.stream()
                .map(fieldName -> new FiltersAggregator.KeyedFilter(fieldName, QueryBuilders.existsQuery(fieldName)))
                .toList();

        final var filtersAggregation = AggregationBuilders.filters(AGG_NAME, filters.toArray(new FiltersAggregator.KeyedFilter[]{})).otherBucket(false)
                .subAggregation(AggregationBuilders
                .terms(Message.FIELD_STREAMS)
                .field(Message.FIELD_STREAMS)
                .size(SEARCH_MAX_BUCKETS_OS));

        searchSourceBuilder.aggregation(filtersAggregation);
        return searchSourceBuilder;
    }
}

