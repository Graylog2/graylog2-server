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

import jakarta.annotation.Nonnull;
import jakarta.inject.Inject;
import org.graylog.storage.opensearch3.OfficialOpensearchClient;
import org.graylog2.indexer.fieldtypes.streamfiltered.esadapters.StreamsForFieldRetriever;
import org.graylog2.plugin.Message;
import org.opensearch.client.opensearch._types.aggregations.Aggregation;
import org.opensearch.client.opensearch._types.aggregations.FiltersAggregation;
import org.opensearch.client.opensearch._types.aggregations.FiltersBucket;
import org.opensearch.client.opensearch._types.aggregations.StringTermsAggregate;
import org.opensearch.client.opensearch._types.aggregations.StringTermsBucket;
import org.opensearch.client.opensearch._types.query_dsl.Query;
import org.opensearch.client.opensearch.core.SearchRequest;
import org.opensearch.client.opensearch.core.SearchResponse;
import org.opensearch.client.opensearch.core.search.TrackHits;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class StreamsForFieldRetrieverOS implements StreamsForFieldRetriever {
    private static final String AGG_NAME_FIELDS = "fields";
    private static final String AGG_NAME_STREAMS = "streams";
    private static final int SEARCH_MAX_BUCKETS_OS = 65_535;

    private final OfficialOpensearchClient officialOpensearchClient;

    @Inject
    public StreamsForFieldRetrieverOS(OfficialOpensearchClient officialOpensearchClient) {
        this.officialOpensearchClient = officialOpensearchClient;
    }

    @Override
    public Map<String, Set<String>> getStreams(final List<String> fieldNames, final String indexName) {

        final SearchResponse<Object> response = officialOpensearchClient.sync(c -> c.search(searchBuilder -> createSearchRequest(fieldNames, indexName), Object.class), "Unable to retrieve fields types aggregations");

        final Map<String, FiltersBucket> fieldBuckets = response.aggregations()
                .get(AGG_NAME_FIELDS)
                .filters()
                .buckets()
                .keyed();

        return fieldNames.stream()
                .collect(Collectors.toMap(
                        fieldName -> fieldName,
                        fieldName -> collectStreamIDs(fieldBuckets.get(fieldName))));
    }

    private static SearchRequest.Builder createSearchRequest(List<String> fieldNames, String indexName) {
        // we are interested only in aggregations, let's disable all search results
        final SearchRequest.Builder builder = new SearchRequest.Builder()
                .index(indexName)
                .trackTotalHits(TrackHits.builder().count(0).build())
                .size(0);

        // This is the main aggregation, collecting field names. Their streams are in a subagg below
        final FiltersAggregation fieldsAggregation = FiltersAggregation.builder()
                .filters(filters -> filters.keyed(fieldsToExistQuery(fieldNames)))
                .otherBucket(false)
                .build();

        // this is a sub-aggregation, for each field name it collects all related stream IDs
        final Aggregation streamsSubaggregation = Aggregation.builder()
                .terms(terms -> terms.field(Message.FIELD_STREAMS).size(SEARCH_MAX_BUCKETS_OS))
                .build();

        builder.aggregations(AGG_NAME_FIELDS, aggregations -> aggregations.filters(fieldsAggregation).aggregations(AGG_NAME_STREAMS, streamsSubaggregation));

        return builder;
    }

    /**
     * Extracts stream IDs from the bucket sub-aggregation
     *
     * @param filtersBucket bucket related to one field name
     * @return stream IDs
     */
    private Set<String> collectStreamIDs(FiltersBucket filtersBucket) {
        final StringTermsAggregate aggregation = filtersBucket.aggregations()
                .get(AGG_NAME_STREAMS)
                .sterms();

        return aggregation.buckets()
                .array()
                .stream()
                .map(StringTermsBucket::key)
                .collect(Collectors.toSet());
    }

    /**
     * Converts field names into a map fieldName -> fieldExistQuery(fieldName)
     */
    @Nonnull
    private static Map<String, Query> fieldsToExistQuery(List<String> fieldNames) {
        return fieldNames.stream()
                .collect(Collectors.toMap(fieldName -> fieldName, StreamsForFieldRetrieverOS::fieldExistQuery));
    }

    @Nonnull
    private static Query fieldExistQuery(String fieldName) {
        return Query.builder().
                exists(existsQuery -> existsQuery.field(fieldName))
                .build();
    }
}

