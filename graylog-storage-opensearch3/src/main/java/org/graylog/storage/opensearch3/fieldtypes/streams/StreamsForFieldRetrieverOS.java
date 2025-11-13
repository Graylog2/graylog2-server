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
package org.graylog.storage.opensearch3.fieldtypes.streams;

import jakarta.inject.Inject;
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
import org.graylog.storage.opensearch3.OfficialOpensearchClient;
import org.graylog.storage.opensearch3.OpenSearchClient;
import org.graylog2.indexer.fieldtypes.streamfiltered.esadapters.StreamsForFieldRetriever;
import org.graylog2.plugin.Message;
import org.opensearch.client.opensearch._types.aggregations.FiltersAggregation;
import org.opensearch.client.opensearch._types.query_dsl.Query;
import org.opensearch.client.opensearch.core.search.TrackHits;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class StreamsForFieldRetrieverOS implements StreamsForFieldRetriever {
    private static final String AGG_NAME = "fields";
    private static final int SEARCH_MAX_BUCKETS_OS = 65_535;

    private final OpenSearchClient client;
    private final OfficialOpensearchClient officialOpensearchClient;

    @Inject
    public StreamsForFieldRetrieverOS(final OpenSearchClient client, OfficialOpensearchClient officialOpensearchClient) {
        this.client = client;
        this.officialOpensearchClient = officialOpensearchClient;
    }

    private record FieldBucket(String fieldName, Set<String> value) {}

    @Override
    public Map<String, Set<String>> getStreams(final List<String> fieldNames, final String indexName) {
        final SearchResponse response = client.search(createSearchRequest(fieldNames, indexName),
                "Unable to retrieve fields types aggregations");

        final org.opensearch.client.opensearch.core.SearchResponse<Map> newResponse = officialOpensearchClient.sync(c -> c.search(searchBuilder -> creaxSearchSourceBuilder(fieldNames, indexName), Map.class), "Unable to retrieve fields types aggregations");


        final ParsedFilters aggregation = response.getAggregations().get(AGG_NAME);

        return fieldNames.stream()
                .map(fieldName -> new FieldBucket(fieldName, retrieveStreamsFromAggregationInResponse(aggregation.getBucketByKey(fieldName))))
                .collect(Collectors.toMap(FieldBucket::fieldName, FieldBucket::value));
    }

    private org.opensearch.client.opensearch.core.SearchRequest.Builder creaxSearchSourceBuilder(List<String> fieldNames, String indexName) {
        final org.opensearch.client.opensearch.core.SearchRequest.Builder builder = new org.opensearch.client.opensearch.core.SearchRequest.Builder()
            .trackTotalHits(TrackHits.builder().count(0).build())
                .size(0);

        final org.opensearch.client.opensearch._types.aggregations.Aggregation subagg = org.opensearch.client.opensearch._types.aggregations.Aggregation.builder().terms(terms -> terms.field(Message.FIELD_STREAMS).size(SEARCH_MAX_BUCKETS_OS)).build();

        final FiltersAggregation filtersAgg = FiltersAggregation.builder()
                .filters(fi -> fi.keyed(fieldsToExistQuery(fieldNames)))
                .otherBucket(false)
                .build();

        builder.aggregations(AGG_NAME, agg -> agg.filters(filtersAgg)); // <-- here I'd like to add a subagg named Message.FIELD_STREAMS, should be subaggregation of the filters aggregation
        return builder;
    }

    private Map<String, Query> fieldsToExistQuery(List<String> fieldNames) {
        return fieldNames.stream().collect(Collectors.toMap(fieldName -> fieldName, fieldName -> Query.builder().exists(e -> e.field(fieldName)).build()));
    }

    private SearchSourceBuilder createSearchSourceBuilder(final List<String> fieldNames) {
        final SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder()
                .trackTotalHits(false)
                .size(0);

        final List<FiltersAggregator.KeyedFilter> filters = fieldNames.stream()
                .map(fieldName -> new FiltersAggregator.KeyedFilter(fieldName, QueryBuilders.existsQuery(fieldName)))
                .toList();

        final var filtersAggregation = AggregationBuilders.filters(AGG_NAME, filters.toArray(FiltersAggregator.KeyedFilter[]::new)).otherBucket(false)
                .subAggregation(AggregationBuilders
                        .terms(Message.FIELD_STREAMS)
                        .field(Message.FIELD_STREAMS)
                        .size(SEARCH_MAX_BUCKETS_OS));

        searchSourceBuilder.aggregation(filtersAggregation);
        return searchSourceBuilder;
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
}

